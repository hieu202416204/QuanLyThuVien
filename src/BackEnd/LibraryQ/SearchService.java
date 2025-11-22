package BackEnd.LibraryQ;

import BackEnd.Book.Book;
import Database.BookDAO;

import java.util.List;

public class SearchService {
    // Sử dụng BookDAO để truy cập DB
    private final BookDAO bookDAO = new BookDAO();

    public SearchService(Library library) {
        // Khởi tạo (hoặc có thể bỏ qua tham số library nếu không dùng)
    }

    // --- 1. TÌM KIẾM THEO TÊN (Linh hoạt: chứa chuỗi) ---
    // (Thay thế searchBookByNameExact bằng searchBookByPartialName)
    public List<Book> searchByName(String name) {
        // Tên method trong BookDAO là searchBookByPartialName
        return bookDAO.searchBookByPartialName(name);
    }

    // --- 2. TÌM KIẾM THEO TÁC GIẢ (Linh hoạt: chứa chuỗi) ---
    // (BookDAO không có partial cho Author, nên tạm dùng Exact hoặc tạo mới trong DAO)
    // Giả sử ta muốn dùng searchCombined để tìm theo tên/tác giả chung:
    public List<Book> searchByAuthor(String author) {
        // Nếu muốn tìm kiếm linh hoạt:
        return bookDAO.searchBookCombined(author);
        // Nếu muốn khớp chính xác:
        // return bookDAO.searchBookByAuthorExact(author);
    }

    // --- 3. TÌM KIẾM TỔNG HỢP (Tên HOẶC Tác giả - Linh hoạt) ---
    public List<Book> searchCombined(String query) {
        // Phương thức này có trong BookDAO
        return bookDAO.searchBookCombined(query);
    }

    // Phương thức đã dùng cho chức năng searchByName cũ (tùy chọn)
    public List<Book> searchByPartialName(String query) {
        return bookDAO.searchBookByPartialName(query);
    }
}