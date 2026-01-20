package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.Booking;
import fit.se.springdatathymleafshopping.repositories.BookingRepository;
import fit.se.springdatathymleafshopping.services.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/v1/payment")
public class PaymentController {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private VNPayService vnPayService;

    // API tạo URL thanh toán (Được gọi bởi Ajax/Frontend)
    @PostMapping("/create-vnpay-url")
    @ResponseBody
    public ResponseEntity<?> createVnPayUrl(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            // 1. Lấy mã đơn hàng (bookingCode) từ Frontend
            String bookingCode = payload.get("bookingCode").toString();

            // 2. Tìm đơn hàng trong Database
            Booking booking = bookingRepository.findByBookingCode(bookingCode)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + bookingCode));

            // 3. Lấy số tiền THẬT từ Database (Server Side)
            // Lưu ý: booking.getTotalAmount() là Double (VD: 3500000.0) -> Ép về long
            long amount = booking.getTotalAmount().longValue();

            // 4. Tạo các tham số cần thiết
            String orderInfo = "Thanh toan don hang " + bookingCode;
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String returnUrl = baseUrl + "/api/v1/payment/vnpay-callback";

            // 5. Gọi Service để tạo URL
            String paymentUrl = vnPayService.createPaymentUrl(amount, orderInfo, returnUrl);

            System.out.println("✅ PaymentController: Đã tạo URL cho đơn " + bookingCode + " với giá " + amount);

            return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    // Xử lý khi VNPAY trả kết quả về (Callback)
    @GetMapping("/vnpay-callback")
    public String vnpayCallback(HttpServletRequest request, Model model) {
        String status = request.getParameter("vnp_ResponseCode");
        String bookingCode = request.getParameter("vnp_TxnRef");

        if ("00".equals(status)) {
            // Thanh toán thành công -> Cập nhật trạng thái đơn hàng
            bookingRepository.findByBookingCode(bookingCode).ifPresent(b -> {
                b.setStatus("PAID");
                b.setPaidAmount(b.getTotalAmount());
                bookingRepository.save(b);
            });
            model.addAttribute("success", true);
            model.addAttribute("message", "Thanh toán thành công!");
        } else {
            // Thanh toán thất bại
            model.addAttribute("success", false);
            model.addAttribute("message", "Thanh toán thất bại. Mã lỗi VNPAY: " + status);
        }

        model.addAttribute("bookingCode", bookingCode);
        return "payment-result"; // Trả về file HTML hiển thị kết quả
    }
}