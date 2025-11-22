package BackEnd.Sattistics;

import BackEnd.LibraryQ.Library;
import BackEnd.User.User;
import Database.UserDAO;

import java.util.List;

public class UserStatistic {
    private final UserDAO userDAO = new UserDAO();
    Library library;
    public UserStatistic(Library library){
        this.library = library;
    }
    public List<User> danhSachNguoiDung(){
        return userDAO.getUsersSortedByBorrowedCount(); // Truy vấn DB
    }
}