package BackEnd.Utils;

import BackEnd.Book.Book;
import BackEnd.User.User;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ImportUtil {

    /**
     * Đọc danh sách Sách từ file CSV.
     * Định dạng mong đợi: ID, Tên Sách, Tác Giả, Năm XB
     */
    public static List<Book> importBooksFromCSV(File file) {
        List<Book> books = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                // Bỏ qua dòng tiêu đề (Header)
                if (isFirstLine) {
                    // Xử lý BOM nếu có (Excel hay thêm ký tự này vào đầu file)
                    line = line.replace("\ufeff", "");
                    if (line.toLowerCase().startsWith("id")) { // Kiểm tra xem có phải header không
                        isFirstLine = false;
                        continue;
                    }
                    isFirstLine = false;
                }

                if (line.trim().isEmpty()) continue;

                // Tách chuỗi CSV (Xử lý dấu phẩy trong ngoặc kép)
                String[] parts = parseCSVLine(line);

                // Kiểm tra đủ dữ liệu tối thiểu (ID, Tên, Tác giả, Năm)
                if (parts.length >= 4) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String author = parts[2].trim();
                    String year = parts[3].trim();

                    // Xóa dấu ngoặc kép bao quanh nếu có (do CSV thêm vào)
                    name = removeQuotes(name);
                    author = removeQuotes(author);

                    if (!id.isEmpty() && !name.isEmpty()) {
                        books.add(new Book(id, name, author, year));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return books;
    }

    /**
     * Đọc danh sách Người dùng từ file CSV.
     * Định dạng mong đợi: ID, Tên, Email (Tùy chọn)
     */
    public static List<User> importUsersFromCSV(File file) {
        List<User> users = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    line = line.replace("\ufeff", "");
                    if (line.toLowerCase().startsWith("id")) {
                        isFirstLine = false;
                        continue;
                    }
                    isFirstLine = false;
                }

                if (line.trim().isEmpty()) continue;

                String[] parts = parseCSVLine(line);

                // Cần ít nhất ID và Tên
                if (parts.length >= 2) {
                    String id = parts[0].trim();
                    String name = removeQuotes(parts[1].trim());
                    String email = "";

                    // Nếu file có cột thứ 3 là Email
                    if (parts.length >= 3) {
                        email = removeQuotes(parts[2].trim());
                    }

                    if (!id.isEmpty() && !name.isEmpty()) {
                        users.add(new User(id, name, email));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Hàm tách dòng CSV chuẩn: Xử lý trường hợp dữ liệu chứa dấu phẩy ","
     * Ví dụ: "Harry Potter, và Hòn đá" -> được tính là 1 trường
     */
    private static String[] parseCSVLine(String line) {
        // Regex này tách dấu phẩy NHƯNG bỏ qua dấu phẩy nằm trong ngoặc kép
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    private static String removeQuotes(String text) {
        if (text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1).replace("\"\"", "\"");
        }
        return text;
    }
}