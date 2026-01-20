package fit.se.springdatathymleafshopping.services;

import fit.se.springdatathymleafshopping.dtos.BookingRequestDTO;
import fit.se.springdatathymleafshopping.dtos.PassengerDTO;
import fit.se.springdatathymleafshopping.entities.*;
import fit.se.springdatathymleafshopping.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.crypto.password.PasswordEncoder; // Mở lại nếu dùng
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired private BookingRepository bookingRepo;
    @Autowired private DepartureScheduleRepository scheduleRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private BookingPassengerRepository passengerRepo;
    @Autowired private VoucherRepository voucherRepository;

    @Transactional(rollbackFor = Exception.class)
    public Booking bookTour(BookingRequestDTO req) {
        // 1. TÌM LỊCH TRÌNH
        DepartureSchedule schedule = scheduleRepo.findById(req.getScheduleId())
                .orElseThrow(() -> new RuntimeException("Lịch trình không tồn tại!"));

        // 2. TỰ ĐỘNG SINH KHÁCH (Quan trọng: Nếu form không gửi danh sách khách)
        if (req.getPassengers() == null || req.getPassengers().isEmpty()) {
            System.out.println("⚠️ Danh sách khách trống. Đang tự động sinh dựa trên số lượng...");
            List<PassengerDTO> autoPassengers = new ArrayList<>();
            int adults = (req.getAdultCount() != null && req.getAdultCount() > 0) ? req.getAdultCount() : 1;
            int children = (req.getChildCount() != null) ? req.getChildCount() : 0;
            int infants = (req.getInfantCount() != null) ? req.getInfantCount() : 0;

            for (int i = 0; i < adults; i++) autoPassengers.add(new PassengerDTO("Người lớn " + (i+1), "ADULT", "MALE", null));
            for (int i = 0; i < children; i++) autoPassengers.add(new PassengerDTO("Trẻ em " + (i+1), "CHILD", "MALE", null));
            for (int i = 0; i < infants; i++) autoPassengers.add(new PassengerDTO("Em bé " + (i+1), "INFANT", "MALE", null));

            req.setPassengers(autoPassengers);
        }

        // 3. TÍNH TIỀN (LOGIC CHUẨN)
        double adultPrice  = schedule.getPriceAdult();
        double childPrice  = adultPrice * 0.75;
        double infantPrice = adultPrice * 0.10;

        double totalAmount = 0.0;

        for (PassengerDTO p : req.getPassengers()) {
            String type = p.getType() != null ? p.getType() : "ADULT";
            double price;

            switch (type) {
                case "CHILD":
                    price = childPrice;
                    break;
                case "INFANT":
                    price = infantPrice;
                    break;
                default:
                    price = adultPrice;
            }

            totalAmount += price;
        }

        // 4. XỬ LÝ USER
        User user = null;
        if (req.getUserId() != null) user = userRepo.findById(req.getUserId()).orElse(null);
        if (user == null && req.getCustomerEmail() != null) user = userRepo.findByEmail(req.getCustomerEmail()).orElse(null);

        // 5. TẠO BOOKING
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSchedule(schedule);
        booking.setBookingDate(LocalDateTime.now());
        booking.setBookingCode("BK-" + System.currentTimeMillis());
        booking.setStatus("PENDING");
        booking.setTotalAmount(totalAmount); // Lưu số tiền đã tính

        booking.setCustomerName(req.getCustomerName());
        booking.setCustomerPhone(req.getCustomerPhone());
        booking.setCustomerEmail(req.getCustomerEmail());
        booking.setPaymentMethod(req.getPaymentMethod());
        booking.setNote(req.getNotes());

        Booking savedBooking = bookingRepo.save(booking);

        // 6. LƯU CHI TIẾT
        for (PassengerDTO pDto : req.getPassengers()) {
            BookingPassenger p = new BookingPassenger();
            p.setBooking(savedBooking);
            p.setFullName(pDto.getFullName());
            p.setDob(pDto.getDob() != null ? pDto.getDob() : LocalDate.now());
            p.setGender(pDto.getGender());
            p.setType(pDto.getType());
            passengerRepo.save(p);
        }

        // Update kho
        schedule.setBooked(schedule.getBooked() + req.getPassengers().size());
        scheduleRepo.save(schedule);

        return savedBooking;
    }
}