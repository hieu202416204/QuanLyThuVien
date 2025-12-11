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
    /**
     * Gửi email chào mừng khi tạo tài khoản thành công.
     */
    public static void sendWelcomeEmail(Library library, String recipientEmail, String userName, String userId) {
        // 1. Kiểm tra đầu vào
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            System.out.println("-> Không gửi email chào mừng: Không có địa chỉ email.");
            return; // Không làm gì nếu không có email
        }

        // 2. Lấy cấu hình (Giống hàm sendOverdueNotification)
        String senderEmail = library.getSettingsDAO().getSetting("admin_email");
        String appPassword = library.getSettingsDAO().getSetting("app_password");

        if (senderEmail.isEmpty() || appPassword.isEmpty()) {
            System.err.println("-> Chưa cấu hình Email Admin, bỏ qua gửi mail chào mừng.");
            return;
        }

        // 3. Cấu hình Server
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, appPassword);
            }
        });

        // 4. Soạn và Gửi
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));

            // Tiêu đề Email
            message.setSubject("🎉 Chào mừng bạn đến với Thư viện!");

            // Nội dung Email
            String content = "Xin chào " + userName + ",\n\n"
                    + "Chúc mừng! Tài khoản thư viện của bạn đã được tạo thành công.\n"
                    + "Dưới đây là thông tin thành viên của bạn:\n\n"
                    + "--------------------------------\n"
                    + "📛 Tên thành viên: " + userName + "\n"
                    + "🆔 Mã thành viên (ID): " + userId + "\n"
                    + "📧 Email đăng ký: " + recipientEmail + "\n"
                    + "--------------------------------\n\n"
                    + "Bạn có thể sử dụng Mã thành viên này để mượn sách tại thư viện.\n"
                    + "Xin cảm ơn và chúc bạn có những trải nghiệm đọc sách tuyệt vời!\n\n"
                    + "Trân trọng,\n"
                    + "Ban quản lý Thư viện.";

            message.setText(content);

            Transport.send(message);
            System.out.println("✅ Đã gửi email chào mừng tới: " + recipientEmail);

        } catch (MessagingException e) {
            System.err.println("❌ Lỗi gửi mail chào mừng: " + e.getMessage());
        }
    }
}