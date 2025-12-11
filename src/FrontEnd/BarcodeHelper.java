package FrontEnd;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class BarcodeHelper {

    /**
     * Tạo ảnh Mã vạch (Code 128) từ chuỗi văn bản
     */
    public static WritableImage createBarcode(String data, int width, int height) {
        try {
            // Code 128 là chuẩn mã vạch phổ biến nhất cho thẻ thành viên
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.CODE_128, width, height);

            WritableImage image = new WritableImage(width, height);
            PixelWriter pw = image.getPixelWriter();

            // Vẽ từng điểm ảnh (Pixel)
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    // Nếu bit là true -> vẽ màu đen, ngược lại trong suốt (hoặc trắng)
                    pw.setColor(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.TRANSPARENT);
                }
            }
            return image;

        } catch (Exception e) {
            System.err.println("Lỗi tạo Barcode: " + e.getMessage());
            return null;
        }
    }

    /**
     * Nếu thích QR Code hơn thì dùng hàm này (Vuông)
     */
    public static WritableImage createQRCode(String data, int size) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, size, size);
            WritableImage image = new WritableImage(size, size);
            PixelWriter pw = image.getPixelWriter();

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    pw.setColor(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.TRANSPARENT);
                }
            }
            return image;
        } catch (Exception e) {
            return null;
        }
    }
}