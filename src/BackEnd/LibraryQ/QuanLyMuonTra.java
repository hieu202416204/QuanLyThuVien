package BackEnd.LibraryQ;
import BackEnd.Book.Book;
import BackEnd.User.User;
import BackEnd.Utils.LanguageManager;
import com.google.zxing.PlanarYUVLuminanceSource;

public class QuanLyMuonTra {

    private final Library library; // Vẫn cần Library để lấy các DAO


    public QuanLyMuonTra(Library library) {
        this.library = library;
    }
    /**
     * Kiểm tra và tính tiền phạt (Đọc cấu hình động từ DB).
     */
    public long calculateFine(String userId, String bookId) {
        // 1. Lấy cấu hình từ DB (Nếu chưa cài đặt thì dùng mặc định: 60 ngày, 2000đ)
        String daysStr = library.getSettingsDAO().getSetting("max_borrow_days");
        String fineStr = library.getSettingsDAO().getSetting("fine_per_day");

        int maxDays = daysStr.isEmpty() ? 60 : Integer.parseInt(daysStr);
        long finePerDay = fineStr.isEmpty() ? 2000 : Long.parseLong(fineStr);

        // 2. Lấy ngày mượn từ DB
        String borrowDateStr = library.getTransactionDAO().getBorrowDateOfActiveTransaction(userId, bookId);
        if (borrowDateStr == null) return 0;

        try {
            // 3. Tính toán ngày (Giữ nguyên logic cũ)
            java.time.LocalDateTime borrowTime;
            if (borrowDateStr.contains(" ")) {
                borrowTime = java.time.LocalDateTime.parse(borrowDateStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                borrowTime = java.time.LocalDate.parse(borrowDateStr).atStartOfDay();
            }

            java.time.LocalDateTime returnTime = java.time.LocalDateTime.now();
            long daysBorrowed = java.time.temporal.ChronoUnit.DAYS.between(borrowTime, returnTime);

            // 4. Tính phạt theo cấu hình mới
            if (daysBorrowed > maxDays) {
                long overdueDays = daysBorrowed - maxDays;
                return overdueDays * finePerDay;
            }

        } catch (Exception e) {
            System.err.println("Lỗi tính phạt: " + e.getMessage());
        }

        return 0; // Không phạt
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

            return LanguageManager.getText("msg.borrow") + book.getName();
        }
        return LanguageManager.getText("msg.borrow_warning");
    }

    public String traSach(String userId, String bookId) {
        Book book = library.getBookDAO().getBookById(bookId);
        User user = library.getUserDAO().getUserById(userId);

        if (user != null && book != null && !book.isStatus()) {

            // Cần kiểm tra xem sách này có đang được mượn bởi người này không (optional nhưng nên có)
            if (!library.getTransactionDAO().isBookCurrentlyBorrowedByUser(userId, bookId)) {
                return LanguageManager.getText("msg.borrow_warning");
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

            // 3. Ghi lại giao dịch trả
            library.getTransactionDAO().recordReturn(userId, bookId);

            return LanguageManager.getText("msg.returnBook") + book.getName();
        }
        return LanguageManager.getText("msg.returnBook_warning");
    }
}