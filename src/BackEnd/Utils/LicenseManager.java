package BackEnd.Utils;

import BackEnd.LibraryQ.Library;
import Database.SettingsDAO;

public class LicenseManager {

    public static final String KEY_PLUS = "jahsdjssfubvu17234b!&!@%!^";
    public static final String KEY_PRO = "jahsdjssfubvu17234b!&!@%!^vnewuivewi18924!@**!*@!&@!@!";

    public enum Level {
        FREE, PLUS, PRO
    }

    private static Level currentLevel = Level.FREE;

    // Định nghĩa giới hạn cho từng gói
    public static final int LIMIT_BOOKS_FREE = 50;
    public static final int LIMIT_BOOKS_PLUS = 1000;
    // PRO là không giới hạn

    /**
     * Khởi tạo: Đọc Key, Xác thực Key VÀ Kiểm tra Hardware ID
     */
    public static void init(Library library) {
        SettingsDAO settingsDAO = library.getSettingsDAO();
        String licenseKey = settingsDAO.getSetting("license_key");

        // 1. Xác thực Key để lấy Level
        Level validatedLevel = validateKey(licenseKey);

        // Key FREE thì không cần kiểm tra khóa máy
        if (validatedLevel == Level.FREE) {
            currentLevel = Level.FREE;
            System.out.println("Current License Level: " + currentLevel);
            return;
        }

        // --- BẮT ĐẦU KIỂM TRA KHÓA MÁY (Hardware Binding) ---

        String storedMac = settingsDAO.getSetting("hardware_id");
        String currentMac = HardwareID.getMacAddress();

        // Trường hợp 1: Máy tính chưa từng kích hoạt Key PRO/PLUS (Lần đầu)
        if (storedMac == null || storedMac.isEmpty() || storedMac.equals("UNKNOWN")) {
            // Lưu MAC Address của máy hiện tại vào DB
            settingsDAO.saveSetting("hardware_id", currentMac);
            currentLevel = validatedLevel; // Kích hoạt thành công
            System.out.println("✅ Kích hoạt thành công. MAC Address đã được lưu: " + currentMac);

        }
        // Trường hợp 2: Đã lưu MAC, nhưng MAC hiện tại KHÔNG KHỚP với MAC đã lưu
        else if (!storedMac.equals(currentMac) && !currentMac.equals("UNKNOWN")) {
            currentLevel = Level.FREE; // Hủy kích hoạt, trả về Free

            // In ra cảnh báo rõ ràng
            System.err.println("⚠️ CẢNH BÁO KHÓA MÁY!");
            System.err.println("MAC đã lưu: " + storedMac);
            System.err.println("MAC hiện tại: " + currentMac);
            System.err.println("Key bản quyền đã bị vô hiệu hóa vì không khớp máy tính.");

            // Xóa key đã lưu khỏi DB (để người dùng phải liên hệ bạn để kích hoạt lại)
            settingsDAO.saveSetting("license_key", null);
        }
        // Trường hợp 3: Mọi thứ đều khớp (Đang dùng máy đã kích hoạt)
        else {
            currentLevel = validatedLevel;
        }

        System.out.println("Current License Level: " + currentLevel);
    }

    /**
     * Logic kiểm tra Key (Trong thực tế sẽ check online hoặc giải mã hash)
     * Ở đây ta dùng logic đơn giản để test.
     */
    public static Level validateKey(String key) {
        if (key == null) return Level.FREE;

        if (key.equals(KEY_PLUS)) return Level.PLUS; // Ví dụ key: KEY-PRO-2025
        if (key.equals(KEY_PRO)) return Level.PRO; // Ví dụ key: KEY-PLUS-2025

        return Level.FREE;
    }

    public static Level getCurrentLevel() {
        return currentLevel;
    }

    // --- CÁC HÀM KIỂM TRA QUYỀN HẠN ---

    // 1. Kiểm tra tính năng Email (Chỉ Plus và Pro)
    public static boolean canSendEmail() {
        return currentLevel == Level.PLUS || currentLevel == Level.PRO;
    }

    // 2. Kiểm tra tính năng Sao lưu/Thống kê nâng cao (Chỉ Pro)
    public static boolean isProFeature() {
        return currentLevel == Level.PRO;
    }

    // 3. Kiểm tra giới hạn số lượng sách
    public static boolean canAddMoreBooks(int currentBookCount) {
        if (currentLevel == Level.PRO) return true; // Pro không giới hạn
        if (currentLevel == Level.PLUS) return currentBookCount < LIMIT_BOOKS_PLUS;
        return currentBookCount < LIMIT_BOOKS_FREE; // Free bị giới hạn
    }

    // Hàm lấy tên gói để hiển thị
    public static String getLevelName() {
        return currentLevel.toString();
    }
}