package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.Booking;
import fit.se.springdatathymleafshopping.entities.PaymentTransaction;
import fit.se.springdatathymleafshopping.entities.enums.BookingStatus;
import fit.se.springdatathymleafshopping.entities.enums.PaymentStatus;
import fit.se.springdatathymleafshopping.repositories.BookingRepository;
import fit.se.springdatathymleafshopping.repositories.PaymentTransactionRepository;
import fit.se.springdatathymleafshopping.services.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    @Autowired
    private EmailService emailService;

    // --- 1. VNPAY Callback ---
    @GetMapping("/vnpay-callback")
    @Transactional
    public String vnpayCallback(HttpServletRequest request, Model model) {
        String vnpResponseCode = request.getParameter("vnp_ResponseCode");
        String vnpTxnRef = request.getParameter("vnp_TxnRef");
        String vnpAmount = request.getParameter("vnp_Amount");
        String vnpTransactionNo = request.getParameter("vnp_TransactionNo");
        String vnpBankCode = request.getParameter("vnp_BankCode");

        try {
            Optional<Booking> optBooking = bookingRepository.findByBookingCode(vnpTxnRef);
            if (optBooking.isEmpty()) return "redirect:/booking/history?error=BookingNotFound";
            Booking booking = optBooking.get();

            if (paymentTransactionRepository.existsByTxnRef(vnpTransactionNo)) {
                return "redirect:/booking/success?code=" + booking.getBookingCode();
            }

            if ("00".equals(vnpResponseCode)) {
                handleSuccessPayment(booking, vnpAmount, vnpTransactionNo, "VNPAY - " + vnpBankCode);
                return "redirect:/booking/success?code=" + booking.getBookingCode();
            } else {
                handleFailedPayment(booking, vnpAmount, vnpTransactionNo, "VNPAY", vnpResponseCode);
                return "redirect:/booking/history?error=PaymentFailed_VNPAY_" + vnpResponseCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/booking/history?error=SystemError";
        }
    }

    // --- 2. MOMO Callback ---
    @GetMapping("/momo-return")
    @Transactional
    public String momoReturn(@RequestParam(value = "resultCode", required = false) Integer resultCode,
                             @RequestParam(value = "orderId", required = false) String orderId,
                             @RequestParam(value = "amount", required = false) String amount,
                             @RequestParam(value = "transId", required = false) String transId) {
        try {
            if (orderId == null) return "redirect:/";
            String bookingCode = orderId.split("_")[0];

            Optional<Booking> optBooking = bookingRepository.findByBookingCode(bookingCode);
            if (optBooking.isEmpty()) return "redirect:/booking/history?error=BookingNotFound";
            Booking booking = optBooking.get();

            if (transId != null && paymentTransactionRepository.existsByTxnRef(transId)) {
                return "redirect:/booking/success?code=" + booking.getBookingCode();
            }

            if (resultCode != null && resultCode == 0) {
                handleSuccessPayment(booking, amount, transId, "MOMO");
                return "redirect:/booking/success?code=" + booking.getBookingCode();
            } else {
                handleFailedPayment(booking, amount, transId, "MOMO", String.valueOf(resultCode));
                return "redirect:/booking/history?error=PaymentFailed_MOMO_" + resultCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/booking/history?error=SystemError";
        }
    }

    // Helper methods
    private void handleSuccessPayment(Booking booking, String amountStr, String txnRef, String method) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setBooking(booking);
        tx.setTxnRef(txnRef);
        tx.setMethod(method);
        tx.setPaymentTime(LocalDateTime.now());
        tx.setStatus(PaymentStatus.SUCCESS);

        BigDecimal amount = new BigDecimal(amountStr);
        if (method.startsWith("VNPAY")) amount = amount.divide(new BigDecimal(100));
        tx.setAmount(amount);
        paymentTransactionRepository.save(tx);

        booking.setPaidAmount(booking.getPaidAmount().add(amount));
        if (booking.getPaidAmount().compareTo(booking.getTotalAmount()) >= 0) {
            booking.setStatus(BookingStatus.PAID);
        }
        bookingRepository.save(booking);

        try { emailService.sendBookingConfirmation(booking); } catch (Exception e) { System.err.println("Email error: " + e.getMessage()); }
    }

    private void handleFailedPayment(Booking booking, String amountStr, String txnRef, String method, String errorCode) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setBooking(booking);
        tx.setTxnRef(txnRef);
        tx.setMethod(method);
        tx.setPaymentTime(LocalDateTime.now());
        tx.setStatus(PaymentStatus.FAILED);
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            if (method.startsWith("VNPAY")) amount = amount.divide(new BigDecimal(100));
            tx.setAmount(amount);
        } catch (Exception e) { tx.setAmount(BigDecimal.ZERO); }
        paymentTransactionRepository.save(tx);
    }
}