package test.Performance;

import Database.BookDAO;
import Database.DatabaseManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchLagTest {

    @Test
    void testSearchResponsiveness() {
        DatabaseManager.initializeDatabase();
        BookDAO dao = new BookDAO();

        String keyword = "Java"; // Từ khóa phổ biến

        long start = System.nanoTime();
        var results = dao.searchBookByPartialName(keyword);
        long end = System.nanoTime();

        double durationMs = (end - start) / 1_000_000.0;

        System.out.println("🔎 Tìm thấy " + results.size() + " kết quả trong " + durationMs + "ms");

        // Tiêu chuẩn UX: Tìm kiếm phải dưới 100ms để cảm thấy "mượt"
        // Nếu > 100ms, người dùng sẽ thấy khựng khi gõ phím
        if (durationMs > 100) {
            System.err.println("⚠️ CẢNH BÁO: Tìm kiếm quá chậm! Cần tối ưu Index.");
        }

        assertTrue(durationMs < 500, "Tìm kiếm không được quá 0.5s");
    }
}