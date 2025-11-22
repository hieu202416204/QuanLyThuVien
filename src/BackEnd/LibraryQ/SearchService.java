package BackEnd.LibraryQ;
// Cần thêm import: Database.BookDAO

import BackEnd.Book.Book;
import Database.BookDAO;

import java.util.List;

public class SearchService {
    private final BookDAO bookDAO = new BookDAO(); // Dùng BookDAO trực tiếp

    public SearchService(Library library) {
        // Không cần làm gì với library nếu dùng DAO trực tiếp
    }

    public List<Book> searchByName(String name) {
        return bookDAO.searchBookByNameExact(name);
    }

    public List<Book> searchByAuthor(String author) {
        return bookDAO.searchBookByAuthorExact(author);
    }

    // ... (Tương tự cho searchByPartialName và searchCombined) ...
}