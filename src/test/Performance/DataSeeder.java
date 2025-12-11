package test.Performance;

import Database.BookDAO;
import Database.DatabaseManager;
import Database.UserDAO;

public class DataSeeder {

    // Số lượng bản ghi muốn bơm vào (Thử với 10.000, rồi tăng lên 50.000)
    private static final int NUM_RECORDS = 20000;

    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();
        System.out.println("🚀 Đang bơm " + NUM_RECORDS + " bản ghi vào hệ thống...");

        long start = System.currentTimeMillis();

        seedUsers();
        seedBooks();

        long end = System.currentTimeMillis();
        System.out.println("✅ Hoàn tất sau: " + (end - start) + "ms");
        System.out.println("👉 Bây giờ hãy mở App lên và thử tìm kiếm để cảm nhận độ lag!");
    }

    private static void seedUsers() {
        UserDAO dao = new UserDAO();
        // Dùng Transaction để insert cực nhanh (quan trọng)
        try (var conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu transaction

            var pstmt = conn.prepareStatement("INSERT INTO users (id, name, email, created_at, soSachDaMuon) VALUES (?, ?, ?, ?, 0)");

            for (int i = 0; i < NUM_RECORDS; i++) {
                pstmt.setString(1, "AUTO_U" + i);
                pstmt.setString(2, "Nguyen Van " + i); // Tên giống nhau để test search lag
                pstmt.setString(3, "user" + i + "@mail.com");
                pstmt.setString(4, java.time.LocalDate.now().toString());
                pstmt.addBatch();

                if (i % 1000 == 0) pstmt.executeBatch(); // Ghi mỗi 1000 dòng
            }
            pstmt.executeBatch();
            conn.commit(); // Chốt đơn

        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void seedBooks() {
        BookDAO dao = new BookDAO();
        try (var conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            var pstmt = conn.prepareStatement("INSERT INTO books (id, name, author, year, status, soLuotMuon) VALUES (?, ?, ?, ?, 1, 0)");

            for (int i = 0; i < NUM_RECORDS; i++) {
                pstmt.setString(1, "AUTO_B" + i);
                pstmt.setString(2, "Java Programming Advanced Vol " + i); // Tên dài để test hiển thị
                pstmt.setString(3, "Author " + (i % 100)); // Ít tác giả để test gom nhóm
                pstmt.setString(4, "202" + (i % 5));
                pstmt.addBatch();

                if (i % 1000 == 0) pstmt.executeBatch();
            }
            pstmt.executeBatch();
            conn.commit();

        } catch (Exception e) { e.printStackTrace(); }
    }
}