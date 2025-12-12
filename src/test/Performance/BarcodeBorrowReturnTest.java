package test.Performance; // Hoặc package test.Functional tùy cấu trúc của bạn

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.LibraryQ.QuanLyMuonTra;
import BackEnd.User.User;
import Database.DatabaseManager;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BarcodeBorrowReturnTest {

    private static Library library;
    private static QuanLyMuonTra qlMuonTra;

    // Giả lập mã vạch in trên thẻ và sách
    private final String USER_BARCODE = "USER_SCAN_001";
    private final String BOOK_BARCODE = "BOOK_SCAN_999";
    private final String INTRUDER_ID = "USER_INTRUDER"; // ID cho test 3

    @BeforeAll
    static void init() {
        DatabaseManager.initializeDatabase();
        library = new Library();
        qlMuonTra = new QuanLyMuonTra(library);
    }

    @BeforeEach
    void setupData() {
        // 1. Xóa dữ liệu cũ để tránh xung đột (Raw SQL)
        try (Connection conn = DatabaseManager.getConnection()) {
            // Xóa Transaction liên quan
            conn.createStatement().execute("DELETE FROM transactions WHERE book_id = '" + BOOK_BARCODE + "'");

            // Xóa User
            conn.createStatement().execute("DELETE FROM users WHERE id = '" + USER_BARCODE + "'");
            conn.createStatement().execute("DELETE FROM users WHERE id = '" + INTRUDER_ID + "'");

            // Xóa Book
            conn.createStatement().execute("DELETE FROM books WHERE id = '" + BOOK_BARCODE + "'");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 2. [QUAN TRỌNG] ĐỒNG BỘ HÓA CACHE
        // Vì ta vừa xóa DB bằng SQL thô, Cache trên RAM vẫn còn nhớ dữ liệu cũ.
        // Cần reload để Cache sạch sẽ giống DB.
        library.getBookDAO().reloadCache();

        // 3. Tạo dữ liệu mẫu sạch
        User u = new User(USER_BARCODE, "Test Scanner User");
        library.addUser(u);

        Book b = new Book(BOOK_BARCODE, "Test Scanner Book", "Author AI", "2025");
        // Đảm bảo sách mới thêm vào là Available
        b.setStatus(true);
        library.addBook(b);
    }

    @Test
    @Order(1)
    @DisplayName("✅ Test 1: Quét Mượn Sách (Normal Flow)")
    void testScanToBorrow() {
        System.out.println("--- Test 1: Quét mã để MƯỢN ---");

        // 1. Mượn
        String resultMsg = qlMuonTra.choMuonSach(USER_BARCODE, BOOK_BARCODE);
        System.out.println("Thông báo hệ thống: " + resultMsg);

        // 2. Kiểm tra DB: Sách phải là False (Đã mượn)
        Book dbBook = library.getBookDAO().getBookById(BOOK_BARCODE);
        assertFalse(dbBook.isStatus(), "Lỗi: Sách vẫn báo 'Có sẵn' sau khi quét mượn!");

        // 3. Kiểm tra DB: Transaction phải tồn tại
        boolean isBorrowed = library.getTransactionDAO().isBookCurrentlyBorrowedByUser(USER_BARCODE, BOOK_BARCODE);
        assertTrue(isBorrowed, "Lỗi: Không tìm thấy giao dịch Active trong DB!");
    }

    @Test
    @Order(2)
    @DisplayName("✅ Test 2: Quét Trả Sách (Normal Flow)")
    void testScanToReturn() {
        System.out.println("--- Test 2: Quét mã để TRẢ ---");

        // B1: Phải mượn trước (Setup data luôn tạo sách Available, nên cần mượn lại)
        String borrowMsg = qlMuonTra.choMuonSach(USER_BARCODE, BOOK_BARCODE);

        // Kiểm tra xem mượn bước đệm có thành công không
        if (borrowMsg.contains("Lỗi") || borrowMsg.contains("Error")) {
            fail("Setup mượn thất bại, không thể test trả. Msg: " + borrowMsg);
        }

        // B2: Thực hiện Trả
        String resultMsg = qlMuonTra.traSach(USER_BARCODE, BOOK_BARCODE);
        System.out.println("Thông báo hệ thống: " + resultMsg);

        // B3: Kiểm tra DB: Sách phải quay về True (Có sẵn)
        Book dbBook = library.getBookDAO().getBookById(BOOK_BARCODE);
        assertTrue(dbBook.isStatus(), "Lỗi: Sách chưa chuyển về trạng thái 'Có sẵn'!");

        // B4: Kiểm tra Transaction đã đóng chưa
        boolean stillBorrowed = library.getTransactionDAO().isBookCurrentlyBorrowedByUser(USER_BARCODE, BOOK_BARCODE);
        assertFalse(stillBorrowed, "Lỗi: Giao dịch vẫn còn treo!");
    }

    @Test
    @Order(3)
    @DisplayName("🚫 Test 3: Quét Mượn sách ĐANG ĐƯỢC MƯỢN (Fail)")
    void testBorrowAlreadyBorrowed() {
        System.out.println("--- Test 3: Mượn sách đang bận ---");

        // B1: User A mượn trước
        qlMuonTra.choMuonSach(USER_BARCODE, BOOK_BARCODE);

        // B2: Tạo User B (Intruder)
        library.addUser(new User(INTRUDER_ID, "Intruder"));

        // B3: User B cố tình quét mượn cuốn đó
        String resultMsg = qlMuonTra.choMuonSach(INTRUDER_ID, BOOK_BARCODE);
        System.out.println("Thông báo hệ thống: " + resultMsg);

        // B4: Kiểm tra
        // Sách vẫn phải thuộc về User A
        boolean ownedByA = library.getTransactionDAO().isBookCurrentlyBorrowedByUser(USER_BARCODE, BOOK_BARCODE);
        assertTrue(ownedByA, "Lỗi nghiêm trọng: Sách đang mượn bị người khác cướp lượt!");

        // User B không được phép có giao dịch này
        boolean ownedByB = library.getTransactionDAO().isBookCurrentlyBorrowedByUser(INTRUDER_ID, BOOK_BARCODE);
        assertFalse(ownedByB, "Lỗi: User B mượn được sách đang bận!");
    }

    @Test
    @Order(4)
    @DisplayName("⚠️ Test 4: Quét Mã Vạch Rác")
    void testScanInvalidCode() {
        System.out.println("--- Test 4: Quét mã không tồn tại ---");
        String fakeBookId = "UNKNOWN_CODE_123";

        String result = qlMuonTra.choMuonSach(USER_BARCODE, fakeBookId);
        System.out.println("Phản hồi: " + result);

        Book b = library.findBookById(fakeBookId);
        assertNull(b, "Lỗi: Mã rác không được tạo ra sách!");
    }
}