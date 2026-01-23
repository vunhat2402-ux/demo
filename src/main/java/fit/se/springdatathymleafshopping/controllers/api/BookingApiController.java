package fit.se.springdatathymleafshopping.controllers.api;

import fit.se.springdatathymleafshopping.dtos.BookingRequestDTO;
import fit.se.springdatathymleafshopping.dtos.PassengerDTO;
import fit.se.springdatathymleafshopping.entities.*;
import fit.se.springdatathymleafshopping.entities.enums.BookingStatus;
import fit.se.springdatathymleafshopping.entities.enums.PassengerType;
import fit.se.springdatathymleafshopping.entities.enums.PaymentMethod;
import fit.se.springdatathymleafshopping.repositories.*;
import fit.se.springdatathymleafshopping.services.MoMoService;
import fit.se.springdatathymleafshopping.services.VNPayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/bookings")
@CrossOrigin(origins = "*")
@Transactional(rollbackFor = Exception.class)
public class BookingApiController {

    private final BookingRepository bookingRepo;
    private final BookingPassengerRepository passengerRepo;
    private final DepartureScheduleRepository scheduleRepo;
    private final UserRepository userRepo;
    private final VNPayService vnPayService;
    private final MoMoService moMoService;

    public BookingApiController(BookingRepository bookingRepo,
                                BookingPassengerRepository passengerRepo,
                                DepartureScheduleRepository scheduleRepo,
                                UserRepository userRepo,
                                VNPayService vnPayService,
                                MoMoService moMoService) {
        this.bookingRepo = bookingRepo;
        this.passengerRepo = passengerRepo;
        this.scheduleRepo = scheduleRepo;
        this.userRepo = userRepo;
        this.vnPayService = vnPayService;
        this.moMoService = moMoService;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequestDTO req,
                                           @RequestParam(value = "scheduleId", required = false) Integer scheduleIdFromQuery) {

        // 1. Xử lý ID lịch trình
        if (req.getScheduleId() == null && scheduleIdFromQuery != null) {
            req.setScheduleId(scheduleIdFromQuery);
        }
        if (req.getScheduleId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng cung cấp Schedule ID"));
        }

        DepartureSchedule schedule = scheduleRepo.findById(req.getScheduleId())
                .orElseThrow(() -> new RuntimeException("Lịch trình không tồn tại"));

        // 2. Tạo danh sách hành khách mặc định nếu thiếu
        if (req.getPassengers() == null || req.getPassengers().isEmpty()) {
            List<PassengerDTO> auto = new ArrayList<>();
            int adults = req.getAdultCount() == null ? 1 : req.getAdultCount();
            int children = req.getChildCount() == null ? 0 : req.getChildCount();
            int infants = req.getInfantCount() == null ? 0 : req.getInfantCount();

            for (int i = 0; i < adults; i++) auto.add(new PassengerDTO("Người lớn " + (i + 1), "ADULT", "UNKNOWN", null));
            for (int i = 0; i < children; i++) auto.add(new PassengerDTO("Trẻ em " + (i + 1), "CHILD", "UNKNOWN", null));
            for (int i = 0; i < infants; i++) auto.add(new PassengerDTO("Em bé " + (i + 1), "INFANT", "UNKNOWN", null));

            req.setPassengers(auto);
        }

        // 3. Kiểm tra số lượng chỗ trống (Trừ kho)
        int currentBooked = schedule.getBooked() == null ? 0 : schedule.getBooked();
        int quota = schedule.getQuota() == null ? Integer.MAX_VALUE : schedule.getQuota();
        int totalGuests = req.getPassengers().size();

        if (currentBooked + totalGuests > quota) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Lịch khởi hành này đã hết chỗ hoặc không đủ số lượng ghế trống."));
        }

        // 4. Tính toán tổng tiền
        BigDecimal adultPrice = schedule.getPriceAdult() == null ? BigDecimal.ZERO : schedule.getPriceAdult();
        BigDecimal childPrice = schedule.getPriceChild() != null ? schedule.getPriceChild() : adultPrice.multiply(new BigDecimal("0.75"));
        BigDecimal infantPrice = schedule.getPriceInfant() != null ? schedule.getPriceInfant() : adultPrice.multiply(new BigDecimal("0.10"));

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PassengerDTO p : req.getPassengers()) {
            String type = p.getType() == null ? "ADULT" : p.getType().toUpperCase();
            if ("CHILD".equals(type)) {
                totalAmount = totalAmount.add(childPrice);
            } else if ("INFANT".equals(type)) {
                totalAmount = totalAmount.add(infantPrice);
            } else {
                totalAmount = totalAmount.add(adultPrice);
            }
        }
        // Làm tròn tiền
        totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);

        // 5. Tạo và Lưu Booking
        Booking booking = new Booking();
        booking.setBookingCode("BK-" + System.currentTimeMillis());
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setSchedule(schedule);
        booking.setTotalAmount(totalAmount);
        booking.setPaidAmount(BigDecimal.ZERO);
        booking.setNote(req.getNotes());

        // Xử lý phương thức thanh toán
        if (req.getPaymentMethod() != null) {
            try {
                booking.setPaymentMethod(PaymentMethod.valueOf(req.getPaymentMethod().toUpperCase()));
            } catch (Exception e) {
                booking.setPaymentMethod(PaymentMethod.CASH);
            }
        } else {
            booking.setPaymentMethod(PaymentMethod.CASH);
        }

        // Gán thông tin User hoặc Khách lẻ
        if (req.getUserId() != null) {
            User user = userRepo.findById(req.getUserId()).orElse(null);
            booking.setUser(user);
            if (user != null) {
                booking.setCustomerName(user.getFullName());
                booking.setCustomerEmail(user.getEmail());
                booking.setCustomerPhone(user.getPhone());
            }
        } else {
            booking.setCustomerName(req.getCustomerName());
            booking.setCustomerEmail(req.getCustomerEmail());
            booking.setCustomerPhone(req.getCustomerPhone());
        }

        Booking savedBooking = bookingRepo.save(booking);

        // 6. Lưu danh sách hành khách vào DB
        for (PassengerDTO p : req.getPassengers()) {
            BookingPassenger bp = new BookingPassenger();
            bp.setBooking(savedBooking);
            bp.setFullName(p.getFullName());
            bp.setGender(p.getGender());
            try {
                bp.setType(PassengerType.valueOf(p.getType()));
            } catch (Exception e) {
                bp.setType(PassengerType.ADULT);
            }
            bp.setDob(p.getDob() != null ? p.getDob() : LocalDate.now());
            passengerRepo.save(bp);
        }

        // 7. Cập nhật số lượng đã đặt (Trừ kho)
        schedule.setBooked(currentBooked + totalGuests);
        scheduleRepo.save(schedule);

        // 8. TẠO LINK THANH TOÁN (VNPAY / MOMO)
        String paymentUrl = "";
        String method = req.getPaymentMethod() != null ? req.getPaymentMethod().toUpperCase() : "CASH";

        if ("VNPAY".equals(method)) {
            // URL trả về phải khớp với PaymentController
            String returnUrl = "http://localhost:8080/payment/vnpay-callback";
            paymentUrl = vnPayService.createPaymentUrl(
                    savedBooking.getTotalAmount().longValue(),
                    "Thanh toan " + savedBooking.getBookingCode(),
                    returnUrl,
                    savedBooking.getBookingCode()
            );
        } else if ("MOMO".equals(method)) {
            paymentUrl = moMoService.createPaymentUrl(savedBooking);
        }

        // 9. Trả về kết quả cho Frontend
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", savedBooking.getId()); // QUAN TRỌNG: ID để chuyển sang trang QR
        resp.put("bookingCode", savedBooking.getBookingCode());
        resp.put("totalAmount", savedBooking.getTotalAmount());
        resp.put("paymentUrl", paymentUrl);

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }
}