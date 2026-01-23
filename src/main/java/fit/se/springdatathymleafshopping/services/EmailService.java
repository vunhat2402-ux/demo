package fit.se.springdatathymleafshopping.services;

import fit.se.springdatathymleafshopping.entities.Booking;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // 👇 Lấy email từ application.properties để làm người gửi (Fix lỗi 555)
    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendBookingConfirmation(Booking booking) {
        try {
            // 1. Lấy thông tin người nhận
            String toEmail = booking.getCustomerEmail();
            String customerName = booking.getCustomerName();

            // Xử lý mã đơn (ưu tiên BookingCode, nếu null thì lấy ID)
            String bookingCode = booking.getBookingCode() != null ? booking.getBookingCode() : String.valueOf(booking.getId());

            // Xử lý thông tin Tour an toàn (tránh lỗi nếu dữ liệu Tour bị thiếu)
            String tourName = (booking.getSchedule() != null && booking.getSchedule().getTour() != null)
                    ? booking.getSchedule().getTour().getName()
                    : "Không xác định";

            String startDate = (booking.getSchedule() != null)
                    ? booking.getSchedule().getStartDate().toString()
                    : "N/A";

            // 2. Tạo Email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            // 👇 QUAN TRỌNG: Phải set người gửi để Gmail không chặn
            helper.setFrom(fromEmail);

            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đặt tour thành công - Mã đơn: " + bookingCode);

            // 3. Nội dung HTML chuyên nghiệp
            String htmlContent = "<h3>Cảm ơn quý khách đã đặt tour tại Smart Travel!</h3>"
                    + "<p>Xin chào <b>" + customerName + "</b>,</p>"
                    + "<p>Đơn hàng của bạn đã được thanh toán thành công.</p>"
                    + "<hr>"
                    + "<ul>"
                    + "<li><b>Mã đơn:</b> #" + bookingCode + "</li>"
                    + "<li><b>Tour:</b> " + tourName + "</li>"
                    + "<li><b>Ngày đi:</b> " + startDate + "</li>"
                    + "<li><b>Tổng tiền:</b> " + String.format("%,.0f", booking.getTotalAmount()) + " VNĐ</li>"
                    + "</ul>"
                    + "<p>Vui lòng mang theo email này khi đến điểm hẹn.</p>"
                    + "<p>Trân trọng,<br>Đội ngũ Smart Travel</p>";

            helper.setText(htmlContent, true); // true = bật chế độ HTML

            // 4. Gửi mail
            mailSender.send(message);
            System.out.println("✅ Đã gửi email thành công cho: " + toEmail);

        } catch (MessagingException e) {
            System.err.println("❌ Lỗi gửi mail (Messaging): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Lỗi khác khi gửi mail: " + e.getMessage());
            e.printStackTrace();
        }
    }
}