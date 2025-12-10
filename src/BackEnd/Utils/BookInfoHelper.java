package BackEnd.Utils;

import BackEnd.Book.Book;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookInfoHelper {

    // Google Books API (Miễn phí, không cần key cho nhu cầu cơ bản)
    private static final String GOOGLE_API_URL = "https://www.googleapis.com/books/v1/volumes?q=isbn:";

    /**
     * Tìm thông tin sách từ ISBN
     * @param isbn Mã vạch sách
     * @return Đối tượng Book chứa thông tin (hoặc null nếu không thấy)
     */
    public static Book fetchBookDetails(String isbn) {
        try {
            // 1. Gửi request
            URL url = new URL(GOOGLE_API_URL + isbn.trim());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) return null;

            // 2. Đọc kết quả JSON
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            String json = response.toString();
            if (json.contains("\"totalItems\": 0")) return null; // Không tìm thấy

            // 3. Phân tích dữ liệu (Dùng Regex để không phụ thuộc thư viện ngoài)
            Book book = new Book();
            book.setId(isbn); // ID chính là mã vạch

            // Lấy Tên sách ("title": "...")
            book.setName(extractJsonValue(json, "\"title\":\\s*\"([^\"]+)\""));

            // Lấy Tác giả ("authors": [ "..."]) - Lấy tác giả đầu tiên
            book.setAuthor(extractJsonValue(json, "\"authors\":\\s*\\[\\s*\"([^\"]+)\""));

            // Lấy Năm xuất bản ("publishedDate": "YYYY-MM-DD" hoặc "YYYY")
            String date = extractJsonValue(json, "\"publishedDate\":\\s*\"([^\"]+)\"");
            if (date != null && date.length() >= 4) {
                book.setYear(date.substring(0, 4)); // Chỉ lấy 4 số đầu (Năm)
            } else {
                book.setYear("Unknown");
            }

            // Lấy Link ảnh bìa ("thumbnail": "...")
            // Lưu ý: Đây là URL online, sau này cần tải về máy
            String imgUrl = extractJsonValue(json, "\"thumbnail\":\\s*\"([^\"]+)\"");
            if (imgUrl != null) {
                // Google trả về http, nên đổi thành https
                book.setImagePath(imgUrl.replace("http://", "https://"));
            }

            // Mặc định sách mới là có sẵn
            book.setStatus(true);

            return book;

        } catch (Exception e) {
            System.err.println("Lỗi lấy thông tin sách: " + e.getMessage());
            return null;
        }
    }

    // Hàm hỗ trợ lấy giá trị trong chuỗi JSON bằng Regex
    private static String extractJsonValue(String json, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    // Hàm tải ảnh từ URL về máy tính (Để lưu vào thư mục images)
    public static java.io.File downloadCoverImage(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) return null;
            URL url = new URL(imageUrl);
            java.io.InputStream in = url.openStream();

            // Tạo file tạm
            java.io.File tempFile = java.io.File.createTempFile("downloaded_cover_", ".jpg");
            java.nio.file.Files.copy(in, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            in.close();

            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }
}