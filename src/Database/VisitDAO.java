package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
public class VisitDAO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Ghi nhận một lượt vào thư viện
     */
    public boolean checkIn(String userId) {
        String sql = "INSERT INTO visit_logs (user_id, visit_time) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, LocalDateTime.now().format(DATE_TIME_FORMATTER));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Đếm số lượng người vào trong ngày hôm nay (Hoặc ngày bất kỳ)
     * Qua ngày mới, hàm này tự động trả về 0 do điều kiện WHERE thay đổi.
     */
    public int getDailyVisitCount(LocalDate date) {
        // Cắt chuỗi lấy 10 ký tự đầu (yyyy-MM-dd) để so sánh
        String sql = "SELECT COUNT(*) FROM visit_logs WHERE substr(visit_time, 1, 10) = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, date.toString()); // LocalDate.toString() trả về yyyy-MM-dd

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Lấy dữ liệu thống kê cho biểu đồ (Ví dụ: 7 ngày gần nhất)
     * @return Map<Ngày, Số lượng>
     */
    public Map<String, Integer> getVisitStats(int limitDays) {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT substr(visit_time, 1, 10) as day, COUNT(*) as count " +
                "FROM visit_logs " +
                "GROUP BY day " +
                "ORDER BY day DESC LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limitDays);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                stats.put(rs.getString("day"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
    /**
     * Thống kê số lượt vào theo từng ngày trong khoảng thời gian (Dùng cho xem Tuần và Tháng)
     * @param startDate Ngày bắt đầu (VD: Thứ 2)
     * @param endDate Ngày kết thúc (VD: Chủ Nhật)
     * @return Map<"2023-10-25", 15> (Ngày -> Số lượng)
     */
    public Map<String, Integer> getVisitsInDateRange(LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT substr(visit_time, 1, 10) as day, COUNT(*) as count " +
                "FROM visit_logs " +
                "WHERE day >= ? AND day <= ? " +
                "GROUP BY day";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate.toString());
            pstmt.setString(2, endDate.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("day"), rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Thống kê số lượt vào theo từng tháng trong năm (Dùng cho xem Năm)
     * @param year Năm cần xem (VD: 2025)
     * @return Map<Tháng (1-12), Số lượng>
     */
    public Map<Integer, Integer> getVisitsByMonthInYear(int year) {
        Map<Integer, Integer> result = new HashMap<>();
        // Cắt chuỗi lấy tháng: substr(visit_time, 6, 2)
        String sql = "SELECT cast(substr(visit_time, 6, 2) as integer) as month, COUNT(*) as count " +
                "FROM visit_logs " +
                "WHERE substr(visit_time, 1, 4) = ? " +
                "GROUP BY month";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, String.valueOf(year));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("month"), rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}