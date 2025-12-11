package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FinancialDAO {

    public FinancialDAO() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS finances (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "description TEXT, " +
                "amount REAL, " + // Số tiền
                "type TEXT, " +   // 'FINE' (Phạt), 'LOST' (Đền bù)
                "created_at TEXT)";
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void recordFine(String description, double amount) {
        String sql = "INSERT INTO finances (description, amount, type, created_at) VALUES (?, ?, 'FINE', datetime('now'))";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, description);
            pstmt.setDouble(2, amount);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public double getTotalRevenue() {
        String sql = "SELECT SUM(amount) FROM finances";
        try (Connection conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }
}