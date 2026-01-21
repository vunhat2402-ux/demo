package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.dtos.BookingRequestDTO;
import fit.se.springdatathymleafshopping.dtos.BookingResponseDTO;
import fit.se.springdatathymleafshopping.entities.*;
import fit.se.springdatathymleafshopping.entities.enums.BookingStatus;
import fit.se.springdatathymleafshopping.entities.enums.PaymentMethod;
import fit.se.springdatathymleafshopping.repositories.*;
import fit.se.springdatathymleafshopping.services.BookingService;
import fit.se.springdatathymleafshopping.services.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired private DepartureScheduleRepository scheduleRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BookingService bookingService;

    // Đổi tên biến thành chữ thường để tránh nhầm với class tĩnh
    @Autowired private VNPayService vnPayService;

    // 1. HIỆN TRANG ĐẶT TOUR
    @GetMapping("/create")
    public String showBookingForm(@RequestParam("scheduleId") Integer scheduleId,
                                  Model model, Principal principal) {

        DepartureSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Lịch trình không tồn tại"));

        model.addAttribute("schedule", schedule);

        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            model.addAttribute("user", user);
        } else {
            model.addAttribute("user", null);
        }

        return "booking-create";
    }

    // 2. XỬ LÝ ĐẶT TOUR (Submit form thường)
    @PostMapping("/create")
    @Transactional
    public String createBooking(@RequestParam Integer scheduleId,
                                @RequestParam String customerName,
                                @RequestParam String customerPhone,
                                @RequestParam String customerEmail,
                                @RequestParam Integer adultCount,
                                @RequestParam Integer childCount,
                                @RequestParam(required = false) String totalAmount,
                                Principal principal) {

        DepartureSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Lịch trình không tồn tại"));

        Booking booking = new Booking();
        booking.setBookingCode("BK-" + System.currentTimeMillis());
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setCustomerName(customerName);
        booking.setCustomerPhone(customerPhone);
        booking.setCustomerEmail(customerEmail);

        BigDecimal total;
        try {
            if (totalAmount != null && !totalAmount.isBlank()) {
                total = new BigDecimal(totalAmount);
            } else {
                BigDecimal priceAdult = schedule.getPriceAdult() == null ? BigDecimal.ZERO : schedule.getPriceAdult();
                BigDecimal priceChild = schedule.getPriceChild() == null ? BigDecimal.ZERO : schedule.getPriceChild();
                total = priceAdult.multiply(BigDecimal.valueOf(adultCount))
                        .add(priceChild.multiply(BigDecimal.valueOf(childCount)));
            }
        } catch (Exception ex) {
            total = BigDecimal.ZERO;
        }

        booking.setTotalAmount(total);
        booking.setSchedule(schedule);

        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            booking.setUser(user);
        }

        bookingRepository.save(booking);

        // Trừ kho
        int prevBooked = schedule.getBooked() == null ? 0 : schedule.getBooked();
        int add = (adultCount == null ? 0 : adultCount) + (childCount == null ? 0 : childCount);
        schedule.setBooked(prevBooked + add);
        scheduleRepository.save(schedule);

        return "redirect:/booking/success?code=" + booking.getBookingCode();
    }

    // 3. TRANG THÀNH CÔNG
    @GetMapping("/success")
    public String showSuccess(@RequestParam String code, Model model) {
        model.addAttribute("code", code);
        return "booking-success";
    }

    // 4. LỊCH SỬ ĐẶT VÉ (Đã fix lỗi 500 Lazy Load)
    @GetMapping("/history") public String showHistory(Model model, Principal principal) { if (principal == null) return "redirect:/login"; User user = userRepository.findByEmail(principal.getName()).orElse(null); if (user == null) return "redirect:/login"; try {
        List<Booking> list = bookingRepository.findByUserIdWithScheduleAndTour(user.getId());
        List<BookingResponseDTO> dtos = list.stream() .map(BookingResponseDTO::fromEntity) .collect(Collectors.toList()); model.addAttribute("bookings", dtos); return "booking-history"; } catch (Exception ex) {
        ex.printStackTrace();
        model.addAttribute("errorMessage", "Đã có lỗi khi tải lịch sử. Vui lòng thử lại sau."); return "error-page";  } }

    // 5. Submit booking (API cho việc tích hợp VNPAY)
    @PostMapping("/submit")
    public String submitBooking(@ModelAttribute BookingRequestDTO bookingRequest,
                                HttpServletRequest request) {

        // Lưu booking vào DB
        Booking savedBooking = bookingService.bookTour(bookingRequest);

        // Nếu chọn thanh toán VNPAY
        if (savedBooking.getPaymentMethod() == PaymentMethod.VNPAY) {

            BigDecimal total = savedBooking.getTotalAmount() == null ? BigDecimal.ZERO : savedBooking.getTotalAmount();
            long amount = total.longValue();

            String orderInfo = "Thanh toan tour " + savedBooking.getBookingCode();
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String returnUrl = baseUrl + "/api/v1/payment/vnpay-callback"; // Đảm bảo URL này khớp với Controller Callback của bạn

            // SỬA LỖI TẠI ĐÂY: Dùng instance 'vnPayService' (chữ thường) thay vì Class tĩnh
            String vnpayUrl = vnPayService.createPaymentUrl(amount, orderInfo, returnUrl, savedBooking.getBookingCode());

            return "redirect:" + vnpayUrl;
        }

        return "redirect:/booking/success?code=" + savedBooking.getBookingCode();
    }

    // Debug
    @GetMapping("/debug-price/{scheduleId}")
    @ResponseBody
    public String debugPrice(@PathVariable Integer scheduleId) {
        DepartureSchedule s = scheduleRepository.findById(scheduleId).orElse(null);
        if (s == null) return "❌ Lịch trình ID " + scheduleId + " KHÔNG TỒN TẠI!";
        return "Giá NL: " + s.getPriceAdult() + " | Giá TE: " + s.getPriceChild();
    }
}