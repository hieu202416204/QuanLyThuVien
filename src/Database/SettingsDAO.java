package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsDAO {

    /**
     * Lưu hoặc Cập nhật một cấu hình (Upsert)
     */
    public boolean saveSetting(String key, String value) {
        // Sử dụng REPLACE INTO của SQLite
        String sql = "REPLACE INTO settings (key_name, value) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);
            pstmt.setString(2, value);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi lưu setting: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy giá trị cấu hình theo key
     */
    public String getSetting(String key) {
        String sql = "SELECT value FROM settings WHERE key_name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đọc setting: " + e.getMessage());
        }
        return ""; // Trả về rỗng nếu không tìm thấy
    }
}