package BackEnd.Utils;

import javafx.scene.image.Image;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {

    // Map lưu trữ: Key là đường dẫn file, Value là Ảnh đã resize (JavaFX Image)
    // Dùng ConcurrentHashMap để an toàn trong môi trường đa luồng
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();

    /**
     * Lấy ảnh từ Cache
     */
    public static Image get(String path) {
        return cache.get(path);
    }

    /**
     * Lưu ảnh vào Cache
     */
    public static void put(String path, Image img) {
        if (path != null && img != null) {
            cache.put(path, img);
        }
    }

    /**
     * Kiểm tra xem ảnh đã có trong Cache chưa
     */
    public static boolean contains(String path) {
        return cache.containsKey(path);
    }

    /**
     * Xóa sạch Cache (Dùng khi Reset dữ liệu hoặc khi tắt App để giải phóng RAM)
     */
    public static void clear() {
        cache.clear();
        System.out.println("Image Cache cleared.");
    }

    /**
     * Xóa một ảnh cụ thể (Dùng khi cập nhật ảnh bìa sách)
     */
    public static void remove(String path) {
        cache.remove(path);
    }
}