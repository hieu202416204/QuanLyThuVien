package BackEnd.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileUtil {

    // Tên thư mục chứa ảnh (nằm cùng cấp với file chạy của dự án)
    private static final String IMAGE_DIR = "images";

    /**
     * Copy file ảnh từ máy người dùng vào thư mục 'images' của dự án.
     * @param sourceFile File ảnh gốc người dùng chọn.
     * @return Tên file mới đã được lưu (ví dụ: "img_123456789.jpg").
     */
    public static String saveImageToLocal(File sourceFile) throws IOException {
        // 1. Tạo thư mục 'images' nếu chưa có
        File directory = new File(IMAGE_DIR);
        if (!directory.exists()) {
            directory.mkdir();
        }

        // 2. Tạo tên file mới ngẫu nhiên để tránh trùng tên (dùng UUID)
        String originalName = sourceFile.getName();
        String extension = "";
        int i = originalName.lastIndexOf('.');
        if (i > 0) {
            extension = originalName.substring(i); // Lấy đuôi .jpg, .png
        }

        // Tên file mới: img_randomChuoi.jpg
        String newFileName = "img_" + UUID.randomUUID().toString() + extension;

        // 3. File đích
        File destFile = new File(directory, newFileName);

        // 4. Thực hiện Copy
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return newFileName; // Chỉ trả về tên file để lưu vào DB
    }

    /**
     * Lấy đường dẫn tuyệt đối của file ảnh trong thư mục dự án để hiển thị.
     */
    public static String getLocalImagePath(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;

        // Trả về đường dẫn: [Thư mục dự án]/images/[tên file]
        File file = new File(IMAGE_DIR, fileName);
        if (file.exists()) {
            return file.toURI().toString(); // Trả về dạng URI để ImageView dễ đọc
        }
        return null;
    }

    /**
     * Kiểm tra xem file ảnh có tồn tại trong thư mục local không (dùng cho ImageIO)
     */
    public static File getLocalFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;
        return new File(IMAGE_DIR, fileName);
    }
    /**
     * Xóa toàn bộ ảnh trong thư mục images (Dùng cho Factory Reset)
     */
    public static void clearAllImages() {
        File directory = new File(IMAGE_DIR);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }
}