package BackEnd.Utils;

import BackEnd.LibraryQ.Library;
import BackEnd.User.User;
import Database.VisitDAO;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

public class BarcodeScannerHandler {

    private final StringBuilder barcodeBuffer = new StringBuilder();
    private long lastKeystrokeTime = 0;

    // Ngưỡng thời gian giữa các phím để phân biệt máy quét và người gõ tay.
    // Máy quét thường < 50ms/ký tự. Người gõ > 100ms.
    private static final long THRESHOLD_MS = 100;

    private final VisitDAO visitDAO = new VisitDAO();
    private final Library library;

    public BarcodeScannerHandler(Library library) {
        this.library = library;
    }

    /**
     * Gắn bộ lắng nghe vào Scene chính của ứng dụng
     */
    public void attachToScene(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            long currentTime = System.currentTimeMillis();

            // Nếu người dùng nhập quá chậm, reset buffer (coi như đang gõ phím bình thường)
            if (currentTime - lastKeystrokeTime > THRESHOLD_MS && barcodeBuffer.length() > 0) {
                barcodeBuffer.setLength(0);
            }
            lastKeystrokeTime = currentTime;

            switch (event.getCode()) {
                case ENTER:
                    if (barcodeBuffer.length() > 0) {
                        String code = barcodeBuffer.toString().trim();
                        handleScannedCode(code);
                        barcodeBuffer.setLength(0); // Reset sau khi xử lý
                        event.consume(); // Chặn sự kiện Enter để không ảnh hưởng UI khác
                    }
                    break;
                default:
                    // Chỉ nhận ký tự chữ và số
                    if (event.getText() != null && !event.getText().isEmpty()) {
                        barcodeBuffer.append(event.getText());
                    }
                    break;
            }
        });
    }

    private void handleScannedCode(String code) {

        // 1. Lấy User từ Library (an toàn nhất)
        User user = library.getUserDAO().getUserById(code);

        if (user == null) {
            System.out.println("Not a user barcode: " + code);
            return;
        }

        // 2. Chạy VisitDAO trên thread riêng — nhưng không dùng AWT (tránh crash)
        new Thread(() -> {
            boolean ok = false;
            try {
                ok = visitDAO.checkIn(code);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (ok) {
                Platform.runLater(() -> {
                    System.out.println("CHECK-IN: " + code);
                });
            }
        }).start();
    }


    private void showNotification(String msg) {
        // Hiển thị thông báo nhỏ ở góc (Toast) hoặc System out
        // Ở đây dùng Alert tạm thời, tốt nhất nên dùng thư viện Toast (như ControlsFX)
        System.out.println("✅ CHECK-IN SUCCESS: " + msg);

        // Bạn có thể update 1 Label nhỏ ở góc màn hình HomeUI tại đây
    }

    private void playSoundSuccess() {
        // (Tùy chọn) Phát tiếng bíp
        try {
            // AudioClip beep = new AudioClip(getClass().getResource("/resources/beep.wav").toString());
            // beep.play();
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {}
    }
}