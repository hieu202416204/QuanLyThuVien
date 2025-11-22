package XuLiAnh;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageResizer {

    /**
     * Thay đổi kích thước ảnh với chất lượng cao, tránh vỡ ảnh.
     * * @param originalImage Đối tượng BufferedImage gốc.
     *
     * @param targetWidth  Chiều rộng đầu ra mong muốn.
     * @param targetHeight Chiều cao đầu ra mong muốn.
     * @return BufferedImage đã được thay đổi kích thước.
     */
    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {

        // 1. Tạo ảnh đầu ra với kích thước mục tiêu
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType());

        // 2. Lấy đối tượng Graphics2D để vẽ lên ảnh mới
        Graphics2D g2d = resizedImage.createGraphics();

        // 3. Thiết lập các gợi ý (RenderingHints) để đảm bảo chất lượng cao
        // QUALITY: Tăng cường chất lượng render tổng thể
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // INTERPOLATION: Sử dụng nội suy Bicubic (thường là tốt nhất) để tránh răng cưa/vỡ ảnh
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // ANTIALIASING: Làm mượt các đường viền
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 4. Vẽ ảnh gốc lên ảnh mới với kích thước mục tiêu
        // Tham số cuối cùng (null) là ImageObserver, không cần thiết trong BufferedImage
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);

        // 5. Giải phóng tài nguyên Graphics2D
        g2d.dispose();

        return resizedImage;
    }
}