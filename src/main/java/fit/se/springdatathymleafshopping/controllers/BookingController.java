package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.dtos.BookingRequestDTO;
import fit.se.springdatathymleafshopping.entities.*;
import fit.se.springdatathymleafshopping.repositories.*;
import fit.se.springdatathymleafshopping.services.BookingService;
import fit.se.springdatathymleafshopping.services.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/booking")
public class BookingController {

    @Autowired private DepartureScheduleRepository scheduleRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BookingService bookingService;
    @Autowired private VNPayService VNPayService;

    // 1. HIỆN TRANG ĐẶT TOUR
    @GetMapping("/create")
    public String showBookingForm(@RequestParam("scheduleId") Integer scheduleId,
                                  Model model, Principal principal) {

        DepartureSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Lịch trình không tồn tại"));

        model.addAttribute("schedule", schedule);

        // Nếu đã đăng nhập thì điền sẵn thông tin, nếu chưa thì để null
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            model.addAttribute("user", user);
        } else {
            model.addAttribute("user", null); // Khách vãng lai
        }

        return "booking-create";
    }

    // 2. XỬ LÝ ĐẶT TOUR (Lưu vào DB)
    @PostMapping("/create")
    public String createBooking(@RequestParam Integer scheduleId,
                                @RequestParam String customerName,
                                @RequestParam String customerPhone,
                                @RequestParam String customerEmail,
                                @RequestParam Integer adultCount,
                                @RequestParam Integer childCount,
                                @RequestParam Long totalAmount, // Lấy từ form (hoặc tính lại ở đây cho an toàn)
                                Principal principal) {

        DepartureSchedule schedule = scheduleRepository.findById(scheduleId).get();

        Booking booking = new Booking();
        booking.setBookingCode("BK-" + System.currentTimeMillis()); // Mã đơn ngẫu nhiên
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus("PENDING"); // Mới đặt là Chờ thanh toán
        booking.setCustomerName(customerName);
        booking.setCustomerPhone(customerPhone);
        booking.setCustomerEmail(customerEmail);
        booking.setTotalAmount(Double.valueOf(totalAmount));
        booking.setSchedule(schedule);

        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            booking.setUser(user);
        }

        bookingRepository.save(booking);

        // Cập nhật số chỗ đã đặt
        schedule.setBooked(schedule.getBooked() + adultCount + childCount);
        scheduleRepository.save(schedule);

        return "redirect:/booking/success?code=" + booking.getBookingCode();
    }

    // 3. TRANG THÀNH CÔNG
    @GetMapping("/success")
    public String showSuccess(@RequestParam String code, Model model) {
        model.addAttribute("code", code);
        return "booking-success";
    }

    // 4. LỊCH SỬ ĐẶT VÉ (Khớp với link ở menu)
    @GetMapping("/history")
    public String showHistory(Model model, Principal principal ,Integer userId) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElse(null);

        // Cần thêm hàm findByUser trong BookingRepository
        List<Booking> list = bookingRepository.findByUserId(userId);
        model.addAttribute("bookings", list);

        return "booking-history";
    }
    @PostMapping("/submit")
    public String submitBooking(@ModelAttribute BookingRequestDTO bookingRequest,
                                HttpServletRequest request) { // 1. Thêm request để lấy địa chỉ web

        Booking savedBooking = bookingService.bookTour(bookingRequest);

        if ("VNPAY".equals(savedBooking.getPaymentMethod())) {

            // 2. Ép kiểu tiền về long (Service sẽ tự nhân 100 sau)
            long amount = savedBooking.getTotalAmount().longValue();

            String orderInfo = "Thanh toan tour " + savedBooking.getBookingCode();

            // 3. Tạo địa chỉ trả về động (http://localhost:8080/booking/payment-result)
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String returnUrl = baseUrl + "/booking/payment-result"; // Đảm bảo bạn có mapping URL này trong Controller

            // 4. Gọi Service với ĐỦ 3 THAM SỐ
            String vnpayUrl = VNPayService.createPaymentUrl(amount, orderInfo, returnUrl);

            return "redirect:" + vnpayUrl;
        }

        return "redirect:/booking/success?code=" + savedBooking.getBookingCode();
    }
    // --- API DEBUG SIÊU CẤP ---
    // Chạy thử link này: http://localhost:8080/booking/debug-price/1  (Thay 1 bằng ID lịch trình bạn đang test)
    @GetMapping("/debug-price/{scheduleId}")
    @ResponseBody
    public String debugPrice(@PathVariable Integer scheduleId) {
        DepartureSchedule s = scheduleRepository.findById(scheduleId).orElse(null);

        if (s == null) return "❌ Lịch trình ID " + scheduleId + " KHÔNG TỒN TẠI!";

        StringBuilder sb = new StringBuilder();
        sb.append("🔍 THÔNG TIN LỊCH TRÌNH ID: ").append(scheduleId).append("<br>");
        sb.append("--------------------------------------------------<br>");
        sb.append("📅 Ngày đi: ").append(s.getStartDate()).append("<br>");

        // Soi kỹ giá trị gốc (Raw value)
        sb.append("💰 Giá Người lớn (Gốc): ").append(s.getPriceAdult()).append("<br>");
        sb.append("💰 Giá Trẻ em (Gốc): ").append(s.getPriceChild()).append("<br>");

        // Kiểm tra xem có bị null hay 0 không
        if (s.getPriceAdult() == null || s.getPriceAdult() == 0) {
            sb.append("❌ CẢNH BÁO: Giá đang bị NULL hoặc 0! <br>");
            sb.append("👉 Nguyên nhân: Hibernate chưa map được cột 'price_adult' HOẶC data.sql chưa chạy.");
        } else {
            sb.append("✅ GIÁ TỐT: Dữ liệu đã vào Java thành công!");
        }

        return sb.toString();
    }
}