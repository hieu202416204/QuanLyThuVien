package Database;

import BackEnd.Histories.UserInUserHistory;
import BackEnd.LibraryQ.Library;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TransactionDAO {

    // Format chuẩn có giờ (ghi mới)
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Format chuẩn chỉ ngày (để nhận dạng dữ liệu cũ)
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // =======================================================
    //  HÀM CHUẨN HÓA NGÀY GIỜ – XỬ LÝ CẢ DỮ LIỆU CŨ
    // =======================================================

    /**
     * Chuẩn hóa giá trị thời gian từ DB.
     * - "yyyy-MM-dd HH:mm:ss"  → parse chuẩn
     * - "yyyy-MM-dd"           → gán giờ mặc định 00:00:00
     * - null                   → null
     */
    private LocalDateTime normalizeDateTime(String raw) {
        if (raw == null) return null;

        try {
            // Nếu có giờ (dữ liệu mới)
            if (raw.contains(" ")) {
                return LocalDateTime.parse(raw, DATE_TIME_FORMATTER);
            }
            // Nếu chỉ có ngày (dữ liệu cũ)
            return LocalDateTime.parse(raw + " 00:00:00", DATE_TIME_FORMATTER);

        } catch (Exception e) {
            System.err.println("Lỗi normalizeDateTime: '" + raw + "' | " + e.getMessage());
            return null;
        }
    }

    // =======================================================
    // I. GHI GIAO DỊCH
    // =======================================================

    public boolean recordBorrow(String userId, String bookId) {
        String sql = "INSERT INTO transactions (user_id, book_id, borrow_date, status) "
                + "VALUES (?, ?, ?, 'BORROWED')";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, bookId);

            // Ghi ngày giờ thực tế
            pstmt.setString(3, LocalDateTime.now().format(DATE_TIME_FORMATTER));

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi ghi giao dịch mượn: " + e.getMessage());
            return false;
        }
    }


    public boolean recordReturn(String userId, String bookId) {
        String sql =
                "UPDATE transactions SET return_date = ?, status = 'RETURNED' "
                        + "WHERE user_id = ? AND book_id = ? AND status = 'BORROWED' LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, LocalDateTime.now().format(DATE_TIME_FORMATTER));
            pstmt.setString(2, userId);
            pstmt.setString(3, bookId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi ghi giao dịch trả: " + e.getMessage());
            return false;
        }
    }

    // =======================================================
    // II. LỊCH SỬ NGƯỜI DÙNG
    // =======================================================

    /**
     * Lấy lịch sử mượn trả sách của MỘT NGƯỜI DÙNG CỤ THỂ
     */
    public List<String[]> getUserHistory(String userId) {
        List<String[]> list = new ArrayList<>();
        // Kết hợp (JOIN) bảng transactions và books để lấy tên sách cho thân thiện
        String sql = "SELECT b.name, t.borrow_date, t.return_date, t.status " +
                "FROM transactions t " +
                "JOIN books b ON t.book_id = b.id " +
                "WHERE t.user_id = ? ORDER BY t.borrow_date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    list.add(new String[]{
                            rs.getString(1), // Tên sách
                            rs.getString(2), // Ngày mượn
                            rs.getString(3), // Ngày trả
                            rs.getString(4)  // Trạng thái (BORROWED / RETURNED)
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // =======================================================
    // III. LỊCH SỬ TOÀN BỘ
    // =======================================================

    public List<UserInUserHistory> getAllTransactionsHistory() {
        List<UserInUserHistory> historyList = new ArrayList<>();

        String sql = "SELECT t.user_id, t.borrow_date, t.return_date, b.name AS book_name "
                + "FROM transactions t JOIN books b ON t.book_id = b.id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String userId = rs.getString("user_id");
                String bookName = rs.getString("book_name");

                LocalDateTime borrowTime = normalizeDateTime(rs.getString("borrow_date"));
                historyList.add(new UserInUserHistory(
                        userId, null, "Mượn", borrowTime, bookName));

                String returnRaw = rs.getString("return_date");
                if (returnRaw != null) {
                    LocalDateTime returnTime = normalizeDateTime(returnRaw);
                    historyList.add(new UserInUserHistory(
                            userId, null, "Trả", returnTime, bookName));
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy tất cả lịch sử giao dịch: " + e.getMessage());
        }

        historyList.sort(Comparator.comparing(UserInUserHistory::getLocalDateTime).reversed());
        return historyList;
    }

    // =======================================================
    // IV. LỊCH SỬ THEO SÁCH
    // =======================================================

    public List<String[]> getBookHistory(String bookId) {
        List<String[]> list = new ArrayList<>();

        String sql = "SELECT user_id, borrow_date, return_date "
                + "FROM transactions WHERE book_id = ? ORDER BY borrow_date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bookId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString("user_id"),
                            rs.getString("borrow_date"),
                            rs.getString("return_date")
                    });
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy lịch sử sách: " + e.getMessage());
        }

        return list;
    }

    // =======================================================
    // V. XÓA LỊCH SỬ
    // =======================================================

    public boolean deleteTransactionsByUserId(String userId) {
        String sql = "DELETE FROM transactions WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa lịch sử giao dịch của người dùng: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTransactionsByBookId(String bookId) {
        String sql = "DELETE FROM transactions WHERE book_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bookId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa lịch sử giao dịch của sách: " + e.getMessage());
            return false;
        }
    }

    // =======================================================
    // VI. KIỂM TRA SÁCH ĐANG MƯỢN
    // =======================================================

    public boolean isBookCurrentlyBorrowedByUser(String userId, String bookId) {
        String sql =
                "SELECT COUNT(*) FROM transactions "
                        + "WHERE user_id = ? AND book_id = ? AND status = 'BORROWED'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, bookId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra sách đang mượn: " + e.getMessage());
        }

        return false;
    }
    /**
     * Lấy ngày mượn của một giao dịch đang hoạt động (chưa trả).
     * @return Chuỗi ngày mượn (yyyy-MM-dd HH:mm:ss) hoặc null nếu không tìm thấy.
     */
    public String getBorrowDateOfActiveTransaction(String userId, String bookId) {
        String sql = "SELECT borrow_date FROM transactions "
                + "WHERE user_id = ? AND book_id = ? AND status = 'BORROWED'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, bookId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("borrow_date");
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy ngày mượn: " + e.getMessage());
        }
        return null;
    }
    // =======================================================
    // VIII. LẤY DANH SÁCH ĐANG MƯỢN (CHO UI MƯỢN/TRẢ)
    // =======================================================
    public List<BackEnd.Histories.CurrentTransaction> getCurrentlyBorrowedBooks() {
        List<BackEnd.Histories.CurrentTransaction> list = new ArrayList<>();

        // Join 3 bảng để lấy đầy đủ tên người và tên sách
        String sql = "SELECT t.user_id, u.name as user_name, t.book_id, b.name as book_name, t.borrow_date " +
                "FROM transactions t " +
                "JOIN users u ON t.user_id = u.id " +
                "JOIN books b ON t.book_id = b.id " +
                "WHERE t.status = 'BORROWED'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // Xử lý ngày tháng
                LocalDateTime dateTime = normalizeDateTime(rs.getString("borrow_date"));
                LocalDate date = (dateTime != null) ? dateTime.toLocalDate() : LocalDate.now();

                list.add(new BackEnd.Histories.CurrentTransaction(
                        rs.getString("user_id"),
                        rs.getString("user_name"),
                        rs.getString("book_id"),
                        rs.getString("book_name"),
                        date
                ));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy danh sách đang mượn: " + e.getMessage());
        }
        return list;
    }
    // Lấy danh sách giao dịch quá hạn có kèm Email người dùng
    public List<String[]> getOverdueTransactionsWithEmail(int limitDay) {
        List<String[]> list = new ArrayList<>();
        //===============================================================
        // quá hạn giả sử 60 ngày
        String sql = "SELECT u.name, u.email, b.name as book_name, t.borrow_date " +
                "FROM transactions t " +
                "JOIN users u ON t.user_id = u.id " +
                "JOIN books b ON t.book_id = b.id " +
                "WHERE t.status = 'BORROWED' AND u.email IS NOT NULL AND u.email != ''";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                LocalDateTime borrowTime = normalizeDateTime(rs.getString("borrow_date"));
                long days = java.time.temporal.ChronoUnit.DAYS.between(borrowTime, LocalDateTime.now());
//========================================================================
                if (days > limitDay) { // Nếu quá 60 ngày
                    list.add(new String[]{
                            rs.getString("email"),
                            rs.getString("name"),
                            rs.getString("book_name"),
                            String.valueOf(days)
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    // --- Lấy danh sách ngày mượn/trả của các giao dịch ĐÃ TRẢ ---
    public List<long[]> getReturnDurations() {
        List<long[]> list = new ArrayList<>();
        String sql = "SELECT borrow_date, return_date FROM transactions WHERE status = 'RETURNED'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while(rs.next()) {
                LocalDateTime borrow = normalizeDateTime(rs.getString("borrow_date"));
                LocalDateTime ret = normalizeDateTime(rs.getString("return_date"));

                if (borrow != null && ret != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(borrow, ret);
                    list.add(new long[]{days});
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
    // =======================================================
    // XÓA 1 DÒNG LỊCH SỬ CỤ THỂ
    // =======================================================
    public boolean deleteHistoryRecord(String userId, String bookName, LocalDateTime time) {
        if (time == null) return false;

        // 1. Chuỗi có đầy đủ giờ phút giây
        String exactTimeStr = time.format(DATE_TIME_FORMATTER); // VD: 2024-02-24 10:20:30

        // 2. Chuỗi chỉ có ngày
        String shortTimeStr = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")); // VD: 2024-02-24

        // Bao phủ toàn bộ các trường hợp lưu trữ thời gian của SQLite
        String sql = "DELETE FROM transactions " +
                "WHERE user_id = ? " +
                "AND book_id IN (SELECT id FROM books WHERE name = ?) " +
                "AND (" +
                "borrow_date = ? OR return_date = ? OR " +    // Trùng khớp hoàn toàn
                "borrow_date = ? OR return_date = ? OR " +    // Trùng khớp dữ liệu cũ
                "borrow_date LIKE ? OR return_date LIKE ?" +  // Đề phòng DB có đuôi mili-giây .000
                ")";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, bookName);

            // Truyền tham số cho nhóm exactTime
            pstmt.setString(3, exactTimeStr);
            pstmt.setString(4, exactTimeStr);

            // Truyền tham số cho nhóm shortTime (dữ liệu cũ)
            pstmt.setString(5, shortTimeStr);
            pstmt.setString(6, shortTimeStr);

            // Truyền tham số cho nhóm LIKE
            pstmt.setString(7, exactTimeStr + "%");
            pstmt.setString(8, exactTimeStr + "%");

            // Nếu > 0 nghĩa là đã xóa thành công ít nhất 1 dòng
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi xóa lịch sử: " + e.getMessage());
            return false;
        }
    }

}
