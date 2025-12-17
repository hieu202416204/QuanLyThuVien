package BackEnd.Utils;

import BackEnd.LibraryQ.Library;
import BackEnd.User.User;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class BarcodeScannerHandler {
    private final Library library;
    private final StringBuilder barcodeBuffer = new StringBuilder();
    private long lastKeystrokeTime = 0;

    // Thời gian tối đa giữa 2 ký tự để coi là máy quét (ms)
    // Máy quét thường bắn rất nhanh (<50ms), người gõ phím thì chậm hơn
    private static final long SCANNER_THRESHOLD = 100;

    public BarcodeScannerHandler(Library library) {
        this.library = library;
    }

    public void attachToScene(Scene scene) {
        // Sử dụng addEventFilter để "đón lõng" sự kiện ngay từ gốc (trước khi đến Button hay TextField)
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            // 1. KIỂM TRA QUAN TRỌNG:
            // Nếu người dùng đang focus vào một ô nhập liệu (TextField, TextArea...)
            // -> THÌ BỎ QUA NGAY (để ô đó tự xử lý việc nhập/quét)
            Node focusOwner = scene.getFocusOwner();
            if (focusOwner instanceof TextInputControl) {
                return;
            }

            // 2. Logic phát hiện máy quét (Dựa trên tốc độ nhập)
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastKeystrokeTime > SCANNER_THRESHOLD) {
                // Nếu khoảng cách giữa 2 phím quá lâu -> Người dùng đang gõ tay -> Reset buffer
                barcodeBuffer.setLength(0);
            }
            lastKeystrokeTime = currentTime;

            // 3. Xử lý ký tự
            if (event.getCode() == KeyCode.ENTER) {
                // Nếu nhấn Enter và buffer có dữ liệu -> XỬ LÝ QUÉT
                if (barcodeBuffer.length() > 0) {
                    String code = barcodeBuffer.toString().trim();
                    handleGlobalCheckIn(code); // Gọi hàm xử lý

                    barcodeBuffer.setLength(0); // Reset sau khi xử lý
                    event.consume(); // Chặn sự kiện Enter để không kích hoạt nút bấm đang focus
                }
            } else {
                // Chỉ thu thập ký tự in được (chữ, số)
                if (event.getText().length() == 1) {
                    barcodeBuffer.append(event.getText());
                }
            }
        });
    }

    // --- LOGIC CHECK-IN NGƯỜI DÙNG ---
    private void handleGlobalCheckIn(String userId) {
        userId = userId.toUpperCase();
        System.out.println("Global Scanner detected: " + userId);

        // Tìm User trong hệ thống
        User user = library.searchUserById(userId);

        if (user != null) {
            String finalUserId = userId;
            Platform.runLater(() -> {
                // Thực hiện logic check-in
                library.checkIn(finalUserId);

                // Hiển thị thông báo nhỏ hoặc popup
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Check-in Success");
                alert.setHeaderText("Welcome, " + user.getName());
                alert.setContentText("Check-in time recorded.");
                alert.show();

                // Tự động đóng popup sau 2 giây (để đỡ phải click)
                new Thread(() -> {
                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
                    Platform.runLater(alert::close);
                }).start();
            });
        } else {
            // Nếu quét mã không phải User (ví dụ quét nhầm mã sách ở màn hình chính)
            // Thì có thể bỏ qua hoặc báo lỗi nhẹ
            System.out.println("Unknown code scanned globally: " + userId);
        }
    }
}