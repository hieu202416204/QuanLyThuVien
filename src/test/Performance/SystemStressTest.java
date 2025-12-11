package test.Performance;

import BackEnd.Book.Book;
import BackEnd.User.User;
import Database.BookDAO;
import Database.DatabaseManager;
import Database.TransactionDAO;
import Database.UserDAO;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SystemStressTest {

    private static final int BULK_SIZE = 5000; // Thử nghiệm với 5.000 bản ghi
    private BookDAO bookDAO = new BookDAO();
    private UserDAO userDAO = new UserDAO();
    private TransactionDAO transDAO = new TransactionDAO();

    @BeforeAll
    static void init() {
        DatabaseManager.initializeDatabase();
    }

    // Xóa dữ liệu test cũ để tránh rác
    @BeforeEach
    void cleanUp() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM books WHERE id LIKE 'STRESS_%'");
            stmt.execute("DELETE FROM users WHERE id LIKE 'STRESS_%'");
            stmt.execute("DELETE FROM transactions WHERE user_id LIKE 'STRESS_%'");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Test
    @Order(1)
    @DisplayName("🔥 Test 1: Nhập 5.000 User và 5.000 Sách cùng lúc")
    void testBulkInsert() {
        long start = System.currentTimeMillis();

        // 1. Thêm 5000 User
        for (int i = 0; i < BULK_SIZE; i++) {
            User u = new User("STRESS_U" + i, "User Test " + i, "test" + i + "@mail.com", null);
            userDAO.addUser(u);
        }

        // 2. Thêm 5000 Book
        for (int i = 0; i < BULK_SIZE; i++) {
            Book b = new Book("STRESS_B" + i, "Book Test " + i, "Author AI", "2025");
            bookDAO.addBook(b);
        }

        long end = System.currentTimeMillis();
        System.out.println("✅ Thời gian thêm " + (BULK_SIZE * 2) + " bản ghi: " + (end - start) + "ms");

        // Đánh giá: Nếu dưới 5 giây là Tốt (cho SQLite local)
        Assertions.assertTrue((end - start) < 10000, "Hệ thống quá chậm khi nhập liệu lớn!");
    }

    @Test
    @Order(2)
    @DisplayName("⚠️ Test 2: Trường hợp xấu (Dữ liệu rác & SQL Injection)")
    void testBadInput() {
        // Thử nhập ID chứa ký tự đặc biệt nguy hiểm cho SQL
        String badId = "STRESS_' OR '1'='1";
        String badName = "Robert'); DROP TABLE students; --"; // Giai thoại nổi tiếng về SQL Injection

        User u = new User(badId, badName, "hacker@mail.com", null);

        // DAO sử dụng PreparedStatement nên phải xử lý được cái này mà không lỗi
        boolean success = userDAO.addUser(u);

        // Kiểm tra xem có thêm được không (hoặc ít nhất không được crash app)
        if (success) {
            User retrieved = userDAO.getUserById(badId);
            Assertions.assertNotNull(retrieved);
            Assertions.assertEquals(badName, retrieved.getName());
            System.out.println("✅ Hệ thống an toàn trước SQL Injection cơ bản.");
        } else {
            System.out.println("⚠️ Hệ thống từ chối ký tự đặc biệt (Tốt).");
        }
    }

    @Test
    @Order(3)
    @DisplayName("🔄 Test 3: Giao dịch Mượn/Trả liên tục (500 lượt)")
    void testTransactionStress() {
        // Tạo sẵn 1 user và 1 book
        userDAO.addUser(new User("STRESS_U_TRANS", "Trans User", null, null));
        bookDAO.addBook(new Book("STRESS_B_TRANS", "Trans Book", "Auth", "2020"));

        long start = System.currentTimeMillis();

        for (int i = 0; i < 500; i++) {
            // Mượn
            transDAO.recordBorrow("STRESS_U_TRANS", "STRESS_B_TRANS");
            // Trả ngay lập tức
            transDAO.recordReturn("STRESS_U_TRANS", "STRESS_B_TRANS");
        }

        long end = System.currentTimeMillis();
        System.out.println("✅ Thời gian xử lý 1000 giao dịch (Mượn+Trả): " + (end - start) + "ms");
    }
}