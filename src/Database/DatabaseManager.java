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
                    +"category TEXT,"
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
// --- 5. TỐI ƯU HÓA: TẠO INDEX ---
            // Phần này giúp tìm kiếm NHANH GẤP NHIỀU LẦN

            // Index cho tìm kiếm sách theo Tên
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_name ON books(name)");

            // Index cho tìm kiếm sách theo Tác giả
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_author ON books(author)");

            // Index cho tìm kiếm người dùng theo Tên
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_name ON users(name)");

            // Index cho trạng thái giao dịch (để lọc sách đang mượn nhanh hơn)
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_trans_status ON transactions(status)");

            // Index cho khóa ngoại (tăng tốc độ Join bảng khi xem lịch sử)
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_trans_userid ON transactions(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_trans_bookid ON transactions(book_id)");
// 6. TẠO BẢNG VISIT_LOGS (Lịch sử ra vào)
// Lưu từng lượt quét để sau này tính toán thống kê chi tiết
            String sqlVisits = "CREATE TABLE IF NOT EXISTS visit_logs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "user_id TEXT NOT NULL,"
                    + "visit_time TEXT," // Lưu yyyy-MM-dd HH:mm:ss
                    + "FOREIGN KEY(user_id) REFERENCES users(id)"
                    + ");";
            stmt.execute(sqlVisits);

// Tạo Index cho ngày để truy vấn thống kê nhanh
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_visit_time ON visit_logs(visit_time)");
            System.out.println("Database initialized successfully with Indexes.");

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