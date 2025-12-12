package BackEnd.Utils;

import BackEnd.LibraryQ.Library;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.regex.Pattern;

public class EmailService {

    // Regex kiểm tra email
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Hàm gửi email chung (Generic)
     * Tối ưu: Chỉ tập trung vào việc gửi, tách biệt việc cấu hình.
     */
    public static boolean sendEmail(String toEmail, String subject, String body, String fromEmail, String password) {
        // 1. Cấu hình SMTP Server (Gmail)
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // 2. Tạo phiên làm việc (Session)
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            // 3. Tạo nội dung thư
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            // 4. Gửi
            Transport.send(message);
            // In thông báo thành công (Có thể dùng LanguageManager ở đây nếu muốn log ra UI)
            System.out.println(String.format("Email sent to: %s", toEmail));
            return true;

        } catch (MessagingException e) {
            System.err.println("Lỗi gửi email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gửi email thông báo quá hạn (Đã tối ưu: Gọi lại sendEmail)
     */
    public static boolean sendOverdueNotification(Library library, String recipientEmail, String userName, String bookName, long daysOverdue) {
        // 1. Lấy cấu hình
        String senderEmail = library.getSettingsDAO().getSetting("admin_email");
        String appPassword = library.getSettingsDAO().getSetting("app_password");

        if (senderEmail.isEmpty() || appPassword.isEmpty()) {
            System.err.println(LanguageManager.getText("mail.error.config"));
            return false;
        }

        // 2. Lấy nội dung từ Từ điển (Đa ngôn ngữ)
        String subject = LanguageManager.getText("mail.overdue.subject");

        // Format nội dung với các tham số: Tên user, Tên sách, Số ngày
        String bodyTemplate = LanguageManager.getText("mail.overdue.body");
        String body = String.format(bodyTemplate, userName, bookName, daysOverdue);

        // 3. Gọi hàm gửi chung
        return sendEmail(recipientEmail, subject, body, senderEmail, appPassword);
    }

    /**
     * Gửi email chào mừng (Đã tối ưu: Gọi lại sendEmail)
     */
    public static void sendWelcomeEmail(Library library, String recipientEmail, String userName, String userId) {
        // 1. Kiểm tra đầu vào
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return;
        }

        // 2. Lấy cấu hình
        String senderEmail = library.getSettingsDAO().getSetting("admin_email");
        String appPassword = library.getSettingsDAO().getSetting("app_password");

        if (senderEmail.isEmpty() || appPassword.isEmpty()) {
            System.err.println(LanguageManager.getText("mail.error.config"));
            return;
        }

        // 3. Lấy nội dung từ Từ điển
        String subject = LanguageManager.getText("mail.welcome.subject");

        // Format nội dung: Tên user, Tên user (lần 2), ID, Email
        String bodyTemplate = LanguageManager.getText("mail.welcome.body");
        String body = String.format(bodyTemplate, userName, userName, userId, recipientEmail);

        // 4. Gọi hàm gửi chung
        sendEmail(recipientEmail, subject, body, senderEmail, appPassword);
    }
}