package test.Performance;

import BackEnd.Book.Book;
import Database.BookDAO;
import Database.DatabaseManager;
import org.junit.jupiter.api.Test;
import java.util.List;

class GalleryLoadTest {

    @Test
    void testLoadAllBooksForGallery() {
        DatabaseManager.initializeDatabase();
        BookDAO dao = new BookDAO();

        long start = System.currentTimeMillis();

        // 1. Lấy dữ liệu thô (Query DB)
        List<Book> allBooks = dao.getAllBooks();
        long dbTime = System.currentTimeMillis();

        System.out.println("-> DB Query Time: " + (dbTime - start) + "ms (Số lượng: " + allBooks.size() + ")");

        // 2. Mô phỏng việc tạo thẻ sách (UI Object Creation)
        // Lưu ý: Đây chỉ là test logic, không vẽ thật lên màn hình
        int count = 0;
        for (Book b : allBooks) {
            // Giả lập logic tạo Node
            String title = b.getName();
            String id = b.getId();
            // Giả lập đọc ảnh (tốn time)
            if (b.getImagePath() != null) {
                // simulateImageRead();
            }
            count++;
        }

        long uiTime = System.currentTimeMillis();
        System.out.println("-> Simulated UI Prep Time: " + (uiTime - dbTime) + "ms");

        // Nếu tổng thời gian > 1s mà không dùng Pagination, App sẽ đơ lúc mở tab
        if ((uiTime - start) > 1000) {
            System.err.println("⚠️ CẢNH BÁO: Cần dùng Phân trang (Pagination) ngay lập tức!");
        }
    }
}