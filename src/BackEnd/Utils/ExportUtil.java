package BackEnd.Utils;

import BackEnd.Book.Book;
import BackEnd.User.User;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExportUtil {

    private static final String COMMA = ",";
    private static final String NEW_LINE = "\n";

    /**
     * Xuất danh sách Sách ra file CSV
     */
    public static boolean exportBooksToCSV(List<Book> books, File file) {
        // Sử dụng OutputStreamWriter với StandardCharsets.UTF_8 để hỗ trợ tiếng Việt
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            // QUAN TRỌNG: Ghi BOM (Byte Order Mark) để Excel nhận diện tiếng Việt
            writer.write('\ufeff');

            // 1. Ghi dòng tiêu đề
            writer.write("ID,Tên Sách,Tác Giả,Năm XB,Số Lượt Mượn,Trạng Thái");
            writer.write(NEW_LINE);

            // 2. Ghi dữ liệu
            for (Book b : books) {
                String line = String.join(COMMA,
                        escapeSpecialCharacters(b.getId()),
                        escapeSpecialCharacters(b.getName()),
                        escapeSpecialCharacters(b.getAuthor()),
                        escapeSpecialCharacters(b.getYear()),
                        String.valueOf(b.getSoLuotMuon()),
                        b.isStatus() ? "Có sẵn" : "Đang mượn"
                );
                writer.write(line);
                writer.write(NEW_LINE);
            }
            return true;

        } catch (Exception e) {
            System.err.println("Lỗi xuất file CSV: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xuất danh sách Người dùng ra file CSV
     */
    public static boolean exportUsersToCSV(List<User> users, File file) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            writer.write('\ufeff'); // Ghi BOM

            // 1. Ghi tiêu đề
            writer.write("ID,Tên Người Dùng,Tổng Số Sách Đã Mượn");
            writer.write(NEW_LINE);

            // 2. Ghi dữ liệu
            for (User u : users) {
                String line = String.join(COMMA,
                        escapeSpecialCharacters(u.getId()),
                        escapeSpecialCharacters(u.getName()),
                        String.valueOf(u.getSoSachDaMuon())
                );
                writer.write(line);
                writer.write(NEW_LINE);
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xử lý ký tự đặc biệt: Nếu dữ liệu có dấu phẩy (,) thì phải bao quanh bằng dấu ngoặc kép ("")
     * Ví dụ: Sách "Ăn, Cầu nguyện, Yêu" -> "\"Ăn, Cầu nguyện, Yêu\""
     */
    private static String escapeSpecialCharacters(String data) {
        if (data == null) return "";
        String escapedData = data.replaceAll("\\R", " "); // Xóa xuống dòng nếu có
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\""); // Escape dấu ngoặc kép
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
    public static boolean exportHistoryToCSV(List<BackEnd.Histories.UserInUserHistory> historyList, File file) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            writer.write('\ufeff'); // BOM cho tiếng Việt

            // 1. Tiêu đề
            writer.write("Thời Gian,User ID,Tên Người Dùng,ID Sách,Tên Sách,Hành Động");
            writer.write(NEW_LINE);

            // 2. Dữ liệu
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (BackEnd.Histories.UserInUserHistory h : historyList) {
                String timeStr = h.getLocalDateTime() != null ? h.getLocalDateTime().format(formatter) : "";

                String line = String.join(COMMA,
                        timeStr,
                        escapeSpecialCharacters(h.getId()),       // Lưu ý: Trong UserInUserHistory, getId() đôi khi là UserID hoặc BookID tùy ngữ cảnh, cần check kỹ logic DAO
                        escapeSpecialCharacters(h.getName()),     // Tên User
                        escapeSpecialCharacters(h.getId()),       // ID Sách (Cần xem lại mapping trong DAO nếu bị trùng)
                        escapeSpecialCharacters(h.getBookName()),
                        escapeSpecialCharacters(h.getTrangThai()) // Mượn/Trả
                );
                writer.write(line);
                writer.write(NEW_LINE);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xuất lịch sử của riêng một cuốn sách (Dữ liệu dạng List<String[]>)
     */
    public static boolean exportBookSpecificHistoryToCSV(List<String[]> historyList, File file) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            writer.write('\ufeff');

            // 1. Tiêu đề
            writer.write("User ID,Ngày Mượn,Ngày Trả");
            writer.write(NEW_LINE);

            // 2. Dữ liệu (String[]: [0]=UserId, [1]=BorrowDate, [2]=ReturnDate)
            for (String[] record : historyList) {
                String tra = (record[2] != null) ? record[2] : "ĐANG MƯỢN";

                String line = String.join(COMMA,
                        escapeSpecialCharacters(record[0]),
                        escapeSpecialCharacters(record[1]),
                        escapeSpecialCharacters(tra)
                );
                writer.write(line);
                writer.write(NEW_LINE);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
}
}