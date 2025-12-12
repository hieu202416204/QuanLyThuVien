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

    private static final String GUIDE_FILE_NAME = "Guide.docx"; // Tên file để trong resources
    private static final String RESOURCE_PATH = "/resources/" + GUIDE_FILE_NAME; // Đường dẫn trong code

    public static void openUserGuide() {
        try {
            // 1. Kiểm tra xem hệ thống có hỗ trợ mở file không
            if (!Desktop.isDesktopSupported()) {
                LibraryApp.showAlert(Alert.AlertType.ERROR, "Warning", "The system does not support opening this file.");
                return;
            }

            // 2. Lấy file từ trong resource (trong file JAR/Project)
            InputStream inputStream = HelpService.class.getResourceAsStream(RESOURCE_PATH);

            // Xử lý trường hợp không tìm thấy đường dẫn (do đặt sai file)
            if (inputStream == null) {
                // Thử tìm đường dẫn dự phòng (nếu bạn để trực tiếp trong resources không có sub-folder)
                inputStream = HelpService.class.getResourceAsStream("/" + GUIDE_FILE_NAME);
            }

            if (inputStream == null) {
                LibraryApp.showAlert(Alert.AlertType.ERROR, "Error", "Instruction file not found in the system!\n" +
                        "\n" +
                        "Please check the path again: " + RESOURCE_PATH);
                return;
            }

            // 3. Tạo một file tạm ở ngoài ổ cứng để hệ điều hành có thể đọc được
            // Tạo tên file tạm ngẫu nhiên để tránh xung đột
            File tempFile = File.createTempFile("Guide_", ".docx");

            // Đánh dấu để file này tự xóa khi tắt chương trình (tùy chọn)
            tempFile.deleteOnExit();

            // 4. Copy nội dung từ resource ra file tạm
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 5. Mở file bằng phần mềm mặc định (Word)
            Desktop.getDesktop().open(tempFile);

        } catch (IOException e) {
            e.printStackTrace();
            LibraryApp.showAlert(Alert.AlertType.ERROR, "Lỗi IO", "Không thể mở file: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            LibraryApp.showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi không xác định: " + e.getMessage());
        }
    }
}