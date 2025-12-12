package BackEnd.Utils;

import BackEnd.LibraryQ.Library;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupService {

    /**
     * Xuất toàn bộ dữ liệu (Sách, User, Lịch sử) ra các file CSV vào thư mục chỉ định.
     * @param targetFolder Thư mục người dùng chọn để lưu.
     * @param library Đối tượng Library để lấy dữ liệu.
     * @return Chuỗi thông báo kết quả.
     */
    public static String backupDataToCSV(File targetFolder, Library library) {
        // 1. Kiểm tra thư mục đích
        if (targetFolder == null || !targetFolder.exists()) {
            return "Thư mục đích không tồn tại.";
        }

        // Tạo tên file có gắn thời gian để tránh ghi đè và dễ quản lý
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        // 2. Định nghĩa tên các file
        File booksFile = new File(targetFolder, "Books_" + timestamp + ".csv");
        File usersFile = new File(targetFolder, "Users_" + timestamp + ".csv");
        File historyFile = new File(targetFolder, "History_" + timestamp + ".csv");

        StringBuilder resultMsg = new StringBuilder();
        boolean hasError = false;

        // 3. Thực hiện xuất Sách
        if (ExportUtil.exportBooksToCSV(library.getBooks(), booksFile)) {
            resultMsg.append("✅ Book: OK\n");
        } else {
            resultMsg.append("❌ Book: Lỗi\n");
            hasError = true;
        }

        // 4. Thực hiện xuất Người dùng
        if (ExportUtil.exportUsersToCSV(library.getListUsers(), usersFile)) {
            resultMsg.append("✅ User: OK\n");
        } else {
            resultMsg.append("❌ User: Lỗi\n");
            hasError = true;
        }

        // 5. Thực hiện xuất Lịch sử
        // Lưu ý: Cần đảm bảo TransactionDAO có hàm getAllTransactionsHistory() trả về List<UserInUserHistory>
        try {
            var historyList = library.getTransactionDAO().getAllTransactionsHistory();
            if (ExportUtil.exportHistoryToCSV(historyList, historyFile)) {
                resultMsg.append("✅ History: OK");
            } else {
                resultMsg.append("❌ History: Error loading file");
                hasError = true;
            }
        } catch (Exception e) {
            resultMsg.append("❌ History: Error DB (" + e.getMessage() + ")");
            hasError = true;
        }

        // Trả về kết quả tổng hợp
        if (hasError) return "ERROR:\n" + resultMsg.toString();
        return "true"; // Trả về "true" nếu tất cả đều ổn (để khớp với logic cũ trong SettingsTab)
    }
}