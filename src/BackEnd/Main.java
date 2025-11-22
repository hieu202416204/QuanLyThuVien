package BackEnd;

import BackEnd.Book.Book;
import BackEnd.LibraryQ.Library;
import BackEnd.LibraryQ.QuanLyMuonTra;
import BackEnd.LibraryQ.SearchService;
import BackEnd.User.User;

public class Main {
    public static void main(String[] args) {
        Library lib = new Library();
        QuanLyMuonTra loanService = new QuanLyMuonTra(lib);
        SearchService searchService = new SearchService(lib);

        Book book1 = new Book("B001", "Lập trình Java cơ bản", "Nguyễn Văn A", "2019");
        Book book2 = new Book("B002", "Thuật toán nâng cao", "Trần Thị B", "2020");
        Book book3 = new Book("B003", "Cấu trúc dữ liệu", "Lê Văn C", "2018");
        Book book4 = new Book("B004", "Kỹ năng mềm cho sinh viên", "Phạm Thị D", "2021");
        Book book5 = new Book("B005", "Tư duy phản biện", "Ngô Văn E", "2022");
        Book book6 = new Book("B006", "Lịch sử Việt Nam", "Đặng Thị F", "2017");
        Book book7 = new Book("B007", "Marketing thời đại số", "Vũ Văn G", "2023");
        Book book8 = new Book("B008", "Thiết kế đồ họa", "Hoàng Thị H", "2020");
        Book book9 = new Book("B009", "Quản trị dự án", "Đỗ Văn I", "2021");
        Book book10 = new Book("B010", "Phân tích dữ liệu", "Bùi Thị J", "2024");
        lib.addBook(book1);
        lib.addBook(book2);
        lib.addBook(book3);
        lib.addBook(book4);
        lib.addBook(book5);
        lib.addBook(book6);
        lib.addBook(book7);
        lib.addBook(book8);
        lib.addBook(book9);
        lib.addBook(book10);

        User user1 = new User("202416204", "Hieu");
        lib.addUser(user1);
        loanService.choMuonSach("202416204", "B001");
        loanService.traSach("202416204", "B001");


    }
}