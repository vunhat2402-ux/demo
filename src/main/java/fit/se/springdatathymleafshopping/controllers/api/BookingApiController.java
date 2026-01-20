//@RestController
//@RequestMapping("/api/v1/bookings")
//@CrossOrigin(origins = "*")
//@Transactional(rollbackFor = Exception.class)
//public class BookingApiController {
//
//    @Autowired private BookingRepository bookingRepo;
//    @Autowired private BookingPassengerRepository passengerRepo;
//    @Autowired private DepartureScheduleRepository scheduleRepo;
//    @Autowired private UserRepository userRepo;
//
//    @PostMapping
//    public ResponseEntity<?> createBooking(@RequestBody BookingRequestDTO req) {
//
//        // 1. LẤY LỊCH KHỞI HÀNH
//        DepartureSchedule schedule = scheduleRepo.findById(req.getScheduleId())
//                .orElseThrow(() -> new RuntimeException("Lịch khởi hành không tồn tại"));
//
//        // 2. TẠO DANH SÁCH PASSENGER NẾU FORM KHÔNG GỬI
//        if (req.getPassengers() == null || req.getPassengers().isEmpty()) {
//            req.generatePassengersFromCount(); // bạn đã có logic này
//        }
//
//        int totalGuests = req.getPassengers().size();
//
//        // 3. CHECK CÒN CHỖ
//        if (schedule.getBooked() + totalGuests > schedule.getQuota()) {
//            return ResponseEntity.badRequest().body(
//                    Map.of("error", "Lịch này không đủ chỗ trống")
//            );
//        }
//
//        // 4. TÍNH TIỀN (CHUẨN NGHIỆP VỤ)
//        double adultPrice = schedule.getPriceAdult();
//        double childPrice = adultPrice * 0.75;
//        double infantPrice = adultPrice * 0.10;
//
//        double totalAmount = 0;
//
//        for (PassengerDTO p : req.getPassengers()) {
//            String type = p.getType() != null ? p.getType() : "ADULT";
//            switch (type) {
//                case "CHILD": totalAmount += childPrice; break;
//                case "INFANT": totalAmount += infantPrice; break;
//                default: totalAmount += adultPrice;
//            }
//        }
//
//        // ÉP KIỂU TIỀN
//        long finalAmount = Math.round(totalAmount);
//
//        if (finalAmount <= 0) {
//            throw new RuntimeException("Tổng tiền không hợp lệ");
//        }
//
//        // 5. TẠO BOOKING
//        Booking booking = new Booking();
//        booking.setBookingCode("BK-" + System.currentTimeMillis());
//        booking.setBookingDate(LocalDateTime.now());
//        booking.setStatus("PENDING");
//        booking.setSchedule(schedule);
//        booking.setTotalAmount((double) finalAmount);
//        booking.setPaymentMethod(req.getPaymentMethod());
//        booking.setNote(req.getNotes());
//
//        // USER / GUEST
//        if (req.getUserId() != null) {
//            User user = userRepo.findById(req.getUserId()).orElse(null);
//            booking.setUser(user);
//            if (user != null) {
//                booking.setCustomerName(user.getFullName());
//                booking.setCustomerEmail(user.getEmail());
//            }
//        } else {
//            booking.setCustomerName(req.getCustomerName());
//            booking.setCustomerEmail(req.getCustomerEmail());
//            booking.setCustomerPhone(req.getCustomerPhone());
//        }
//
//        Booking savedBooking = bookingRepo.save(booking);
//
//        // 6. LƯU PASSENGER
//        for (PassengerDTO p : req.getPassengers()) {
//            BookingPassenger bp = new BookingPassenger();
//            bp.setBooking(savedBooking);
//            bp.setFullName(p.getFullName());
//            bp.setGender(p.getGender());
//            bp.setType(p.getType());
//            bp.setDob(p.getDob() != null ? p.getDob() : LocalDate.now());
//            passengerRepo.save(bp);
//        }
//
//        // 7. TRỪ KHO
//        schedule.setBooked(schedule.getBooked() + totalGuests);
//        scheduleRepo.save(schedule);
//
//        // 8. TRẢ KẾT QUẢ
//        return ResponseEntity.ok(Map.of(
//                "bookingCode", savedBooking.getBookingCode(),
//                "totalAmount", finalAmount
//        ));
//    }
//}
