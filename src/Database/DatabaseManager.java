package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:library.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. TẠO BẢNG BOOKS
            String sqlBooks = "CREATE TABLE IF NOT EXISTS books ("
                    + "id TEXT PRIMARY KEY,"
                    + "name TEXT NOT NULL,"
                    + "author TEXT,"
                    + "year TEXT,"
                    + "status INTEGER NOT NULL," // 1 (true) là Available, 0 (false) là Borrowed
                    + "imagePath TEXT,"
                    + "soLuotMuon INTEGER DEFAULT 0"
                    + ");";
            stmt.execute(sqlBooks);

            // 2. TẠO BẢNG USERS
            String sqlUsers = "CREATE TABLE IF NOT EXISTS users ("
                    + "id TEXT PRIMARY KEY,"
                    + "name TEXT NOT NULL,"
                    + "soSachDaMuon INTEGER DEFAULT 0"
                    + ");";
            stmt.execute(sqlUsers);

            // 3. TẠO BẢNG TRANSACTIONS (Lịch sử Mượn/Trả)
            String sqlTransactions = "CREATE TABLE IF NOT EXISTS transactions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "user_id TEXT NOT NULL,"
                    + "book_id TEXT NOT NULL,"
                    + "borrow_date TEXT NOT NULL,"
                    + "return_date TEXT," // NULL nếu sách đang được mượn
                    + "status TEXT NOT NULL," // 'BORROWED' hoặc 'RETURNED'
                    + "FOREIGN KEY(user_id) REFERENCES users(id),"
                    + "FOREIGN KEY(book_id) REFERENCES books(id)"
                    + ");";
            stmt.execute(sqlTransactions);

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Lỗi khi kết nối hoặc khởi tạo DB: " + e.getMessage());
            e.printStackTrace();
        }
    }
}