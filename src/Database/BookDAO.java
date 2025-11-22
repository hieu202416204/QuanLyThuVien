package Database;

import BackEnd.Book.Book;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    /**
     * Chuyển đổi một dòng ResultSet thành đối tượng Book.
     */
    private Book extractBookFromResultSet(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getString("id"));
        book.setName(rs.getString("name"));
        book.setAuthor(rs.getString("author"));
        book.setYear(rs.getString("year"));
        // status: 1 là Available (true), 0 là Borrowed (false)
        book.setStatus(rs.getInt("status") == 1);
        book.setImagePath(rs.getString("imagePath"));
        // Dùng setter đặc biệt để đọc số lượt mượn từ DB
        book.setSoLuotMuonFromDB(rs.getInt("soLuotMuon"));
        return book;
    }

    // =======================================================
    // I. CRUD VÀ CƠ BẢN
    // =======================================================

    /**
     * Thêm một cuốn sách mới vào cơ sở dữ liệu (Create).
     */
    public boolean addBook(Book book) {
        String sql = "INSERT INTO books (id, name, author, year, status, imagePath, soLuotMuon) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, book.getId());
            pstmt.setString(2, book.getName());
            pstmt.setString(3, book.getAuthor());
            pstmt.setString(4, book.getYear());
            pstmt.setInt(5, book.isStatus() ? 1 : 0); // Convert boolean sang int
            pstmt.setString(6, book.getImagePath());
            pstmt.setInt(7, book.getSoLuotMuon());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi thêm sách: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy một cuốn sách theo ID (Read).
     */
    public Book getBookById(String bookId) {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractBookFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy sách theo ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy tất cả sách từ cơ sở dữ liệu.
     */
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                books.add(extractBookFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy tất cả sách: " + e.getMessage());
        }
        return books;
    }

    /**
     * Xóa sách theo ID (Delete).
     */
    public boolean deleteBook(String id) {
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa sách: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật thông tin chi tiết của sách (Update - dùng cho BookManagementTab).
     */
    public boolean updateBook(Book book) {
        String sql = "UPDATE books SET name = ?, author = ?, year = ?, imagePath = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, book.getName());
            pstmt.setString(2, book.getAuthor());
            pstmt.setString(3, book.getYear());
            pstmt.setString(4, book.getImagePath());
            pstmt.setString(5, book.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật thông tin sách: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật trạng thái và số lượt mượn của sách (Update - dùng cho Mượn/Trả).
     */
    public boolean updateBookStatus(String bookId, boolean newStatus, int soLuotMuon) {
        String sql = "UPDATE books SET status = ?, soLuotMuon = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newStatus ? 1 : 0);
            pstmt.setInt(2, soLuotMuon);
            pstmt.setString(3, bookId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật trạng thái sách: " + e.getMessage());
            return false;
        }
    }

    // =======================================================
    // II. TÌM KIẾM VÀ THỐNG KÊ
    // =======================================================

    /**
     * Phương thức chung thực thi truy vấn tìm kiếm
     */
    private List<Book> executeSearchQuery(String sql, String query) {
        List<Book> books = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Đặt tham số truy vấn (sẽ khác nhau tùy theo loại tìm kiếm)
            if (sql.contains("LIKE")) {
                // Thêm ký tự đại diện % cho tìm kiếm linh hoạt (partial search)
                pstmt.setString(1, "%" + query + "%");
                // Kiểm tra xem có cần tham số thứ hai cho tìm kiếm kết hợp không
                if (sql.split("\\?").length > 1 && sql.contains("OR")) {
                    pstmt.setString(2, "%" + query + "%");
                }
            } else {
                // Khớp chính xác
                pstmt.setString(1, query);
            }


            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    books.add(extractBookFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tìm kiếm sách: " + e.getMessage());
        }
        return books;
    }

    // --- 1. TÌM KIẾM KHỚP CHÍNH XÁC ---
    public List<Book> searchBookByNameExact(String name) {
        String sql = "SELECT * FROM books WHERE name = ?";
        return executeSearchQuery(sql, name);
    }

    public List<Book> searchBookByAuthorExact(String author) {
        String sql = "SELECT * FROM books WHERE author = ?";
        return executeSearchQuery(sql, author);
    }

    // --- 2. TÌM KIẾM LINH HOẠT (CHỨA CHUỖI) ---
    public List<Book> searchBookByPartialName(String query) {
        String sql = "SELECT * FROM books WHERE name LIKE ?";
        return executeSearchQuery(sql, query);
    }

    // --- 3. TÌM KIẾM TỔNG HỢP (TÊN HOẶC TÁC GIẢ) ---
    public List<Book> searchBookCombined(String query) {
        String sql = "SELECT * FROM books WHERE name LIKE ? OR author LIKE ?";
        return executeSearchQuery(sql, query);
    }

    /**
     * Lấy tất cả sách, sắp xếp theo số lượt mượn giảm dần (dùng cho thống kê TOP).
     */
    public List<Book> getBooksSortedByBorrowCount() {
        List<Book> books = new ArrayList<>();
        // Sử dụng ORDER BY soLuotMuon DESC
        String sql = "SELECT * FROM books ORDER BY soLuotMuon DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                books.add(extractBookFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy sách theo số lượt mượn: " + e.getMessage());
        }
        return books;
    }
}