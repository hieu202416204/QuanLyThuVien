package Database;

import BackEnd.Book.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class BookDAO {

    // 1. Dùng List tĩnh để lưu dữ liệu trên RAM (Chia sẻ chung cho toàn bộ ứng dụng)
    // CopyOnWriteArrayList giúp an toàn khi vừa đọc vừa ghi (Thread-safe)
    private static List<Book> cachedBooks = null;

    public BookDAO() {
        // Nếu cache chưa có dữ liệu, tải từ DB lên ngay
        if (cachedBooks == null) {
            reloadCache();
        }
    }

    /**
     * Tải toàn bộ sách từ DB vào RAM.
     * Gọi hàm này khi khởi động App hoặc sau khi Reset DB.
     */
    public synchronized void reloadCache() {
        cachedBooks = new CopyOnWriteArrayList<>(getAllBooksFromDB());
    }

    // =======================================================
    // I. TÌM KIẾM TRÊN RAM (TỐI ƯU HÓA)
    // =======================================================

    /**
     * Tìm kiếm theo tên (chứa chuỗi) - Cực nhanh nhờ RAM
     */
    public List<Book> searchBookByPartialName(String query) {
        if (query == null || query.isEmpty()) return new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        // Dùng Parallel Stream để tận dụng đa luồng CPU quét list cực nhanh
        return cachedBooks.parallelStream()
                .filter(b -> b.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    /**
     * Tìm kiếm kết hợp Tên hoặc Tác giả
     */
    public List<Book> searchBookCombined(String query) {
        if (query == null || query.isEmpty()) return new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        return cachedBooks.parallelStream()
                .filter(b -> b.getName().toLowerCase().contains(lowerQuery) ||
                        b.getAuthor().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    // =======================================================
    // II. CRUD (CẬP NHẬT ĐỒNG BỘ CẢ DB VÀ CACHE)
    // =======================================================

    public boolean addBook(Book book) {
        // 1. Ghi vào DB trước (để đảm bảo bền vững)
        if (insertBookToDB(book)) {
            // 2. Nếu thành công, thêm vào Cache ngay lập tức
            cachedBooks.add(book);
            return true;
        }
        return false;
    }

    public boolean updateBook(Book book) {
        if (updateBookInDB(book)) {
            // Cập nhật trong Cache: Tìm sách cũ và thay thế
            for (int i = 0; i < cachedBooks.size(); i++) {
                if (cachedBooks.get(i).getId().equals(book.getId())) {
                    cachedBooks.set(i, book);
                    break;
                }
            }
            return true;
        }
        return false;
    }

    public boolean deleteBook(String id) {
        if (deleteBookInDB(id)) {
            // Xóa khỏi Cache
            cachedBooks.removeIf(b -> b.getId().equals(id));
            return true;
        }
        return false;
    }

    /**
     * Cập nhật trạng thái sách (Mượn/Trả) và số lượt mượn.
     * Cập nhật đồng thời cả DB và Cache để UI phản hồi ngay lập tức.
     */
    public boolean updateBookStatus(String bookId, boolean newStatus, int newSoLuotMuon) {
        // 1. Cập nhật trong Database trước
        String sql = "UPDATE books SET status = ?, soLuotMuon = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newStatus ? 1 : 0); // 1: Available, 0: Borrowed
            pstmt.setInt(2, newSoLuotMuon);
            pstmt.setString(3, bookId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // 2. Nếu DB thành công, cập nhật ngay trong Cache (RAM)
                for (Book b : cachedBooks) {
                    if (b.getId().equals(bookId)) {
                        b.setStatus(newStatus);
                        b.setSoLuotMuonFromDB(newSoLuotMuon);
                        break;
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật trạng thái sách: " + e.getMessage());
        }
        return false;
    }

    // =======================================================
    // III. CÁC HÀM TRUY CẬP DB (PRIVATE - CHỈ DÙNG NỘI BỘ)
    // =======================================================

    private List<Book> getAllBooksFromDB() {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT * FROM books"; // Lấy tất cả, không cần WHERE
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(extractBookFromResultSet(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private boolean insertBookToDB(Book book) {
        // Đã bổ sung damage_percent và damage_details
        String sql = "INSERT INTO books (id, name, author, year, category, status, imagePath, soLuotMuon, damage_percent, damage_details) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, book.getId());
            pstmt.setString(2, book.getName());
            pstmt.setString(3, book.getAuthor());
            pstmt.setString(4, book.getYear());
            pstmt.setString(5, book.getCategory());
            pstmt.setInt(6, book.isStatus() ? 1 : 0);
            pstmt.setString(7, book.getImagePath());
            pstmt.setInt(8, book.getSoLuotMuon());
            pstmt.setInt(9, book.getDamagePercent());
            pstmt.setString(10, book.getDamageDetails());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi DB Add: " + e.getMessage());
            return false;
        }
    }

    private boolean updateBookInDB(Book book) {
        // Đã bổ sung damage_percent và damage_details
        String sql = "UPDATE books SET name = ?, author = ?, year = ?, category = ?, imagePath = ?, status = ?, damage_percent = ?, damage_details = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, book.getName());
            pstmt.setString(2, book.getAuthor());
            pstmt.setString(3, book.getYear());
            pstmt.setString(4, book.getCategory());
            pstmt.setString(5, book.getImagePath());
            pstmt.setInt(6, book.isStatus() ? 1 : 0);
            pstmt.setInt(7, book.getDamagePercent());
            pstmt.setString(8, book.getDamageDetails());
            pstmt.setString(9, book.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private boolean deleteBookInDB(String id) {
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // --- Các hàm hỗ trợ khác ---
    public Book getBookById(String id) {
        // Tìm trong Cache trước
        return cachedBooks.stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<Book> getAllBooks() {
        if (cachedBooks == null) reloadCache();
        return new ArrayList<>(cachedBooks);
    }

    public List<Book> getBooksSortedByBorrowCount() {
        reloadCache();
        return cachedBooks.stream()
                .sorted(Comparator.comparingInt(Book::getSoLuotMuon).reversed())
                .collect(Collectors.toList());
    }
    public List<Book> getBooksSortedByRateCount(){
        reloadCache();
        return cachedBooks.stream()
                .sorted(Comparator.comparingInt(Book::getDamagePercent).reversed()).
                collect(Collectors.toList());
    }

    private Book extractBookFromResultSet(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getString("id"));
        book.setName(rs.getString("name"));
        book.setAuthor(rs.getString("author"));
        book.setYear(rs.getString("year"));
        book.setCategory((rs.getString("category")));
        book.setStatus(rs.getInt("status") == 1);
        book.setImagePath(rs.getString("imagePath"));
        book.setSoLuotMuonFromDB(rs.getInt("soLuotMuon"));

        // --- Đọc Tình trạng sách ---
        book.setDamagePercent(rs.getInt("damage_percent"));
        book.setDamageDetails(rs.getString("damage_details"));

        return book;
    }

    // --- 4. LẤY DANH SÁCH CHỦ ĐỀ DUY NHẤT (DISTINCT) ---
    /**
     * Lấy danh sách các chủ đề đang có trong thư viện để gợi ý cho người dùng.
     * Sử dụng Stream trên Cache để cực nhanh, không cần query DB lại.
     */
    public List<String> getUniqueCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM books WHERE category IS NOT NULL AND category != '' ORDER BY category";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    // --- 5. TÌM KIẾM TỪ KHÓA TỐI ƯU (ADVANCED SEARCH) ---
    /**
     * Tìm kiếm thông minh: Quét qua Tên, Tác giả, Chủ đề, Năm, ID.
     * @param keyword Từ khóa bất kỳ
     */
    public List<Book> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return new ArrayList<>();

        final String lowerKey = keyword.toLowerCase().trim();

        // Sử dụng Parallel Stream để tìm kiếm đa luồng tốc độ cao trên RAM
        return cachedBooks.parallelStream()
                .filter(b -> {
                    return (b.getName() != null && b.getName().toLowerCase().contains(lowerKey)) ||
                            (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(lowerKey)) ||
                            (b.getId().toLowerCase().contains(lowerKey)) ||
                            (b.getCategory() != null && b.getCategory().toLowerCase().contains(lowerKey)) ||
                            (b.getYear() != null && b.getYear().contains(lowerKey));
                })
                .collect(Collectors.toList());
    }

    // =======================================================
    // PHÂN TRANG (PAGINATION) - PHỤC VỤ CHO GIAO DIỆN TỐI ƯU
    // =======================================================

    /**
     * Lấy tổng số lượng sách trong DB
     */
    public int getTotalBookCount() {
        String sql = "SELECT COUNT(id) FROM books";
        try (Connection conn = DatabaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Lấy danh sách sách theo trang
     */
    public List<Book> getBooksByPage(int offset, int limit) {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY id LIMIT ? OFFSET ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(extractBookFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy ID sách lớn nhất (VD: B099 -> lấy 99) để tự động tạo ID mới
     */
    public String getLastBookId() {
        String sql = "SELECT id FROM books WHERE id LIKE 'B%' ORDER BY CAST(SUBSTR(id, 2) AS INTEGER) DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getString("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}