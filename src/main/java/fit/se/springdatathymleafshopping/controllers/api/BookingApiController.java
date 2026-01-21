package fit.se.springdatathymleafshopping.controllers.api;

import fit.se.springdatathymleafshopping.dtos.BookingRequestDTO;
import fit.se.springdatathymleafshopping.dtos.PassengerDTO;
import fit.se.springdatathymleafshopping.entities.*;
import fit.se.springdatathymleafshopping.entities.enums.BookingStatus;
import fit.se.springdatathymleafshopping.entities.enums.PassengerType;
import fit.se.springdatathymleafshopping.entities.enums.PaymentMethod;
import fit.se.springdatathymleafshopping.repositories.*;
import fit.se.springdatathymleafshopping.services.VNPayService; // <--- 1. IMPORT SERVICE MỚI
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
    private final VNPayService vnPayService; // <--- 2. KHAI BÁO BIẾN

    // 3. INJECT SERVICE VÀO CONSTRUCTOR
    public BookingApiController(BookingRepository bookingRepo,
                                BookingPassengerRepository passengerRepo,
                                DepartureScheduleRepository scheduleRepo,
                                UserRepository userRepo,
                                VNPayService vnPayService) {
        this.bookingRepo = bookingRepo;
        this.passengerRepo = passengerRepo;
        this.scheduleRepo = scheduleRepo;
        this.userRepo = userRepo;
        this.vnPayService = vnPayService;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequestDTO req,
                                           @RequestParam(value = "scheduleId", required = false) Integer scheduleIdFromQuery) {

        // ==================================================================
        // PHẦN 1: LOGIC CŨ CỦA BẠN (GIỮ NGUYÊN 100%)
        // ==================================================================

        // allow scheduleId in query param for legacy frontend
        if (req.getScheduleId() == null && scheduleIdFromQuery != null) {
            req.setScheduleId(scheduleIdFromQuery);
        }

        if (req.getScheduleId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "scheduleId is required"));
        }

        // 1. LẤY LỊCH KHỞI HÀNH
        DepartureSchedule schedule = scheduleRepo.findById(req.getScheduleId())
                .orElseThrow(() -> new RuntimeException("Lịch khởi hành không tồn tại"));

        // 2. TẠO DANH SÁCH PASSENGER NẾU FORM KHÔNG GỬI
        if (req.getPassengers() == null || req.getPassengers().isEmpty()) {
            List<PassengerDTO> auto = new ArrayList<>();
            int adults = req.getAdultCount() == null ? 1 : req.getAdultCount();
            int children = req.getChildCount() == null ? 0 : req.getChildCount();
            int infants = req.getInfantCount() == null ? 0 : req.getInfantCount();
            for (int i = 0; i < adults; i++) auto.add(new PassengerDTO("Người lớn " + (i+1), "ADULT", "UNKNOWN", null));
            for (int i = 0; i < children; i++) auto.add(new PassengerDTO("Trẻ em " + (i+1), "CHILD", "UNKNOWN", null));
            for (int i = 0; i < infants; i++) auto.add(new PassengerDTO("Em bé " + (i+1), "INFANT", "UNKNOWN", null));
            req.setPassengers(auto);
        }

        int totalGuests = req.getPassengers() == null ? 0 : req.getPassengers().size();

        // 3. CHECK CÒN CHỖ
        int booked = schedule.getBooked() == null ? 0 : schedule.getBooked();
        int quota = schedule.getQuota() == null ? Integer.MAX_VALUE : schedule.getQuota();
        if (booked + totalGuests > quota) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Lịch này không đủ chỗ trống"));
        }

        // 4. TÍNH TIỀN (Giữ nguyên logic BigDecimal của bạn)
        BigDecimal adultPrice = schedule.getPriceAdult() == null ? BigDecimal.ZERO : schedule.getPriceAdult();
        BigDecimal childPrice = schedule.getPriceChild() != null
                ? schedule.getPriceChild()
                : adultPrice.multiply(new BigDecimal("0.75")).setScale(0, RoundingMode.HALF_UP);
        BigDecimal infantPrice = schedule.getPriceInfant() != null
                ? schedule.getPriceInfant()
                : adultPrice.multiply(new BigDecimal("0.10")).setScale(0, RoundingMode.HALF_UP);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PassengerDTO p : req.getPassengers()) {
            String type = p.getType() == null ? "ADULT" : p.getType().toUpperCase();
            switch (type) {
                case "CHILD": totalAmount = totalAmount.add(childPrice); break;
                case "INFANT": totalAmount = totalAmount.add(infantPrice); break;
                default: totalAmount = totalAmount.add(adultPrice);
            }
        }
        totalAmount = totalAmount.setScale(0, RoundingMode.HALF_UP);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Tổng tiền không hợp lệ"));
        }

        // 5. TẠO BOOKING
        Booking booking = new Booking();
        booking.setBookingCode("BK-" + System.currentTimeMillis());
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setSchedule(schedule);
        booking.setTotalAmount(totalAmount);
        booking.setPaidAmount(BigDecimal.ZERO);
        booking.setDiscountAmount(BigDecimal.ZERO);

        if (req.getPaymentMethod() != null) {
            try {
                PaymentMethod pm = PaymentMethod.valueOf(req.getPaymentMethod().toUpperCase());
                booking.setPaymentMethod(pm);
            } catch (Exception ex) {
                booking.setPaymentMethod(null);
            }
        }

        booking.setNote(req.getNotes());

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

        // 6. LƯU PASSENGER
        for (PassengerDTO p : req.getPassengers()) {
            BookingPassenger bp = new BookingPassenger();
            bp.setBooking(savedBooking);
            bp.setFullName(p.getFullName());
            bp.setGender(p.getGender());
            bp.setType(PassengerType.valueOf(p.getType()));
            bp.setDob(p.getDob() != null ? p.getDob() : LocalDate.now());
            passengerRepo.save(bp);
        }

        // 7. TRỪ KHO
        schedule.setBooked(booked + totalGuests);
        scheduleRepo.save(schedule);

        // ==================================================================
        // PHẦN 2: THÊM MỚI (TẠO LINK VNPAY)
        // ==================================================================

        String paymentUrl = "";

        // Chỉ tạo link nếu phương thức thanh toán là VNPAY
        if (req.getPaymentMethod() != null && "VNPAY".equalsIgnoreCase(req.getPaymentMethod())) {
            String returnUrl = "http://localhost:8080/api/payment/vnpay-callback";

            // Gọi Service VNPAY, truyền đúng bookingCode vào
            paymentUrl = vnPayService.createPaymentUrl(
                    savedBooking.getTotalAmount().longValue(),
                    "Thanh toan " + savedBooking.getBookingCode(),
                    returnUrl,
                    savedBooking.getBookingCode() // Key chính để khớp đơn hàng
            );
        }

        // 8. TRẢ KẾT QUẢ (Thêm paymentUrl vào response)
        Map<String, Object> resp = new HashMap<>();
        resp.put("bookingCode", savedBooking.getBookingCode());
        resp.put("totalAmount", totalAmount);
        resp.put("paymentUrl", paymentUrl); // Frontend sẽ nhận link này

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }
}