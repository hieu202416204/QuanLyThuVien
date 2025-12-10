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
                    + "email TEXT,"
                    + "avatarPath TEXT,"
                    + "created_at TEXT,"
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
            // 4. TẠO BẢNG SETTINGS (Lưu cấu hình hệ thống)
            String sqlSettings = "CREATE TABLE IF NOT EXISTS settings ("
                    + "key_name TEXT PRIMARY KEY,"
                    + "value TEXT"
                    + ");";
            stmt.execute(sqlSettings);

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Lỗi khi kết nối hoặc khởi tạo DB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Xóa toàn bộ dữ liệu và khởi tạo lại cấu trúc bảng (Factory Reset)
     */
    public static void resetDatabase() {
        String dropTrans = "DROP TABLE IF EXISTS transactions";
        String dropBooks = "DROP TABLE IF EXISTS books";
        String dropUsers = "DROP TABLE IF EXISTS users";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Xóa bảng theo thứ tự (Transaction xóa trước vì có khóa ngoại)
            stmt.execute(dropTrans);
            stmt.execute(dropBooks);
            stmt.execute(dropUsers);

            System.out.println("All tables dropped.");

            // 2. Gọi lại hàm khởi tạo để tạo bảng mới trắng tinh
            initializeDatabase();

        } catch (SQLException e) {
            System.err.println("Lỗi khi reset DB: " + e.getMessage());
            e.printStackTrace();
        }
    }
}