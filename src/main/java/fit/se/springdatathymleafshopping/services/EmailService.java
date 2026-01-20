package fit.se.springdatathymleafshopping.services;

import fit.se.springdatathymleafshopping.entities.Booking;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendBookingConfirmation(String toEmail, Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setTo(toEmail);
            helper.setSubject("Xác nhận đặt tour thành công - Mã đơn: " + booking.getId());

            // Nội dung Email (HTML)
            String htmlContent = "<h3>Cảm ơn quý khách đã đặt tour tại Smart Travel!</h3>"
                    + "<p>Xin chào <b>" + booking.getUser().getFullName() + "</b>,</p>"
                    + "<p>Đơn hàng của bạn đã được thanh toán thành công.</p>"
                    + "<hr>"
                    + "<ul>"
                    + "<li><b>Mã đơn:</b> #" + booking.getId() + "</li>"
                    + "<li><b>Tour:</b> " + booking.getSchedule().getTour().getName() + "</li>"
                    + "<li><b>Ngày đi:</b> " + booking.getSchedule().getStartDate() + "</li>"
                    + "<li><b>Tổng tiền:</b> " + String.format("%,.0f", booking.getTotalAmount()) + " VNĐ</li>"
                    + "</ul>"
                    + "<p>Vui lòng đến đúng giờ tại điểm hẹn.</p>"
                    + "<p>Trân trọng,<br>Đội ngũ Smart Travel</p>";

            helper.setText(htmlContent, true); // true để bật chế độ HTML

            mailSender.send(message);
            System.out.println("Đã gửi email thành công cho: " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Lỗi gửi mail: " + e.getMessage());
        }
    }
}