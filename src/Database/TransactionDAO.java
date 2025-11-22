package Database;

import BackEnd.Histories.UserInUserHistory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

    public List<UserInUserHistory> getUserHistory(String userId) {
        List<UserInUserHistory> historyList = new ArrayList<>();

        String sql =
                "SELECT t.borrow_date, t.return_date, b.name AS book_name, b.id AS book_id "
                        + "FROM transactions t JOIN books b ON t.book_id = b.id "
                        + "WHERE t.user_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    String bookName = rs.getString("book_name");
                    String bookId = rs.getString("book_id");

                    // ------ BORROW ------
                    LocalDateTime borrowTime = normalizeDateTime(rs.getString("borrow_date"));
                    historyList.add(new UserInUserHistory(
                            userId,
                            bookId,
                            "Mượn sách",
                            borrowTime,
                            bookName
                    ));

                    // ------ RETURN ------
                    String returnRaw = rs.getString("return_date");
                    if (returnRaw != null) {
                        LocalDateTime returnTime = normalizeDateTime(returnRaw);
                        historyList.add(new UserInUserHistory(
                                userId,
                                bookId,
                                "Trả sách",
                                returnTime,
                                bookName
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy lịch sử người dùng: " + e.getMessage());
        }

        historyList.sort(Comparator.comparing(UserInUserHistory::getLocalDateTime).reversed());
        return historyList;
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
}
