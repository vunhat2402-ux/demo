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

    // --- CÁC SERVICE VÀ REPOSITORY ---
    @Autowired private TourService tourService;
    @Autowired private AdminStatisticsService statsService; // ✅ Service thống kê

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
    @Autowired private ConsultationRequestRepository consultationRepo;

    // --- HELPER: GHI LOG ---
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

    // ======================== DASHBOARD (ĐÃ TỐI ƯU) ========================
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 1. Lấy TOÀN BỘ số liệu thống kê từ Service (Gọn gàng hơn)
        Map<String, Object> stats = statsService.getDashboardStats();

        // Gán dữ liệu vào Model
        model.addAttribute("revenue", stats.get("monthlyRevenue"));
        model.addAttribute("pendingRequests", stats.get("pendingRequests"));
        model.addAttribute("pendingOrders", stats.get("pendingOrders"));  // ✅ Lấy từ Service luôn

        // 2. Các số liệu đếm User/Staff (Vẫn giữ nguyên)
        long totalCustomers = userRepository.countCustomers();
        long totalStaffs = userRepository.countStaffs();

        // 3. Xử lý Lịch trình (Upcoming & Hot)
        List<DepartureSchedule> allSchedules = scheduleRepository.findAll();

        List<DepartureSchedule> upcomingSchedules = allSchedules.stream()
                .filter(s -> s.getStartDate() != null && s.getStartDate().isAfter(LocalDate.now()))
                .sorted(Comparator.comparing(DepartureSchedule::getStartDate))
                .limit(5).collect(Collectors.toList());

        List<DepartureSchedule> hotSchedules = allSchedules.stream()
                .filter(s -> s.getStartDate() != null && s.getStartDate().isAfter(LocalDate.now()))
                .filter(s -> {
                    int quota = (s.getQuota() == null) ? 0 : s.getQuota();
                    int booked = (s.getBooked() == null) ? 0 : s.getBooked();
                    if (quota == 0) return false;
                    BigDecimal ratio = new BigDecimal(booked).divide(new BigDecimal(quota), 4, RoundingMode.HALF_UP);
                    return ratio.compareTo(new BigDecimal("0.8")) > 0;
                }).limit(5).collect(Collectors.toList());

        // 4. Biểu đồ doanh thu 6 tháng gần nhất
        List<Booking> allBookings = bookingRepository.findAll();
        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartData = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            chartLabels.add("T" + monthStart.getMonthValue());

            BigDecimal monthlyRevenue = allBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.PAID && b.getBookingDate() != null)
                    .filter(b -> {
                        LocalDate d = b.getBookingDate().toLocalDate();
                        return !d.isBefore(monthStart) && !d.isAfter(monthEnd);
                    })
                    .map(b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            chartData.add(monthlyRevenue);
        }

        // 5. Đẩy dữ liệu ra View
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);
        model.addAttribute("totalTours", tourService.findAllTours().size());
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalStaffs", totalStaffs);
        model.addAttribute("upcomingSchedules", upcomingSchedules);
        model.addAttribute("hotSchedules", hotSchedules);

        List<Booking> recentBookings = allBookings.stream()
                .sorted(Comparator.comparing(Booking::getBookingDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5).collect(Collectors.toList());
        model.addAttribute("recentBookings", recentBookings);

        model.addAttribute("newRequests", consultationRepo.findByIsProcessedFalseOrderByCreatedDateDesc(PageRequest.of(0, 5)));

        return "admin/dashboard";
    }

    // ======================== VOUCHERS (CODE CHUẨN ĐÃ SỬA) ========================
    @GetMapping("/vouchers")
    public String listVouchers(Model model) {
        model.addAttribute("vouchers", voucherRepository.findAll());
        model.addAttribute("newVoucher", new Voucher());
        model.addAttribute("today", LocalDate.now());
        return "admin/voucher";
    }

    @GetMapping("/vouchers/edit/{id}")
    public String editVoucher(@PathVariable("id") Integer id, Model model) {
        Voucher voucher = voucherRepository.findById(id).orElse(null);
        if (voucher == null) return "redirect:/admin/vouchers";

        model.addAttribute("vouchers", voucherRepository.findAll());
        model.addAttribute("newVoucher", voucher);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("isEditMode", true);
        return "admin/voucher";
    }

    @PostMapping("/vouchers/create")
    public String createOrUpdateVoucher(@ModelAttribute("newVoucher") Voucher voucher,
                                        BindingResult bindingResult, // ✅ BindingResult đúng vị trí
                                        Model model) {
        // 1. Kiểm tra lỗi định dạng (Ngày tháng, Enum...)
        if (bindingResult.hasErrors()) {
            System.out.println(">>> LỖI BINDING: " + bindingResult.getAllErrors());
            model.addAttribute("error", "Dữ liệu không hợp lệ! Vui lòng kiểm tra lại.");
            model.addAttribute("vouchers", voucherRepository.findAll());
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("isEditMode", voucher.getId() != null);
            return "admin/voucher";
        }

        boolean isUpdate = (voucher.getId() != null);

        // 2. Validate logic nghiệp vụ
        if (voucher.getQuantity() == null || voucher.getQuantity() < 1 ||
                voucher.getDiscountValue() == null || voucher.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            return returnVoucherError(model, "Lỗi: Số lượng và Giá trị giảm phải lớn hơn 0!");
        }

        if (voucher.getDiscountType() == DiscountType.PERCENT &&
                voucher.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            return returnVoucherError(model, "Lỗi: Giảm giá phần trăm không được quá 100%!");
        }

        // 3. Validate Trùng Mã
        Voucher existing = voucherRepository.findByCode(voucher.getCode()).orElse(null);
        if (existing != null) {
            if (!isUpdate) { // Tạo mới mà trùng -> Lỗi
                return returnVoucherError(model, "Lỗi: Mã '" + voucher.getCode() + "' đã tồn tại!");
            }
            if (isUpdate && !existing.getId().equals(voucher.getId())) { // Sửa mà trùng mã khác -> Lỗi
                return returnVoucherError(model, "Lỗi: Mã '" + voucher.getCode() + "' đã thuộc về voucher khác!");
            }
        }

        // 4. Lưu và Log
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
        voucherRepository.deleteById(id);
        saveLog("KHUYẾN MÃI", "Xóa voucher ID: " + id);
        return "redirect:/admin/vouchers";
    }

    // ======================== CÁC PHẦN KHÁC (GIỮ NGUYÊN) ========================
    // Các hàm dưới đây giữ nguyên để đảm bảo hệ thống chạy bình thường

    @GetMapping("/profile")
    public String myProfile(Model model) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow();
        model.addAttribute("user", currentUser);
        model.addAttribute("listRoles", roleRepository.findAll());
        return "admin/user-form";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("staffList", userRepository.findByRoles_Name("STAFF"));
        model.addAttribute("customerList", userRepository.findByRoles_Name("USER"));
        return "admin/user-list";
    }

    @GetMapping("/users/create")
    public String showCreateUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("listRoles", roleRepository.findAll());
        return "admin/user-form";
    }

    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute("user") User userForm,
                           @RequestParam(value = "roleIds", required = false) List<Integer> roleIds,
                           @RequestParam(value = "newPassword", required = false) String newPassword,
                           Model model) {
        // [Logic lưu user giữ nguyên như cũ của bạn]
        if (userForm.getId() == null) {
            if (userRepository.existsByEmail(userForm.getEmail())) {
                model.addAttribute("error", "Email đã tồn tại!");
                model.addAttribute("listRoles", roleRepository.findAll());
                return "admin/user-form";
            }
            userForm.setPassword("{noop}123456");
        } else {
            User existing = userRepository.findById(userForm.getId()).orElse(userForm);
            existing.setFullName(userForm.getFullName());
            existing.setPhone(userForm.getPhone());
            existing.setLocked(userForm.getLocked());
            if (newPassword != null && !newPassword.isEmpty()) existing.setPassword("{noop}" + newPassword);
            userForm = existing;
        }

        if (roleIds != null) userForm.setRoles(new HashSet<>(roleRepository.findAllById(roleIds)));
        userRepository.save(userForm);
        saveLog("QUẢN LÝ USER", "Cập nhật/Tạo mới user: " + userForm.getEmail());
        return "redirect:/admin/users";
    }

    @GetMapping("/users/toggle-lock/{id}")
    public String toggleLockUser(@PathVariable("id") Integer id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setLocked(user.getLocked() == null ? true : !user.getLocked());
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Integer id) {
        User user = userRepository.findById(id).orElseThrow();
        userLogRepository.deleteByUserId(id);
        user.getRoles().clear();
        userRepository.delete(user);
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

    // --- TOUR MANAGEMENT ---
    @GetMapping("/tours")
    public String listTours(Model model) {
        model.addAttribute("tours", tourService.findAllTours());
        return "admin/tour-list";
    }

    @GetMapping("/tours/add")
    public String showAddTourForm(Model model) {
        model.addAttribute("tour", new Tour());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("destinations", destinationRepository.findAll());
        return "admin/tour-form";
    }

    @PostMapping("/tours/save")
    public String saveTour(@ModelAttribute("tour") Tour tourForm) {
        tourRepository.save(tourForm);
        return "redirect:/admin/tours";
    }

    @GetMapping("/tours/delete/{id}")
    public String deleteTour(@PathVariable("id") Integer id) {
        tourRepository.deleteById(id);
        return "redirect:/admin/tours";
    }

    @GetMapping("/tours/edit/{id}")
    public String showEditTourForm(@PathVariable("id") Integer id, Model model) {
        Tour tour = tourRepository.findById(id).orElseThrow();
        model.addAttribute("tour", tour);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("destinations", destinationRepository.findAll());
        return "admin/tour-form";
    }

    @GetMapping("/bookings")
    public String listBookings(Model model,
                               @RequestParam(value = "search", required = false) String search) { // Đảm bảo tên biến là "search" khớp với HTML

        List<Booking> bookings;

        if (search != null && !search.trim().isEmpty()) {
            // 1. Log ra để kiểm tra xem Server có nhận được từ khóa không
            System.out.println(">>> ĐANG TÌM KIẾM: " + search.trim());

            // 2. Gọi hàm tìm kiếm mềm dẻo vừa viết ở Bước 1
            bookings = bookingRepository.findByBookingCodeContainingIgnoreCase(search.trim());
        } else {
            // 3. Nếu không tìm thì lấy tất cả và đảo ngược (mới nhất lên đầu)
            bookings = bookingRepository.findAll();
            Collections.reverse(bookings);
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("searchCode", search); // Trả lại từ khóa để hiện trên ô input

        return "admin/booking-list";
    }

    @GetMapping("/bookings/detail/{id}")
    @Transactional(readOnly = true)
    public String viewBookingDetail(@PathVariable("id") Integer id, Model model) {
        Booking booking = bookingRepository.findById(id).orElseThrow();
        booking.getDetails().size(); // Trigger lazy loading
        model.addAttribute("booking", booking);
        return "admin/booking-detail";
    }

    // 👇 Thêm @Transactional để đảm bảo cập nhật cả Booking và Schedule cùng lúc an toàn
    @PostMapping("/bookings/update-status")
    @Transactional
    public String updateBookingStatus(@RequestParam("id") Integer id, @RequestParam("status") String statusStr) {
        Booking booking = bookingRepository.findById(id).orElseThrow();

        BookingStatus oldStatus = booking.getStatus();
        BookingStatus newStatus = BookingStatus.valueOf(statusStr);

        // --- LOGIC HOÀN TRẢ CHỖ (QUOTA) ---
        // Nếu đơn đang tính là "Đã đặt" (PENDING/PAID/DEPOSIT) chuyển sang "Hủy" (CANCELLED/REJECTED/REFUNDED)
        // Thì phải TRỪ số lượng đã đặt trong Schedule
        if (isBookingCounted(oldStatus) && !isBookingCounted(newStatus)) {
            DepartureSchedule schedule = booking.getSchedule();
            if (schedule != null) {
                int currentBooked = schedule.getBooked() == null ? 0 : schedule.getBooked();
                // Số chỗ cần trả = Số lượng khách trong đơn
                int seatsToReturn = booking.getPassengers() == null ? 0 : booking.getPassengers().size();

                // Cập nhật lại số đã đặt (Không để âm)
                schedule.setBooked(Math.max(0, currentBooked - seatsToReturn));
                scheduleRepository.save(schedule);

                saveLog("LỊCH TRÌNH", "Hoàn " + seatsToReturn + " chỗ cho lịch trình ID: " + schedule.getId() + " do hủy đơn " + booking.getBookingCode());
            }
        }

        // (Tùy chọn) Ngược lại: Nếu từ Hủy khôi phục lại thành PENDING/PAID thì phải CỘNG lại chỗ
        // Bạn có thể thêm logic đó ở đây nếu muốn, nhưng thường hủy là hủy luôn.

        // --- CẬP NHẬT TRẠNG THÁI BOOKING ---
        booking.setStatus(newStatus);
        bookingRepository.save(booking);

        saveLog("DUYỆT ĐƠN", "Đổi trạng thái đơn " + booking.getBookingCode() + ": " + oldStatus + " -> " + newStatus);
        return "redirect:/admin/bookings/detail/" + id;
    }

    // Hàm phụ trợ để kiểm tra xem trạng thái này có tính là "chiếm chỗ" không
    private boolean isBookingCounted(BookingStatus status) {
        return status == BookingStatus.PENDING ||
                status == BookingStatus.PAID ||
                status == BookingStatus.DEPOSITED ||
                status == BookingStatus.CANCELLED;
    }

    // --- SCHEDULES & LOGS ---
    @GetMapping("/schedules")
    public String listSchedules(Model model) {
        model.addAttribute("schedules", scheduleRepository.findAll());
        return "admin/schedule-list";
    }

    @GetMapping("/logs")
    public String showLogs(Model model) {
        model.addAttribute("logs", userLogRepository.findAllByOrderByTimestampDesc());
        return "admin/logs";
    }
}