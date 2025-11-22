package BackEnd.LibraryQ;
// Cần thêm các import: Database.BookDAO, Database.UserDAO, Database.TransactionDAO
// Loại bỏ các import: BackEnd.Book.BookInUserList, BackEnd.Histories.UserInUserHistory, BackEnd.Book.DayMT

import BackEnd.Book.Book;
import BackEnd.User.User;

public class QuanLyMuonTra {

    private final Library library; // Vẫn cần Library để lấy các DAO

    public QuanLyMuonTra(Library library) {
        this.library = library;
    }

    public String choMuonSach(String userId, String bookId) {
        Book book = library.getBookDAO().getBookById(bookId);
        User user = library.getUserDAO().getUserById(userId);

        if (book != null && user != null && book.isStatus()) {

            // 1. Cập nhật thống kê và trạng thái sách (Ghi vào DB)
            library.getBookDAO().updateBookStatus(
                    bookId,
                    false, // status = false (Đã mượn)
                    book.getSoLuotMuon() + 1
            );

            // 2. Cập nhật thống kê người dùng (Ghi vào DB)
            library.getUserDAO().updateUserBorrowedCount(
                    userId,
                    user.getSoSachDaMuon() + 1
            );

            // 3. Ghi lại giao dịch lịch sử
            library.getTransactionDAO().recordBorrow(userId, bookId);

            return "Mượn sách thành công: " + book.getName();
        }
        return "Lỗi: Không tìm thấy sách/người dùng hoặc sách đã được mượn.";
    }

    public String traSach(String userId, String bookId) {
        Book book = library.getBookDAO().getBookById(bookId);
        User user = library.getUserDAO().getUserById(userId);

        if (user != null && book != null && !book.isStatus()) {

            // Cần kiểm tra xem sách này có đang được mượn bởi người này không (optional nhưng nên có)
            if (!library.getTransactionDAO().isBookCurrentlyBorrowedByUser(userId, bookId)) {
                return "Lỗi: Người dùng không mượn sách này hoặc sách đang được mượn bởi người khác.";
            }

            // 1. Cập nhật trạng thái sách (Ghi vào DB)
            library.getBookDAO().updateBookStatus(
                    bookId,
                    true, // status = true (Có sẵn)
                    book.getSoLuotMuon() // Không tăng số lượt mượn khi trả
            );

            // 2. Cập nhật thống kê người dùng (Giảm số sách đang mượn - Optional, tùy logic thống kê)
            // Nếu soSachDaMuon chỉ đếm sách đã mượn TỔNG CỘNG thì không giảm.
            // Nếu đếm sách ĐANG mượn thì cần phải giảm (cần sửa logic UI nếu thế).
            // Giả định: Đếm TỔNG SỐ LƯỢT MƯỢN (nên không cần giảm).

            // 3. Ghi lại giao dịch trả
            library.getTransactionDAO().recordReturn(userId, bookId);

            return "Trả sách thành công: " + book.getName();
        }
        return "Lỗi: Không tìm thấy sách/người dùng hoặc sách chưa được mượn.";
    }
}