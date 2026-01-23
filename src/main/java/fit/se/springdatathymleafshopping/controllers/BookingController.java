package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.dtos.BookingResponseDTO;
import fit.se.springdatathymleafshopping.entities.*;
import fit.se.springdatathymleafshopping.entities.enums.BookingStatus;
import fit.se.springdatathymleafshopping.entities.enums.PaymentMethod;
import fit.se.springdatathymleafshopping.repositories.*;
import fit.se.springdatathymleafshopping.services.MoMoService;
import fit.se.springdatathymleafshopping.services.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/booking")
public class BookingController {

    private final DepartureScheduleRepository scheduleRepo;
    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final VNPayService vnPayService;
    private final MoMoService moMoService;

    // --- CẤU HÌNH NGÂN HÀNG (Thay thông tin của bạn vào đây) ---
    private final String MY_BANK_ID = "MB";
    private final String MY_ACCOUNT_NO = "0366964556";
    private final String MY_ACCOUNT_NAME = "VU MINH NHAT";
    private final String MY_TEMPLATE = "compact2";

    public BookingController(DepartureScheduleRepository scheduleRepo,
                             BookingRepository bookingRepo,
                             UserRepository userRepo,
                             VNPayService vnPayService,
                             MoMoService moMoService) {
        this.scheduleRepo = scheduleRepo;
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
        this.vnPayService = vnPayService;
        this.moMoService = moMoService;
    }

    // --- GET: Hiện form đặt tour ---
    @GetMapping("/create")
    public String showBookingForm(@RequestParam("scheduleId") Integer scheduleId,
                                  Model model, Principal principal) {
        DepartureSchedule schedule = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Lịch trình không tồn tại"));
        model.addAttribute("schedule", schedule);

        if (principal != null) {
            User user = userRepo.findByEmail(principal.getName()).orElse(null);
            model.addAttribute("user", user);
        }
        return "booking-create";
    }

    // --- POST: Xử lý đặt tour (Dùng cho Form submit truyền thống) ---
    @PostMapping("/create")
    @Transactional
    public String createBooking(@RequestParam Integer scheduleId,
                                @RequestParam String customerName,
                                @RequestParam String customerEmail,
                                @RequestParam Integer adultCount,
                                @RequestParam(required = false) String paymentMethod,
                                HttpServletRequest request) {

        DepartureSchedule schedule = scheduleRepo.findById(scheduleId).orElseThrow();

        Booking booking = new Booking();
        booking.setBookingCode("BK-" + System.currentTimeMillis());
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setCustomerName(customerName);
        booking.setCustomerEmail(customerEmail);

        // Tính tiền đơn giản
        BigDecimal price = schedule.getPriceAdult() != null ? schedule.getPriceAdult() : BigDecimal.ZERO;
        booking.setTotalAmount(price.multiply(BigDecimal.valueOf(adultCount)));
        booking.setSchedule(schedule);

        // Xử lý phương thức thanh toán
        if ("VNPAY".equalsIgnoreCase(paymentMethod)) booking.setPaymentMethod(PaymentMethod.VNPAY);
        else if ("MOMO".equalsIgnoreCase(paymentMethod)) booking.setPaymentMethod(PaymentMethod.MOMO);
        else if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethod)) booking.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        else booking.setPaymentMethod(PaymentMethod.CASH);

        Booking savedBooking = bookingRepo.save(booking);

        // Điều hướng thanh toán
        if (savedBooking.getPaymentMethod() == PaymentMethod.VNPAY) {
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String vnpayUrl = vnPayService.createPaymentUrl(
                    savedBooking.getTotalAmount().longValue(),
                    "Thanh toan " + savedBooking.getBookingCode(),
                    baseUrl + "/payment/vnpay-callback",
                    savedBooking.getBookingCode());
            return "redirect:" + vnpayUrl;
        } else if (savedBooking.getPaymentMethod() == PaymentMethod.MOMO) {
            return "redirect:" + moMoService.createPaymentUrl(savedBooking);
        } else if (savedBooking.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            return "redirect:/booking/payment/bank/" + savedBooking.getId();
        }

        return "redirect:/booking/success?code=" + savedBooking.getBookingCode();
    }

    // --- GET: Trang hiển thị QR Ngân hàng ---
    @GetMapping("/payment/bank/{id}")
    public String showBankQr(@PathVariable Integer id, Model model) {
        Booking booking = bookingRepo.findById(id).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.PENDING) {
            return "redirect:/booking/history";
        }

        // Tạo link VietQR
        String qrUrl = String.format("https://img.vietqr.io/image/%s-%s-%s.png?amount=%d&addInfo=%s",
                MY_BANK_ID, MY_ACCOUNT_NO, MY_TEMPLATE,
                booking.getTotalAmount().longValue(),
                "THANHTOAN " + booking.getBookingCode());

        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("booking", booking);
        model.addAttribute("bankId", MY_BANK_ID);
        model.addAttribute("accNo", MY_ACCOUNT_NO);
        model.addAttribute("accName", MY_ACCOUNT_NAME);

        return "booking-bank";
    }

    // --- GET: Thanh toán lại từ lịch sử ---
    @GetMapping("/payment/{id}")
    public String retryPayment(@PathVariable Integer id, HttpServletRequest request) {
        Booking booking = bookingRepo.findById(id).orElse(null);

        if (booking != null && booking.getStatus() == BookingStatus.PENDING) {
            if (booking.getPaymentMethod() == PaymentMethod.VNPAY) {
                String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
                String url = vnPayService.createPaymentUrl(
                        booking.getTotalAmount().longValue(),
                        "Thanh toan lai " + booking.getBookingCode(),
                        baseUrl + "/payment/vnpay-callback",
                        booking.getBookingCode());
                return "redirect:" + url;
            } else if (booking.getPaymentMethod() == PaymentMethod.MOMO) {
                return "redirect:" + moMoService.createPaymentUrl(booking);
            } else if (booking.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
                return "redirect:/booking/payment/bank/" + booking.getId();
            }
        }
        return "redirect:/booking/history";
    }

    // --- GET: Lịch sử đặt vé ---
    @GetMapping("/history")
    public String showBookingHistory(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userRepo.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        List<Booking> bookings = bookingRepo.findByUserIdWithScheduleAndTour(user.getId());

        List<BookingResponseDTO> dtos = bookings.stream().map(b -> {
            BookingResponseDTO dto = new BookingResponseDTO();
            dto.setId(b.getId());
            dto.setBookingCode(b.getBookingCode());
            dto.setTotalAmount(b.getTotalAmount());
            dto.setStatus(b.getStatus() != null ? b.getStatus().name() : "UNKNOWN");
            if (b.getSchedule() != null && b.getSchedule().getTour() != null) {
                dto.setTourName(b.getSchedule().getTour().getName());
                dto.setStartDate(b.getSchedule().getStartDate());
                // dto.setTourDuration(...) nếu có
            }
            return dto;
        }).collect(Collectors.toList());

        model.addAttribute("bookings", dtos);
        return "booking-history";
    }

    // --- GET: Trang thành công ---
    @GetMapping("/success")
    public String showSuccess(@RequestParam String code, Model model) {
        model.addAttribute("code", code);
        return "booking-success";
    }
}