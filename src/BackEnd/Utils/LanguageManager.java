package BackEnd.Utils;

import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageManager {
    // Biến lưu trữ Bundle hiện tại
    private static ResourceBundle bundle;
    private static Locale currentLocale;

    // Khối tĩnh: Khởi tạo mặc định là Tiếng Việt
    static {
        setLanguage("vi");
    }

    /**
     * Thay đổi ngôn ngữ
     * @param langCode Mã ngôn ngữ: "vi" hoặc "en"
     */
    public static void setLanguage(String langCode) {
        if (langCode.equals("en")) {
            currentLocale = new Locale("en", "US");
        } else {
            currentLocale = new Locale("vi", "VN");
        }

        try {
            // Thêm tham số new UTF8Control() vào cuối
            bundle = ResourceBundle.getBundle("resources.messages", currentLocale, new UTF8Control());
        } catch (Exception e) {
            // Fallback dùng UTF8Control
            try {
                bundle = ResourceBundle.getBundle("messages", currentLocale, new UTF8Control());
            } catch (Exception ex) {
                System.err.println("Lỗi nạp ngôn ngữ: " + ex.getMessage());
            }
        }
    }

    /**
     * Lấy chuỗi văn bản từ key
     */
    public static String getText(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "Key not found: " + key;
        }
    }

    public static String getCurrentLang() {
        return currentLocale.getLanguage();
    }
}