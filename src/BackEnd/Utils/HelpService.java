package BackEnd.Utils;

import FrontEnd.LibraryApp;
import javafx.scene.control.Alert;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class HelpService {

    // Tên file hướng dẫn (Bạn nên dùng PDF để máy nào cũng mở được)
    private static final String GUIDE_FILE_NAME = "Guide.docx";

    // Đường dẫn mặc định trong thư mục resources
    private static final String DEFAULT_PATH = "/resources/" + GUIDE_FILE_NAME;

    public static void openUserGuide() {
        // 1. Kiểm tra xem hệ điều hành có hỗ trợ mở file không (Ví dụ: Server Linux không có GUI sẽ lỗi)
        if (!Desktop.isDesktopSupported()) {
            LibraryApp.showAlert(Alert.AlertType.WARNING, "System Error",
                    "Hệ thống hiện tại không hỗ trợ tính năng mở tài liệu tự động.");
            return;
        }

        // 2. Tìm file trong Resources (Ưu tiên đường dẫn chuẩn -> Fallback ra gốc)
        // Sử dụng try-with-resources để input stream tự đóng sau khi dùng xong
        try (InputStream inputStream = getResourceStream()) {

            if (inputStream == null) {
                LibraryApp.showAlert(Alert.AlertType.ERROR, "File Not Found",
                        "Không tìm thấy file hướng dẫn!\n" +
                                "Vui lòng kiểm tra file tại: src/main/resources/resources/" + GUIDE_FILE_NAME);
                return;
            }

            // 3. Tạo file tạm thời (Temporary File) để HĐH có thể đọc được
            // Lấy đuôi file (.docx hoặc .pdf) từ tên file gốc
            String extension = GUIDE_FILE_NAME.substring(GUIDE_FILE_NAME.lastIndexOf("."));
            File tempFile = File.createTempFile("LibraryApp_UserGuide_", extension);

            // Đánh dấu để file tự xóa khi tắt ứng dụng (Dọn rác)
            tempFile.deleteOnExit();

            // 4. Copy dữ liệu từ trong file JAR/Exe ra file tạm
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 5. Mở file bằng phần mềm mặc định của máy (Word, PDF Reader...)
            Desktop.getDesktop().open(tempFile);

        } catch (IOException e) {
            e.printStackTrace();
            LibraryApp.showAlert(Alert.AlertType.ERROR, "IO Error",
                    "Lỗi khi đọc file hướng dẫn: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            LibraryApp.showAlert(Alert.AlertType.ERROR, "Error",
                    "Lỗi không xác định: " + e.getMessage());
        }
    }

    /**
     * Hàm phụ trợ để tìm file ở nhiều vị trí khác nhau
     * Giúp tránh lỗi khi chạy trong IDE vs chạy file JAR/EXE
     */
    private static InputStream getResourceStream() {
        // Thử tìm trong /resources/Guide.docx
        InputStream is = HelpService.class.getResourceAsStream(DEFAULT_PATH);

        // Nếu không thấy, thử tìm ngay ở thư mục gốc /Guide.docx
        if (is == null) {
            is = HelpService.class.getResourceAsStream("/" + GUIDE_FILE_NAME);
        }

        return is;
    }
}