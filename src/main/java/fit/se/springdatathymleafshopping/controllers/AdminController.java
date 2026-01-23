package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.*;
import fit.se.springdatathymleafshopping.entities.enums.BookingStatus;
import fit.se.springdatathymleafshopping.entities.enums.DiscountType;
import fit.se.springdatathymleafshopping.repositories.*;
import fit.se.springdatathymleafshopping.services.AdminStatisticsService;
import fit.se.springdatathymleafshopping.services.TourService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private TourService tourService;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private DepartureScheduleRepository scheduleRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TourRepository tourRepository;
    @Autowired private TourImageRepository tourImageRepository;
    @Autowired private TourItineraryRepository tourItineraryRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private DestinationRepository destinationRepository;
    @Autowired private UserLogRepository userLogRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AdminStatisticsService statsService;
    @Autowired private ConsultationRequestRepository consultationRepo;

    // 1. PROFILE
    @GetMapping("/profile")
    public String myProfile(Model model) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow();
        model.addAttribute("user", currentUser);
        model.addAttribute("listRoles", roleRepository.findAll());
        return "admin/user-form";
    }

    // --- LOGGING ---
    private void saveLog(String action, String description) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email).orElse(null);

        if (currentUser != null) {
            UserLog log = new UserLog();
            log.setUser(currentUser);
            log.setAction(action);
            log.setDescription(description);
            userLogRepository.save(log);
        }
    }

    // ======================== USER MANAGEMENT ========================
    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("staffList", userRepository.findByRoles_Name("STAFF"));
        model.addAttribute("customerList", userRepository.findByRoles_Name("USER"));
        return "admin/user-list";
    }

    @GetMapping("/users/toggle-lock/{id}")
    public String toggleLockUser(@PathVariable("id") Integer id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setLocked(user.getLocked() == null ? true : !user.getLocked());
        userRepository.save(user);
        saveLog("QUẢN LÝ USER", (user.getLocked() ? "Khóa" : "Mở khóa") + " tài khoản: " + user.getEmail());
        return "redirect:/admin/users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Integer id) {
        User user = userRepository.findById(id).orElseThrow();

        // delete user logs first
        userLogRepository.deleteByUserId(id);

        // clear roles in join table
        user.getRoles().clear();
        userRepository.save(user);

        // delete user
        userRepository.delete(user);

        saveLog("QUẢN LÝ USER", "Đã xóa vĩnh viễn tài khoản: " + user.getEmail());
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Integer id, Model model) {
        User user = userRepository.findById(id).orElseThrow();
        user.setPassword("");
        model.addAttribute("user", user);
        model.addAttribute("listRoles", roleRepository.findAll());
        return "admin/user-form";
    }

    @GetMapping("/users/detail/{id}")
    @Transactional(readOnly = true)
    public String viewUserDetail(@PathVariable("id") Integer id, Model model) {
        User user = userRepository.findById(id).orElseThrow();
        List<Booking> history = bookingRepository.findByUserId(id);
        model.addAttribute("user", user);
        model.addAttribute("history", history);
        return "admin/user-detail";
    }

    // ======================== TOUR MANAGEMENT ========================
    @GetMapping("/tours/edit/{id}")
    public String showEditTourForm(@PathVariable("id") Integer id, Model model) {
        Tour tour = tourRepository.findById(id).orElseThrow();
        model.addAttribute("tour", tour);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("destinations", destinationRepository.findAll());
        return "admin/tour-form";
    }

    @GetMapping("/tours/add")
    public String showAddTourForm(Model model) {
        model.addAttribute("tour", new Tour());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("destinations", destinationRepository.findAll());
        return "admin/tour-form";
    }

    @PostMapping("/tours/save")
    public String saveTour(@ModelAttribute("tour") Tour tourForm,
                           @RequestParam(value = "destinationId", required = false) Integer destinationId,
                           @RequestParam(value = "categoryId", required = false) Integer categoryId,
                           Model model) {

        boolean isEdit = tourForm.getId() != null;

        if (tourRepository.existsByCode(tourForm.getCode())) {
            Tour existingTour = tourRepository.findByCode(tourForm.getCode());
            if (!isEdit || !existingTour.getId().equals(tourForm.getId())) {
                model.addAttribute("error", "Lỗi: Mã tour '" + tourForm.getCode() + "' đã tồn tại trên hệ thống!");
                model.addAttribute("categories", categoryRepository.findAll());
                model.addAttribute("destinations", destinationRepository.findAll());
                return "admin/tour-form";
            }
        }

        Tour tourToSave;
        if (isEdit) {
            tourToSave = tourRepository.findById(tourForm.getId()).orElse(new Tour());
            tourToSave.setName(tourForm.getName());
            tourToSave.setCode(tourForm.getCode() != null ? tourForm.getCode().toUpperCase() : null);
            tourToSave.setTransport(tourForm.getTransport());
            tourToSave.setDescription(tourForm.getDescription());
            tourToSave.setDeparturePoint(tourForm.getDeparturePoint());
        } else {
            tourToSave = tourForm;
            tourToSave.setCode(tourForm.getCode() != null ? tourForm.getCode().toUpperCase() : null);
        }

        if (destinationId != null) tourToSave.setDestination(destinationRepository.findById(destinationId).orElse(null));
        if (categoryId != null) tourToSave.setCategory(categoryRepository.findById(categoryId).orElse(null));

        tourRepository.save(tourToSave);
        saveLog(isEdit ? "CẬP NHẬT TOUR" : "TẠO TOUR MỚI", "Tour: " + tourToSave.getName());
        return "redirect:/admin/tours";
    }

    // ======================== ITINERARY ========================
    @GetMapping("/tours/itinerary/{tourId}")
    public String showItinerary(@PathVariable("tourId") Integer tourId, Model model) {
        Tour tour = tourRepository.findById(tourId).orElseThrow();
        List<TourItinerary> itineraries = tourItineraryRepository.findByTourIdOrderByDayNumberAsc(tourId);
        model.addAttribute("tour", tour);
        model.addAttribute("itineraries", itineraries);
        model.addAttribute("newItinerary", new TourItinerary());
        return "admin/tour-itinerary";
    }

    @PostMapping("/tours/itinerary/save")
    public String saveItinerary(@ModelAttribute("newItinerary") TourItinerary itinerary,
                                @RequestParam("tourId") Integer tourId) {

        if (itinerary.getDayNumber() <= 0) {
            return "redirect:/admin/tours/itinerary/" + tourId + "?error=invalid_day";
        }

        TourItinerary existing = tourItineraryRepository.findByTourIdAndDayNumber(tourId, itinerary.getDayNumber());
        if (existing != null && !Objects.equals(existing.getId(), itinerary.getId())) {
            return "redirect:/admin/tours/itinerary/" + tourId + "?error=duplicate_day";
        }

        Tour tour = tourRepository.findById(tourId).orElseThrow();
        itinerary.setTour(tour);
        tourItineraryRepository.save(itinerary);
        saveLog("LỊCH TRÌNH", "Thêm/Sửa lịch trình Ngày " + itinerary.getDayNumber() + " cho tour " + tour.getCode());
        return "redirect:/admin/tours/itinerary/" + tourId;
    }

    @GetMapping("/tours/itinerary/delete/{id}")
    public String deleteItinerary(@PathVariable("id") Integer id) {
        TourItinerary itinerary = tourItineraryRepository.findById(id).orElseThrow();
        Integer tourId = itinerary.getTour().getId();
        int dayNum = itinerary.getDayNumber();
        String tourCode = itinerary.getTour().getCode();
        tourItineraryRepository.delete(itinerary);
        saveLog("LỊCH TRÌNH", "Xóa lịch trình Ngày " + dayNum + " của tour " + tourCode);
        return "redirect:/admin/tours/itinerary/" + tourId;
    }

    // ======================== IMAGES ========================
    @GetMapping("/tours/images/{tourId}")
    public String showImages(@PathVariable("tourId") Integer tourId, Model model) {
        Tour tour = tourRepository.findById(tourId).orElseThrow();
        model.addAttribute("tour", tour);
        model.addAttribute("images", tourImageRepository.findByTourId(tourId));
        return "admin/tour-images";
    }

    @PostMapping("/tours/images/save")
    public String saveImage(@RequestParam("tourId") Integer tourId, @RequestParam("imageUrl") String imageUrl) {
        Tour tour = tourRepository.findById(tourId).orElseThrow();
        TourImage image = new TourImage();
        image.setTour(tour);
        image.setImageUrl(imageUrl);
        tourImageRepository.save(image);
        saveLog("HÌNH ẢNH", "Thêm ảnh mới cho tour: " + tour.getCode());
        return "redirect:/admin/tours/images/" + tourId;
    }

    @GetMapping("/tours/images/delete/{id}")
    public String deleteImage(@PathVariable("id") Integer id) {
        TourImage image = tourImageRepository.findById(id).orElseThrow();
        Integer tourId = image.getTour().getId();
        String tourCode = image.getTour().getCode();
        tourImageRepository.delete(image);
        saveLog("HÌNH ẢNH", "Xóa một ảnh của tour: " + tourCode);
        return "redirect:/admin/tours/images/" + tourId;
    }

    @GetMapping("/tours")
    public String listTours(Model model) {
        model.addAttribute("tours", tourService.findAllTours());
        return "admin/tour-list";
    }

    // ======================== BOOKINGS ========================
    @GetMapping("/bookings")
    public String listBookings(Model model, @RequestParam(value = "search", required = false) String searchCode) {
        if (searchCode != null && !searchCode.trim().isEmpty()) {
            Booking booking = bookingRepository.findByBookingCode(searchCode.trim()).orElse(null);
            model.addAttribute("bookings", booking == null ? List.of() : List.of(booking));
            model.addAttribute("searchCode", searchCode);
        } else {
            List<Booking> list = bookingRepository.findAll();
            Collections.reverse(list);
            model.addAttribute("bookings", list);
        }
        return "admin/booking-list";
    }

    @GetMapping("/bookings/detail/{id}")
    @Transactional(readOnly = true)
    public String viewBookingDetail(@PathVariable("id") Integer id, Model model) {
        Booking booking = bookingRepository.findById(id).orElseThrow();
        // Access lazy relations inside transaction to avoid LazyInitializationException
        booking.getDetails().size();
        booking.getPassengers().size();
        model.addAttribute("booking", booking);
        return "admin/booking-detail";
    }

    @PostMapping("/bookings/update-status")
    public String updateBookingStatus(@RequestParam("id") Integer id, @RequestParam("status") String status) {
        Booking booking = bookingRepository.findById(id).orElseThrow();

        String oldStatus = booking.getStatus() != null ? booking.getStatus().name() : "N/A";

        try {
            BookingStatus newStatus = BookingStatus.valueOf(status.toUpperCase());
            booking.setStatus(newStatus);
        } catch (IllegalArgumentException ex) {
            return "redirect:/admin/bookings/detail/" + id + "?error=invalid_status";
        }

        bookingRepository.save(booking);
        saveLog("DUYỆT ĐƠN", "Đổi trạng thái đơn " + booking.getBookingCode() + ": " + oldStatus + " -> " + booking.getStatus().name());
        return "redirect:/admin/bookings/detail/" + id;
    }

    // ======================== VOUCHERS (ĐÃ NÂNG CẤP) ========================
    @GetMapping("/vouchers")
    public String listVouchers(Model model) {
        model.addAttribute("vouchers", voucherRepository.findAll());
        model.addAttribute("newVoucher", new Voucher()); // Form rỗng để tạo mới
        model.addAttribute("today", LocalDate.now());
        return "admin/voucher";
    }

    // 👇 API MỚI: HIỆN FORM SỬA VOUCHER
    @GetMapping("/vouchers/edit/{id}")
    public String editVoucher(@PathVariable("id") Integer id, Model model) {
        Voucher voucher = voucherRepository.findById(id).orElse(null);
        if (voucher == null) return "redirect:/admin/vouchers";

        model.addAttribute("vouchers", voucherRepository.findAll());
        model.addAttribute("newVoucher", voucher); // Đổ dữ liệu cũ vào form
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("isEditMode", true); // Cờ để giao diện biết đang sửa
        return "admin/voucher";
    }

    @PostMapping("/vouchers/create")
    public String createOrUpdateVoucher(@ModelAttribute("newVoucher") Voucher voucher,
                                        BindingResult bindingResult, // 👈 QUAN TRỌNG: Phải có cái này ngay sau @ModelAttribute
                                        Model model) {

        // 1. BẮT LỖI ĐỊNH DẠNG (Ngày tháng, Số, Enum...)
        // Nếu không có đoạn này, khi sai định dạng Spring sẽ trả về lỗi 400 trang trắng
        if (bindingResult.hasErrors()) {
            System.out.println(">>> LỖI BINDING: " + bindingResult.getAllErrors()); // In lỗi ra console để debug
            return returnVoucherError(model, "Lỗi định dạng dữ liệu! Vui lòng kiểm tra lại ngày tháng hoặc nhập liệu.");
        }

        boolean isUpdate = (voucher.getId() != null); // Kiểm tra xem có ID không (Sửa hay Tạo mới)

        // 2. Validate dữ liệu cơ bản (Số lượng, Giá trị...)
        if (voucher.getQuantity() == null || voucher.getQuantity() < 1 ||
                voucher.getDiscountValue() == null || voucher.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            return returnVoucherError(model, "Lỗi: Số lượng và Giá trị giảm phải lớn hơn 0!");
        }

        if (voucher.getDiscountType() == DiscountType.PERCENT && voucher.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            return returnVoucherError(model, "Lỗi: Giảm giá phần trăm không được quá 100%!");
        }

        if (voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(LocalDate.now())) {
            return returnVoucherError(model, "Lỗi: Hạn sử dụng không được nhỏ hơn ngày hiện tại!");
        }

        // 3. Validate Trùng Mã Code
        // Tìm voucher trong DB bằng code
        Voucher existing = voucherRepository.findByCode(voucher.getCode()).orElse(null);
        if (existing != null) {
            // Trường hợp 1: Tạo mới mà mã đã tồn tại -> Lỗi
            if (!isUpdate) {
                return returnVoucherError(model, "Lỗi: Mã '" + voucher.getCode() + "' đã tồn tại!");
            }
            // Trường hợp 2: Đang sửa (Update) mà mã lại trùng với một voucher KHÁC -> Lỗi
            // (existing.getId() khác với voucher.getId() đang sửa)
            if (isUpdate && !existing.getId().equals(voucher.getId())) {
                return returnVoucherError(model, "Lỗi: Mã '" + voucher.getCode() + "' đã thuộc về voucher khác!");
            }
        }

        // 4. Lưu vào Database
        voucherRepository.save(voucher);
        saveLog("KHUYẾN MÃI", (isUpdate ? "Cập nhật" : "Tạo mới") + " voucher: " + voucher.getCode());

        return "redirect:/admin/vouchers";
    }

    private String returnVoucherError(Model model, String msg) {
        model.addAttribute("error", msg);
        model.addAttribute("vouchers", voucherRepository.findAll());
        model.addAttribute("today", LocalDate.now());
        return "admin/voucher";
    }

    @GetMapping("/vouchers/delete/{id}")
    public String deleteVoucher(@PathVariable("id") Integer id) {
        Voucher v = voucherRepository.findById(id).orElse(null);
        if (v != null) {
            String code = v.getCode();
            voucherRepository.delete(v);
            saveLog("KHUYẾN MÃI", "Xóa mã giảm giá: " + code);
        }
        return "redirect:/admin/vouchers";
    }

    // ======================== OTHER PAGES ========================
    @GetMapping("/logs")
    public String showLogs(Model model) {
        model.addAttribute("logs", userLogRepository.findAllByOrderByTimestampDesc());
        return "admin/logs";
    }

    @GetMapping("/schedules")
    public String listSchedules(Model model) {
        model.addAttribute("schedules", scheduleRepository.findAll());
        return "admin/schedule-list";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Booking> allBookings = bookingRepository.findAll();
        List<DepartureSchedule> allSchedules = scheduleRepository.findAll();
        long totalCustomers = userRepository.countCustomers();
        long totalStaffs = userRepository.countStaffs();

        BigDecimal revenue = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID)
                .map(b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingOrders = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .count();

        List<DepartureSchedule> upcomingSchedules = allSchedules.stream()
                .filter(s -> s.getStartDate() != null && s.getStartDate().isAfter(LocalDate.now()))
                .sorted(Comparator.comparing(DepartureSchedule::getStartDate))
                .limit(5)
                .collect(Collectors.toList());

        List<DepartureSchedule> hotSchedules = allSchedules.stream()
                .filter(s -> s.getStartDate() != null && s.getStartDate().isAfter(LocalDate.now()))
                .filter(s -> {
                    Integer quota = s.getQuota() == null ? 0 : s.getQuota();
                    Integer booked = s.getBooked() == null ? 0 : s.getBooked();
                    if (quota == 0) return false;
                    BigDecimal ratio = new BigDecimal(booked).divide(new BigDecimal(quota), 4, RoundingMode.HALF_UP);
                    return ratio.compareTo(new BigDecimal("0.8")) > 0;
                })
                .limit(5)
                .collect(Collectors.toList());

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        Long newBookingsToday = bookingRepository.countByBookingDateAfter(startOfToday);

        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartData = new ArrayList<>();

        LocalDate today = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            chartLabels.add("T" + monthStart.getMonthValue());

            BigDecimal monthlyRevenue = allBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.PAID)
                    .filter(b -> {
                        if (b.getBookingDate() == null) return false;
                        LocalDate d = b.getBookingDate().toLocalDate();
                        return !d.isBefore(monthStart) && !d.isAfter(monthEnd);
                    })
                    .map(b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            chartData.add(monthlyRevenue);
        }

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);
        model.addAttribute("newBookingsToday", newBookingsToday);
        model.addAttribute("totalTours", tourService.findAllTours().size());
        model.addAttribute("revenue", revenue);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("upcomingSchedules", upcomingSchedules);
        model.addAttribute("hotSchedules", hotSchedules);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalStaffs", totalStaffs);
        model.addAttribute("stats", statsService.getDashboardStats());
        model.addAttribute("newRequests", consultationRepo.findByIsProcessedFalseOrderByCreatedDateDesc(PageRequest.of(0, 5)));

        List<Booking> recentBookings = allBookings.stream()
                .sorted(Comparator.comparing(Booking::getBookingDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("recentBookings", recentBookings);

        return "admin/dashboard";
    }

    // CREATE USER FORM
    @GetMapping("/users/create")
    public String showCreateUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("listRoles", roleRepository.findAll());
        return "admin/user-form";
    }

    // SAVE USER (create/update)
    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute("user") User userForm,
                           @RequestParam(value = "roleIds", required = false) List<Integer> roleIds,
                           @RequestParam(value = "newPassword", required = false) String newPassword,
                           Model model) {

        // Validation
        if (userForm.getId() == null) {
            if (userForm.getEmail() == null || !userForm.getEmail().toLowerCase().endsWith("@gmail.com")) {
                model.addAttribute("error", "Lỗi: Email phải thuộc tên miền @gmail.com!");
                model.addAttribute("listRoles", roleRepository.findAll());
                return "admin/user-form";
            }
        }

        String phoneRegex = "^(03|05|07|08|09)\\d{8,9}$";
        if (userForm.getPhone() == null || !userForm.getPhone().matches(phoneRegex)) {
            model.addAttribute("error", "Lỗi: SĐT không hợp lệ! Phải là số VN (đầu 03,05,07,08,09) và có 10-11 số.");
            model.addAttribute("listRoles", roleRepository.findAll());
            if (userForm.getId() != null) {
                User oldUser = userRepository.findById(userForm.getId()).orElse(new User());
                userForm.setEmail(oldUser.getEmail());
            }
            return "admin/user-form";
        }

        // Create new
        if (userForm.getId() == null) {
            if (userRepository.existsByEmail(userForm.getEmail())) {
                model.addAttribute("error", "Email đã tồn tại!");
                model.addAttribute("listRoles", roleRepository.findAll());
                return "admin/user-form";
            }

            userForm.setPassword("{noop}123456");

            if (roleIds != null) {
                userForm.setRoles(new HashSet<>(roleRepository.findAllById(roleIds)));
            } else {
                roleRepository.findByName("STAFF").ifPresent(userForm::addRole);
            }

            userRepository.save(userForm);
            saveLog("QUẢN LÝ USER", "Đã tạo tài khoản mới: " + userForm.getEmail());
        } else {
            User existing = userRepository.findById(userForm.getId()).orElse(userForm);
            existing.setFullName(userForm.getFullName());
            existing.setPhone(userForm.getPhone());
            existing.setLocked(userForm.getLocked());

            if (newPassword != null && !newPassword.trim().isEmpty()) {
                existing.setPassword("{noop}" + newPassword);
            }

            if (roleIds != null) {
                existing.setRoles(new HashSet<>(roleRepository.findAllById(roleIds)));
            } else {
                existing.getRoles().clear();
            }

            userRepository.save(existing);
            saveLog("QUẢN LÝ USER", "Đã cập nhật user: " + existing.getEmail());
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/tours/delete/{id}")
    public String deleteTour(@PathVariable("id") Integer id) {
        tourRepository.deleteById(id);
        saveLog("XÓA TOUR", "Đã xóa tour ID: " + id);
        return "redirect:/admin/tours";
    }

    // SCHEDULE FORM
    @GetMapping("/schedules/add")
    public String showAddScheduleForm(@RequestParam(value = "tourId", required = false) Integer tourId, Model model) {
        DepartureSchedule schedule = new DepartureSchedule();
        if (tourId != null) {
            tourRepository.findById(tourId).ifPresent(schedule::setTour);
        }
        model.addAttribute("schedule", schedule);
        model.addAttribute("tours", tourRepository.findAll());
        return "admin/schedule-form";
    }

    // SAVE SCHEDULE
    @PostMapping("/schedules/save")
    public String saveSchedule(@ModelAttribute("schedule") DepartureSchedule schedule) {
        if (schedule.getId() == null) {
            schedule.setBooked(0);
        }
        scheduleRepository.save(schedule);
        String tourCode = schedule.getTour() != null ? schedule.getTour().getCode() : "N/A";
        saveLog("LỊCH KHỞI HÀNH", "Đã lưu lịch khởi hành ngày " + schedule.getStartDate() + " cho tour " + tourCode);
        return "redirect:/admin/schedules";
    }
}
