package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.Booking;
import fit.se.springdatathymleafshopping.entities.PaymentTransaction;
import fit.se.springdatathymleafshopping.entities.enums.BookingStatus;
import fit.se.springdatathymleafshopping.entities.enums.PaymentStatus;
import fit.se.springdatathymleafshopping.repositories.BookingRepository;
import fit.se.springdatathymleafshopping.repositories.PaymentTransactionRepository;
import fit.se.springdatathymleafshopping.services.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/api/v1/payment")
public class PaymentController {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentTransactionRepository paymentTransactionRepository;
    @Autowired private VNPayService vnPayService; // Đã inject ở đây

    // API tạo URL thanh toán (Được gọi bởi Ajax/Frontend)
    @PostMapping("/create-vnpay-url")
    @ResponseBody
    public ResponseEntity<?> createVnPayUrl(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            String bookingCode = payload.get("bookingCode") == null ? null : payload.get("bookingCode").toString();
            if (bookingCode == null || bookingCode.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "bookingCode is required"));
            }

            Booking booking = bookingRepository.findByBookingCode(bookingCode)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + bookingCode));

            // Lấy số tiền từ DB (BigDecimal) và chuyển sang long (đơn vị VND)
            BigDecimal total = booking.getTotalAmount() == null ? BigDecimal.ZERO : booking.getTotalAmount();
            long amount = total.longValue();

            String orderInfo = "Thanh toan don hang " + bookingCode;
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String returnUrl = baseUrl + "/api/v1/payment/vnpay-callback";

            // --- SỬA LỖI TẠI ĐÂY ---
            // 1. Dùng 'vnPayService' (chữ thường) thay vì 'VNPayService'
            // 2. Dùng 'booking' thay vì 'savedBooking'
            String vnpayUrl = vnPayService.createPaymentUrl(amount, orderInfo, returnUrl, booking.getBookingCode());

            // 3. Trả về đúng tên biến 'vnpayUrl'
            return ResponseEntity.ok(Map.of("paymentUrl", vnpayUrl));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    // Xử lý khi VNPAY trả kết quả về (Callback)
    @GetMapping("/vnpay-callback")
    @Transactional
    public String vnpayCallback(HttpServletRequest request, Model model) {
        // Các tham số VNPAY trả về
        String vnpResponseCode = request.getParameter("vnp_ResponseCode"); // "00" = success
        String vnpTxnRef = request.getParameter("vnp_TxnRef"); // bookingCode
        String vnpAmount = request.getParameter("vnp_Amount");
        String vnpTransactionNo = request.getParameter("vnp_TransactionNo");
        String vnpBankCode = request.getParameter("vnp_BankCode");

        model.addAttribute("bookingCode", vnpTxnRef);

        if (vnpTxnRef == null || vnpTxnRef.isBlank()) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Thiếu tham số vnp_TxnRef từ VNPAY");
            return "payment-result";
        }

        try {
            // Tìm booking theo bookingCode
            Optional<Booking> optBooking = bookingRepository.findByBookingCode(vnpTxnRef);
            if (optBooking.isEmpty()) {
                model.addAttribute("success", false);
                model.addAttribute("message", "Không tìm thấy đơn hàng: " + vnpTxnRef);
                return "payment-result";
            }

            Booking booking = optBooking.get();

            // Kiểm tra trùng lặp giao dịch
            String txnRefToCheck = vnpTransactionNo != null && !vnpTransactionNo.isBlank() ? vnpTransactionNo : vnpTxnRef;
            boolean alreadyProcessed = paymentTransactionRepository.existsByTxnRef(txnRefToCheck);

            if ("00".equals(vnpResponseCode)) {
                if (alreadyProcessed) {
                    model.addAttribute("success", true);
                    model.addAttribute("message", "Giao dịch đã được xử lý trước đó");
                    return "payment-result";
                }

                // Lưu Transaction
                PaymentTransaction tx = new PaymentTransaction();
                tx.setTxnRef(txnRefToCheck);
                // Lưu ý: VNPAY trả về amount * 100, cần chia 100 nếu muốn lưu số thực
                tx.setAmount(parseAmountSafe(vnpAmount).divide(new BigDecimal(100)));
                tx.setMethod(vnpBankCode != null ? vnpBankCode : "VNPAY");
                tx.setPaymentTime(LocalDateTime.now());
                tx.setStatus(PaymentStatus.SUCCESS);
                tx.setBooking(booking);
                paymentTransactionRepository.save(tx);

                // Cập nhật Booking
                BigDecimal paid = booking.getPaidAmount() == null ? BigDecimal.ZERO : booking.getPaidAmount();
                BigDecimal add = tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount();
                booking.setPaidAmount(paid.add(add));
                booking.setStatus(BookingStatus.PAID);
                bookingRepository.save(booking);

                model.addAttribute("success", true);
                model.addAttribute("message", "Thanh toán thành công!");
            } else {
                // Thanh toán thất bại
                if (!alreadyProcessed) {
                    PaymentTransaction tx = new PaymentTransaction();
                    tx.setTxnRef(txnRefToCheck);
                    tx.setAmount(parseAmountSafe(vnpAmount).divide(new BigDecimal(100)));
                    tx.setMethod(vnpBankCode != null ? vnpBankCode : "VNPAY");
                    tx.setPaymentTime(LocalDateTime.now());
                    tx.setStatus(PaymentStatus.FAILED);
                    tx.setBooking(booking);
                    paymentTransactionRepository.save(tx);
                }

                model.addAttribute("success", false);
                model.addAttribute("message", "Thanh toán thất bại. Mã lỗi VNPAY: " + vnpResponseCode);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            model.addAttribute("success", false);
            model.addAttribute("message", "Lỗi xử lý callback: " + ex.getMessage());
        }

        return "payment-result";
    }

    private BigDecimal parseAmountSafe(String vnpAmount) {
        try {
            if (vnpAmount == null || vnpAmount.isBlank()) return BigDecimal.ZERO;
            return new BigDecimal(vnpAmount);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}