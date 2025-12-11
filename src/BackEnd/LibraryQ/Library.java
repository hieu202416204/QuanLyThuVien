package BackEnd.LibraryQ;

import BackEnd.Book.Book;
import BackEnd.User.User;
import Database.*;

import java.util.List;

public class Library {

    // DAO CORE: Chỉ lưu trữ các lớp truy cập dữ liệu
    private final BookDAO bookDAO;
    private final UserDAO userDAO;
    private final TransactionDAO transactionDAO;
    private final SettingsDAO settingsDAO; // cho biet so luong ngay qua han duoc phep, so tien phai tra tu database
    private final FinancialDAO financialDAO;
    // Constructor: Khởi tạo các DAO
    public Library() {
        this.bookDAO = new BookDAO();
        this.userDAO = new UserDAO();
        this.transactionDAO = new TransactionDAO();
        this.settingsDAO = new SettingsDAO();
        this.financialDAO = new FinancialDAO();
    }

    // ----------------------------------------------------------------------
    // PHƯƠNG THỨC TRUY CẬP SÁCH (Book CRUD)
    // ----------------------------------------------------------------------

    public void addBook(Book book) {
        bookDAO.addBook(book);
    }

    public boolean deleteBook(String id) {
        return bookDAO.deleteBook(id);
    }

    public List<Book> getBooks() {
        return bookDAO.getAllBooks();
    }
    public SettingsDAO getSettingsDAO() {
        return settingsDAO;
    }

    public FinancialDAO getFinancialDAO() {
        return this.financialDAO;
    }

    public Book findBookById(String bookId) {
        return bookDAO.getBookById(bookId);
    }

    // ----------------------------------------------------------------------
    // PHƯƠNG THỨC TRUY CẬP NGƯỜI DÙNG (User CRUD)
    // ----------------------------------------------------------------------

    public void addUser(User user) {
        userDAO.addUser(user);
    }

    public boolean deleteUser(String userId) {
        return userDAO.deleteUser(userId);
    }

    public List<User> getListUsers() {
        return userDAO.getAllUsers();
    }

    public User findUserById(String userId) {
        return userDAO.getUserById(userId);
    }

    // Phương thức tìm kiếm (đã sửa để gọi DAO)
    public User searchUserById(String userId){
        return userDAO.getUserById(userId);
    }

    public List<User> searchUserByName(String userName){
        return userDAO.searchUserByName(userName);
    }

    // ----------------------------------------------------------------------
    // GETTER CHO DAO (ĐỂ CÁC LỚP KHÁC NHƯ QuanLyMuonTra TRUY CẬP)
    // ----------------------------------------------------------------------

    public BookDAO getBookDAO() {
        return bookDAO;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public TransactionDAO getTransactionDAO() {
        return transactionDAO;
    }

}