package BackEnd.LibraryQ;
// Cần thêm các import: Database.BookDAO, Database.UserDAO, Database.TransactionDAO
// Loại bỏ các import: BackEnd.Book.BookInUserList, BackEnd.Histories.UserInUserHistory, BackEnd.Book.DayMT

import BackEnd.Book.Book;
import BackEnd.User.User;

public class QuanLyMuonTra {
    private static final int MAX_DAYS_ALLOWED = 60; // Số ngày tối đa được mượn
    private static final long FINE_PER_DAY = 20000;  // Phạt 20000đ mỗi ngày quá hạn

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
    /**
     * Kiểm tra và tính tiền phạt (nếu có).
     * @return Số tiền phạt (VNĐ). Trả về 0 nếu không quá hạn hoặc lỗi.
     */
    public long calculateFine(String userId, String bookId) {
        // 1. Lấy ngày mượn từ DB
        String borrowDateStr = library.getTransactionDAO().getBorrowDateOfActiveTransaction(userId, bookId);

        if (borrowDateStr == null) return 0; // Không tìm thấy giao dịch

        try {
            // 2. Chuyển đổi chuỗi ngày thành LocalDateTime
            // Lưu ý: TransactionDAO dùng format "yyyy-MM-dd HH:mm:ss" hoặc "yyyy-MM-dd"
            java.time.LocalDateTime borrowTime;
            if (borrowDateStr.contains(" ")) {
                borrowTime = java.time.LocalDateTime.parse(borrowDateStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                borrowTime = java.time.LocalDate.parse(borrowDateStr).atStartOfDay();
            }

            // 3. Tính khoảng cách ngày
            java.time.LocalDateTime returnTime = java.time.LocalDateTime.now();
            long daysBorrowed = java.time.temporal.ChronoUnit.DAYS.between(borrowTime, returnTime);

            // 4. Tính phạt
            if (daysBorrowed > MAX_DAYS_ALLOWED) {
                long overdueDays = daysBorrowed - MAX_DAYS_ALLOWED;
                return overdueDays * FINE_PER_DAY;
            }

        } catch (Exception e) {
            System.err.println("Lỗi tính phạt: " + e.getMessage());
        }

        return 0; // Không phạt
    }
}