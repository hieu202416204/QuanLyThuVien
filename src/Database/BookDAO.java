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
        System.out.println("⚡ [Cache] Đã tải " + cachedBooks.size() + " cuốn sách vào bộ nhớ RAM.");
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
                // Duyệt qua list cache để tìm sách và sửa đổi trực tiếp object đó
                for (Book b : cachedBooks) {
                    if (b.getId().equals(bookId)) {
                        b.setStatus(newStatus);
                        b.setSoLuotMuonFromDB(newSoLuotMuon);
                        break; // Tìm thấy rồi thì dừng vòng lặp
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
        String sql = "INSERT INTO books (id, name, author, year,category, status, imagePath, soLuotMuon) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi DB Add: " + e.getMessage());
            return false;
        }
    }

    private boolean updateBookInDB(Book book) {
        String sql = "UPDATE books SET name = ?, author = ?, year = ?, category =?, imagePath = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, book.getName());
            pstmt.setString(2, book.getAuthor());
            pstmt.setString(3, book.getYear());
            pstmt.setString(4, book.getCategory());
            pstmt.setString(5, book.getImagePath());
            pstmt.setString(6, book.getId());
            if (pstmt.executeUpdate() > 0) {
                // Update Cache
                for (int i = 0; i < cachedBooks.size(); i++) {
                    if (cachedBooks.get(i).getId().equals(book.getId())) {
                        cachedBooks.set(i, book);
                        break;
                    }
                }
                return true;
            }
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
        id.toLowerCase();
        // Tìm trong Cache trước (Nhanh hơn DB nhiều)
        return cachedBooks.stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<Book> getAllBooks() {
        if (cachedBooks == null) reloadCache();
        return new ArrayList<>(cachedBooks);
    }

    // Sort, Map theo yêu cầu (Ví dụ: Lấy top sách mượn nhiều nhất từ Cache)
    public List<Book> getBooksSortedByBorrowCount() {
        return cachedBooks.stream()
                .sorted(Comparator.comparingInt(Book::getSoLuotMuon).reversed()) // Sort trên RAM
                .collect(Collectors.toList());
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
        return book;
    }
    // --- 4. LẤY DANH SÁCH CHỦ ĐỀ DUY NHẤT (DISTINCT) ---
    /**
     * Lấy danh sách các chủ đề đang có trong thư viện để gợi ý cho người dùng.
     * Sử dụng Stream trên Cache để cực nhanh, không cần query DB lại.
     */
    public List<String> getUniqueCategories() {
        if (cachedBooks == null) reloadCache();
        return cachedBooks.stream()
                .map(Book::getCategory)       // Lấy trường category
                .filter(c -> c != null && !c.isEmpty()) // Lọc bỏ null/rỗng
                .distinct()                   // Lọc trùng
                .sorted()                     // Sắp xếp A-Z
                .collect(Collectors.toList());
    }

    // --- 5. TÌM KIẾM TỪ KHÓA TỐI ƯU (ADVANCED SEARCH) ---
    /**
     * Tìm kiếm thông minh: Quét qua Tên, Tác giả, Chủ đề, Năm, ID.
     * @param keyword Từ khóa bất kỳ
     */
    public List<Book> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return new ArrayList<>();

        final String lowerKey = keyword.toLowerCase().trim();

        // Sử dụng Parallel Stream để tìm kiếm đa luồng tốc độ cao
        return cachedBooks.parallelStream()
                .filter(b -> {
                    // Kiểm tra null an toàn và so sánh
                    return (b.getName() != null && b.getName().toLowerCase().contains(lowerKey)) ||
                            (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(lowerKey)) ||
                            (b.getId().toLowerCase().contains(lowerKey)) ||
                            (b.getCategory() != null && b.getCategory().toLowerCase().contains(lowerKey)) ||
                            (b.getYear() != null && b.getYear().contains(lowerKey));
                })
                .collect(Collectors.toList());
    }
}