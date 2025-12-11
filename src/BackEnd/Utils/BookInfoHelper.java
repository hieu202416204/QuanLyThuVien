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

    private static final String GOOGLE_API_URL = "https://www.googleapis.com/books/v1/volumes?q=isbn:";

    public static Book fetchBookDetails(String isbn) {
        try {
            // 1. LÀM SẠCH MÃ (Quan trọng): Xóa hết dấu gạch ngang, chỉ giữ số và chữ X
            // Ví dụ: "978-0-590..." -> "9780590..."
            String cleanIsbn = isbn.replaceAll("[^0-9Xx]", "");

            if (cleanIsbn.isEmpty()) return null;

            // 2. Gửi request
            URL url = new URL(GOOGLE_API_URL + cleanIsbn);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                System.err.println("Lỗi kết nối Google API: HTTP " + conn.getResponseCode());
                return null;
            }

            // 3. Đọc kết quả JSON
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            String json = response.toString();

            // Debug: In ra để xem Google trả về gì (nếu cần kiểm tra)
            // System.out.println("JSON Response: " + json);

            if (json.contains("\"totalItems\": 0")) return null; // Không tìm thấy

            // 4. Phân tích dữ liệu (Regex cải tiến)
            Book book = new Book();
            book.setId(isbn); // Giữ nguyên mã gốc người dùng nhập làm ID (hoặc dùng cleanIsbn tùy bạn)

            // Regex linh hoạt hơn: (?s) cho phép tìm xuyên dòng, .*? là non-greedy match
            book.setName(extractJsonValue(json, "\"title\"\\s*:\\s*\"([^\"]+)\""));
            book.setAuthor(extractJsonValue(json, "\"authors\"\\s*:\\s*\\[\\s*\"([^\"]+)\""));

            String date = extractJsonValue(json, "\"publishedDate\"\\s*:\\s*\"([^\"]+)\"");
            if (date != null && date.length() >= 4) {
                book.setYear(date.substring(0, 4));
            } else {
                book.setYear("Unknown");
            }

            String imgUrl = extractJsonValue(json, "\"thumbnail\"\\s*:\\s*\"([^\"]+)\"");
            if (imgUrl != null) {
                // Fix lỗi URL ảnh của Google (thường thiếu s ở http)
                book.setImagePath(imgUrl.replace("http://", "https://"));
            }

            book.setStatus(true);
            return book;

        } catch (Exception e) {
            System.err.println("Lỗi lấy thông tin sách: " + e.getMessage());
            return null;
        }
    }

    private static String extractJsonValue(String json, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static java.io.File downloadCoverImage(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) return null;
            URL url = new URL(imageUrl);
            java.io.InputStream in = url.openStream();
            java.io.File tempFile = java.io.File.createTempFile("downloaded_cover_", ".jpg");
            java.nio.file.Files.copy(in, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            in.close();
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }
}