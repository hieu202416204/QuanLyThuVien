package Database;

import BackEnd.User.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /**
     * Chuyển đổi một dòng ResultSet thành đối tượng User.
     */
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getString("id"));
        user.setName(rs.getString("name"));
        // Sử dụng setter đặc biệt để đọc số sách đã mượn từ DB
        user.setSoSachDaMuonFromDB(rs.getInt("soSachDaMuon"));
        return user;
    }

    // =======================================================
    // I. CRUD CƠ BẢN
    // =======================================================

    /**
     * Thêm một người dùng mới vào cơ sở dữ liệu (Create).
     */
    public boolean addUser(User user) {
        // soSachDaMuon mặc định là 0 khi thêm mới
        String sql = "INSERT INTO users (id, name, soSachDaMuon) VALUES (?, ?, 0)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getName());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi thêm người dùng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa người dùng theo ID (Delete).
     */
    public boolean deleteUser(String userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa người dùng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy người dùng theo ID (Read).
     */
    public User getUserById(String userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy người dùng theo ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy tất cả người dùng, sắp xếp theo tên (dùng cho UI).
     */
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                userList.add(extractUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy tất cả người dùng: " + e.getMessage());
        }
        return userList;
    }

    // =======================================================
    // II. TÌM KIẾM VÀ CẬP NHẬT
    // =======================================================

    /**
     * Tìm kiếm người dùng theo tên (Partial Match) - Dùng cho UI tìm kiếm.
     */
    public List<User> searchUserByName(String userName) {
        List<User> userList = new ArrayList<>();
        // Dùng LIKE %...% để tìm kiếm chuỗi con không phân biệt chữ hoa/thường (ở SQLite)
        String sql = "SELECT * FROM users WHERE name LIKE ? ORDER BY name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + userName + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    userList.add(extractUserFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tìm kiếm người dùng theo tên: " + e.getMessage());
        }
        return userList;
    }

    /**
     * Cập nhật số sách đã mượn (Update - dùng cho logic Mượn/Trả sách).
     * @param userId ID người dùng cần cập nhật.
     * @param newCount Tổng số sách đã mượn mới.
     */
    public boolean updateUserBorrowedCount(String userId, int newCount) {
        String sql = "UPDATE users SET soSachDaMuon = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newCount);
            pstmt.setString(2, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật số sách đã mượn: " + e.getMessage());
            return false;
        }
    }

    // =======================================================
    // III. THỐNG KÊ
    // =======================================================

    /**
     * Lấy danh sách người dùng, sắp xếp theo số sách đã mượn giảm dần (dùng cho thống kê TOP).
     */
    public List<User> getUsersSortedByBorrowedCount() {
        List<User> userList = new ArrayList<>();
        // Sử dụng ORDER BY soSachDaMuon DESC
        String sql = "SELECT * FROM users ORDER BY soSachDaMuon DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                userList.add(extractUserFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy người dùng theo số sách đã mượn: " + e.getMessage());
        }
        return userList;
    }
}