package BackEnd.Utils;

import BackEnd.LibraryQ.Library;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    // Không còn SENDER_EMAIL và APP_PASSWORD tĩnh nữa

    /**
     * Gửi email thông báo (Cần truyền Library để truy cập DB)
     */
    public static boolean sendOverdueNotification(Library library, String recipientEmail, String userName, String bookName, long daysOverdue) {

        // 1. Lấy cấu hình từ DB
        String senderEmail = library.getSettingsDAO().getSetting("admin_email");
        String appPassword = library.getSettingsDAO().getSetting("app_password");

        // Kiểm tra xem đã cấu hình chưa
        if (senderEmail.isEmpty() || appPassword.isEmpty()) {
            System.err.println("Chưa cấu hình Email trong phần Cài đặt!");
            return false;
        }

        // 2. Cấu hình Server (Giữ nguyên)
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, appPassword); // Dùng biến động
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("⚠️ THÔNG BÁO QUÁ HẠN: Thư viện"); // Có thể thêm tên thư viện từ DB sau này

            String content = "Xin chào " + userName + ",\n\n"
                    + "Hệ thống ghi nhận bạn đang mượn cuốn sách: " + bookName + "\n"
                    + "Tình trạng: QUÁ HẠN " + daysOverdue + " ngày.\n\n"
                    + "Vui lòng mang sách đến trả tại thư viện sớm nhất.\n\n"
                    + "Trân trọng.";

            message.setText(content);
            Transport.send(message);
            System.out.println("-> Đã gửi mail cho: " + recipientEmail);
            return true;

        } catch (MessagingException e) {
            System.err.println("Lỗi gửi mail: " + e.getMessage());
            return false;
        }
    }
}