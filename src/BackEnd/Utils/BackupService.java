package BackEnd.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupService {

    private static final String DB_NAME = "library.db";

    /**
     * Tạo bản sao lưu cơ sở dữ liệu
     * @param targetFolder Thư mục lưu file backup
     * @return Đường dẫn file đã lưu
     */
    public static String backupDatabase(File targetFolder) throws IOException {
        File currentDB = new File(DB_NAME);
        if (!currentDB.exists()) {
            throw new IOException("Không tìm thấy file dữ liệu gốc!");
        }

        // Tạo tên file: library_backup_2023-10-25_15-30-00.db
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String backupFileName = "library_backup_" + timestamp + ".db";
        File backupFile = new File(targetFolder, backupFileName);

        // Copy file
        Files.copy(currentDB.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return backupFile.getAbsolutePath();
    }

    /**
     * Khôi phục dữ liệu từ file backup (Cần khởi động lại app sau khi restore)
     */
    public static void restoreDatabase(File backupFile) throws IOException {
        File currentDB = new File(DB_NAME);

        // Copy đè file backup vào file chính
        Files.copy(backupFile.toPath(), currentDB.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}