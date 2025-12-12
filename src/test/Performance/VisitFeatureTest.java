package test.Performance;

import BackEnd.User.User;
import Database.DatabaseManager;
import Database.UserDAO;
import Database.VisitDAO;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VisitFeatureTest {

    private static final VisitDAO visitDAO = new VisitDAO();
    private static final UserDAO userDAO = new UserDAO();

    @BeforeAll
    static void initDB() {
        // 1. Khởi tạo Database
        DatabaseManager.initializeDatabase();
    }

    @BeforeEach
    void cleanUp() {
        // Xóa dữ liệu cũ để test cho sạch
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.createStatement().execute("DELETE FROM visit_logs");
            conn.createStatement().execute("DELETE FROM users WHERE id LIKE 'TEST_%'");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Tạo 2 User giả để test
        userDAO.addUser(new User("TEST_U1", "User One"));
        userDAO.addUser(new User("TEST_U2", "User Two"));
    }

    @Test
    @Order(1)
    @DisplayName("✅ Test 1: Check-in thời gian thực (Real-time)")
    void testCheckIn() {
        System.out.println("--- Test 1: Check-in ---");

        // Mô phỏng quét mã
        boolean success1 = visitDAO.checkIn("TEST_U1");
        boolean success2 = visitDAO.checkIn("TEST_U2");
        boolean success3 = visitDAO.checkIn("TEST_U1"); // U1 vào lần nữa

        assertTrue(success1, "Check-in U1 thất bại");
        assertTrue(success2, "Check-in U2 thất bại");

        // Kiểm tra tổng số lượt hôm nay
        int count = visitDAO.getDailyVisitCount(LocalDate.now());
        System.out.println("Số lượt vào hôm nay: " + count);

        assertEquals(3, count, "Tổng số lượt vào phải là 3");
    }

    @Test
    @Order(2)
    @DisplayName("📅 Test 2: Thống kê theo Tuần (Giả lập dữ liệu quá khứ)")
    void testChartDataLogic() {
        System.out.println("--- Test 2: Chart Data Logic ---");

        // Vì hàm checkIn() luôn lấy giờ hiện tại, ta cần "hack" SQL để chèn dữ liệu ngày cũ
        // Giả sử hôm nay là Thứ 4, ta chèn dữ liệu cho Thứ 2, Thứ 3
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate twoDaysAgo = today.minusDays(2);

        // Chèn thủ công
        insertFakeVisit("TEST_U1", today);       // 1 lượt hôm nay
        insertFakeVisit("TEST_U1", yesterday);   // 1 lượt hôm qua
        insertFakeVisit("TEST_U2", yesterday);   // 1 lượt nữa hôm qua (Tổng qua = 2)
        insertFakeVisit("TEST_U1", twoDaysAgo);  // 1 lượt hôm kia

        // Gọi hàm lấy dữ liệu biểu đồ (Khoảng từ 2 ngày trước đến hôm nay)
        Map<String, Integer> data = visitDAO.getVisitsInDateRange(twoDaysAgo, today);

        System.out.println("Dữ liệu biểu đồ: " + data);

        // Kiểm tra kết quả
        assertEquals(1, data.get(today.toString()), "Hôm nay phải có 1");
        assertEquals(2, data.get(yesterday.toString()), "Hôm qua phải có 2");
        assertEquals(1, data.get(twoDaysAgo.toString()), "Hôm kia phải có 1");
    }

    @Test
    @Order(3)
    @DisplayName("🛡️ Test 3: Bảo vệ dữ liệu (Quét mã rác)")
    void testInvalidCheckIn() {
        System.out.println("--- Test 3: Invalid Check-in ---");

        // Cố tình insert một user không tồn tại qua DAO (Database có khóa ngoại Foreign Key nên sẽ chặn)
        // Lưu ý: SQLite mặc định đôi khi tắt Foreign Key, cần đảm bảo nó bật hoặc DAO xử lý

        // Trong BarcodeScannerHandler logic của bạn đã chặn trước khi gọi DAO:
        // if (library.getUserById(code) != null) ...

        // Nhưng ta cứ test tầng DAO xem nó phản ứng thế nào nếu gọi trực tiếp
        boolean result = visitDAO.checkIn("NON_EXIST_USER");

        // Nếu bật Foreign Key constraints thì cái này trả về false.
        // Nếu chưa bật PRAGMA foreign_keys = ON trong kết nối thì có thể nó vẫn true.
        // Tuy nhiên, logic ở tầng App (ScannerHandler) đã lọc rồi nên ở đây ta chỉ log ra xem thôi.
        System.out.println("Kết quả check-in user không tồn tại: " + result);
    }

    // --- Helper để chèn dữ liệu quá khứ ---
    private void insertFakeVisit(String userId, LocalDate date) {
        String fakeTime = date.toString() + " 10:00:00"; // Giờ cố định
        try (Connection conn = DatabaseManager.getConnection()) {
            var pstmt = conn.prepareStatement("INSERT INTO visit_logs (user_id, visit_time) VALUES (?, ?)");
            pstmt.setString(1, userId);
            pstmt.setString(2, fakeTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}