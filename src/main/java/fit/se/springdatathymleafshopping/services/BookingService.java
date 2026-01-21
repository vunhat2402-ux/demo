package fit.se.springdatathymleafshopping.services;

import fit.se.springdatathymleafshopping.dtos.BookingRequestDTO;
import fit.se.springdatathymleafshopping.dtos.PassengerDTO;
import fit.se.springdatathymleafshopping.entities.*;
import fit.se.springdatathymleafshopping.entities.enums.BookingStatus;
import fit.se.springdatathymleafshopping.entities.enums.PassengerType;
import fit.se.springdatathymleafshopping.entities.enums.PaymentMethod;
import fit.se.springdatathymleafshopping.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepo;
    private final DepartureScheduleRepository scheduleRepo;
    private final UserRepository userRepo;
    private final BookingPassengerRepository passengerRepo;
    private final VoucherRepository voucherRepository;

    public BookingService(BookingRepository bookingRepo,
                          DepartureScheduleRepository scheduleRepo,
                          UserRepository userRepo,
                          BookingPassengerRepository passengerRepo,
                          VoucherRepository voucherRepository) {
        this.bookingRepo = bookingRepo;
        this.scheduleRepo = scheduleRepo;
        this.userRepo = userRepo;
        this.passengerRepo = passengerRepo;
        this.voucherRepository = voucherRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Booking bookTour(BookingRequestDTO req) {
        // 1. Tìm schedule
        DepartureSchedule schedule = scheduleRepo.findById(req.getScheduleId())
                .orElseThrow(() -> new RuntimeException("Lịch trình không tồn tại!"));

        // 2. Nếu không có passengers, tự sinh
        if (req.getPassengers() == null || req.getPassengers().isEmpty()) {
            List<PassengerDTO> autoPassengers = new ArrayList<>();
            int adults = (req.getAdultCount() != null && req.getAdultCount() > 0) ? req.getAdultCount() : 1;
            int children = (req.getChildCount() != null) ? req.getChildCount() : 0;
            int infants = (req.getInfantCount() != null) ? req.getInfantCount() : 0;

            for (int i = 0; i < adults; i++) autoPassengers.add(new PassengerDTO("Người lớn " + (i+1), "ADULT", "MALE", null));
            for (int i = 0; i < children; i++) autoPassengers.add(new PassengerDTO("Trẻ em " + (i+1), "CHILD", "MALE", null));
            for (int i = 0; i < infants; i++) autoPassengers.add(new PassengerDTO("Em bé " + (i+1), "INFANT", "MALE", null));

            req.setPassengers(autoPassengers);
        }

        // 3. Tính tiền bằng BigDecimal
        BigDecimal adultPrice = schedule.getPriceAdult() == null ? BigDecimal.ZERO : schedule.getPriceAdult();
        BigDecimal childPrice = schedule.getPriceChild() != null
                ? schedule.getPriceChild()
                : adultPrice.multiply(new BigDecimal("0.75")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal infantPrice = schedule.getPriceInfant() != null
                ? schedule.getPriceInfant()
                : adultPrice.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PassengerDTO p : req.getPassengers()) {
            String type = p.getType() == null ? "ADULT" : p.getType().toUpperCase();
            switch (type) {
                case "CHILD":
                    totalAmount = totalAmount.add(childPrice);
                    break;
                case "INFANT":
                    totalAmount = totalAmount.add(infantPrice);
                    break;
                default:
                    totalAmount = totalAmount.add(adultPrice);
            }
        }
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

        // 4. Xử lý user (nếu có)
        User user = null;
        if (req.getUserId() != null) user = userRepo.findById(req.getUserId()).orElse(null);
        if (user == null && req.getCustomerEmail() != null) user = userRepo.findByEmail(req.getCustomerEmail()).orElse(null);

        // 5. Tạo booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSchedule(schedule);
        booking.setBookingDate(LocalDateTime.now());
        booking.setBookingCode("BK-" + System.currentTimeMillis());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(totalAmount);
        booking.setPaidAmount(BigDecimal.ZERO);
        booking.setDiscountAmount(BigDecimal.ZERO);

        booking.setCustomerName(req.getCustomerName());
        booking.setCustomerPhone(req.getCustomerPhone());
        booking.setCustomerEmail(req.getCustomerEmail());

        // Payment method: nếu entity dùng enum PaymentMethod, set enum; nếu entity stores String, set name()
        if (req.getPaymentMethod() != null) {
            try {
                PaymentMethod pm = PaymentMethod.valueOf(req.getPaymentMethod().toUpperCase());
                // nếu Booking.paymentMethod là enum type:
                booking.setPaymentMethod(pm);
            } catch (Exception ex) {
                // nếu không parse được, để null hoặc xử lý theo logic cũ
                booking.setPaymentMethod(null);
            }
        }

        booking.setNote(req.getNotes());

        Booking savedBooking = bookingRepo.save(booking);

        // 6. Lưu passengers
        for (PassengerDTO pDto : req.getPassengers()) {
            BookingPassenger p = new BookingPassenger();
            p.setBooking(savedBooking);
            p.setFullName(pDto.getFullName());
            p.setDob(pDto.getDob() != null ? pDto.getDob() : LocalDate.now());
            p.setGender(pDto.getGender());

            try {
                PassengerType pt = PassengerType.valueOf((pDto.getType() == null ? "ADULT" : pDto.getType()).toUpperCase());
                p.setType(pt);
            } catch (Exception ex) {
                p.setType(PassengerType.ADULT);
            }

            passengerRepo.save(p);
        }

        // 7. Cập nhật booked an toàn
        int prevBooked = schedule.getBooked() == null ? 0 : schedule.getBooked();
        int addCount = req.getPassengers() == null ? 0 : req.getPassengers().size();
        schedule.setBooked(prevBooked + addCount);
        scheduleRepo.save(schedule);

        return savedBooking;
    }
}
