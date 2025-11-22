package XuLiAnh;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
public class ImageProcessor {

    private BufferedImage image;

    // Khởi tạo từ file ảnh người dùng tải lên
    public ImageProcessor(File file) throws IOException {
        this.image = ImageIO.read(file);
    }

    // Lấy ảnh gốc
    public BufferedImage getImage() {
        return image;
    }

    // Resize ảnh
    public BufferedImage resize(int width, int height) {
        Image tmp = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return resized;
    }

    // Chuyển ảnh sang grayscale
    public BufferedImage toGray() {
        BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return gray;
    }

    // Lưu ảnh ra file
    public void save(BufferedImage img, String format, File output) throws IOException {
        ImageIO.write(img, format, output);
    }
}
