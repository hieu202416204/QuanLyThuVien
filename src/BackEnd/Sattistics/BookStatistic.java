package BackEnd.Sattistics;
// Cần thêm import: Database.BookDAO, Database.UserDAO

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import Database.BookDAO;

import java.util.List;

public class BookStatistic {
    private final BookDAO bookDAO = new BookDAO();
    Library library;
    public BookStatistic(Library library){
        this.library = library;
    }
    public List<Book> getTopBook(){
        return bookDAO.getBooksSortedByBorrowCount(); // Truy vấn DB
    }
}

