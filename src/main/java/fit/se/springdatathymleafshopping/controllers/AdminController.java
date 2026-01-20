package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.*;
import fit.se.springdatathymleafshopping.repositories.*;
import fit.se.springdatathymleafshopping.services.AdminStatisticsService;
import fit.se.springdatathymleafshopping.services.TourService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    // 1. THÊM HÀM PROFILE (Để Staff/Admin tự sửa thông tin mình)
    @GetMapping("/profile")
    public String myProfile(Model model) {
        // Lấy email người đang đăng nhập
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow();

        // Tái sử dụng form sửa user có sẵn
        // Lưu ý: Staff vào đây vẫn sửa được, nhưng ở hàm saveUser bạn nên chặn Staff đổi quyền (Role)
        model.addAttribute("user", currentUser);
        model.addAttribute("listRoles", roleRepository.findAll());
        return "admin/user-form";
    }

    // --- HÀM GHI LOG ---
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

    // ======================== QUẢN LÝ USER ========================
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

        // 1. Xóa tất cả Nhật ký hoạt động của user này trước
        // Bạn cần inject UserLogRepository vào AdminController nếu chưa có
        userLogRepository.deleteByUserId(id);

        // 2. Xóa các quyền hạn trong bảng trung gian (users_roles)
        user.getRoles().clear();
        userRepository.save(user);

        // 3. Bây giờ mới xóa User
        userRepository.delete(user);

        saveLog("QUẢN LÝ USER", "Đã xóa vĩnh viễn tài khoản: " + user.getEmail());
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Integer id, Model model) {
        User user = userRepository.findById(id).orElseThrow();

        // 👇 THÊM DÒNG NÀY: Xóa mật khẩu trước khi gửi ra màn hình để bảo mật
        user.setPassword("");

        model.addAttribute("user", user);
        model.addAttribute("listRoles", roleRepository.findAll());
        return "admin/user-form";
    }

    @GetMapping("/users/detail/{id}")
    public String viewUserDetail(@PathVariable("id") Integer id, Model model) {
        User user = userRepository.findById(id).orElseThrow();
        List<Booking> history = bookingRepository.findByUserId(id);
        model.addAttribute("user", user);
        model.addAttribute("history", history);
        return "admin/user-detail";
    }

    // ======================== QUẢN LÝ TOUR ========================
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
        model.addAttribute("tour", new Tour()); // Gửi đối tượng Tour rỗng
        model.addAttribute("categories", categoryRepository.findAll()); // Danh sách danh mục
        model.addAttribute("destinations", destinationRepository.findAll()); // Danh sách điểm đến
        return "admin/tour-form"; // Sử dụng chung template với form Sửa
    }

    @PostMapping("/tours/save")
    public String saveTour(@ModelAttribute("tour") Tour tourForm,
                           @RequestParam(value = "destinationId", required = false) Integer destinationId,
                           @RequestParam(value = "categoryId", required = false) Integer categoryId,
                           Model model) {

        boolean isEdit = tourForm.getId() != null;

        // 1. CHẶN TRÙNG MÃ TOUR (Chỉ chặn khi tạo mới hoặc đổi mã khác)
        if (tourRepository.existsByCode(tourForm.getCode())) {
            Tour existingTour = tourRepository.findByCode(tourForm.getCode());
            // Nếu tạo mới hoàn toàn mà mã đã có, HOẶC sửa tour A nhưng nhập mã của tour B -> BÁO LỖI
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
            tourToSave.setCode(tourForm.getCode().toUpperCase()); // Luôn viết hoa mã tour
            tourToSave.setTransport(tourForm.getTransport());
            tourToSave.setDescription(tourForm.getDescription());
            tourToSave.setDeparturePoint(tourForm.getDeparturePoint()); // Lưu nơi khởi hành
        } else {
            tourToSave = tourForm;
            tourToSave.setCode(tourForm.getCode().toUpperCase());
        }

        if (destinationId != null) tourToSave.setDestination(destinationRepository.findById(destinationId).orElse(null));
        if (categoryId != null) tourToSave.setCategory(categoryRepository.findById(categoryId).orElse(null));

        tourRepository.save(tourToSave);
        saveLog(isEdit ? "CẬP NHẬT TOUR" : "TẠO TOUR MỚI", "Tour: " + tourToSave.getName());
        return "redirect:/admin/tours";
    }

    // ======================== QUẢN LÝ LỊCH TRÌNH ========================
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

        // 1. Chặn số ngày âm hoặc bằng 0
        if (itinerary.getDayNumber() <= 0) {
            // Có thể redirect kèm param error
            return "redirect:/admin/tours/itinerary/" + tourId + "?error=invalid_day";
        }

        // 2. Chặn trùng ngày (Một tour không thể có 2 ngày giống nhau)
        // Logic: Tìm xem tour này đã có ngày này chưa
        TourItinerary existing = tourItineraryRepository.findByTourIdAndDayNumber(tourId, itinerary.getDayNumber());

        // Nếu đã có và ID khác nhau (nghĩa là đang tạo mới hoặc sửa thành ngày đã tồn tại)
        if (existing != null && !existing.getId().equals(itinerary.getId())) {
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

        // Lấy thông tin trước khi xóa để ghi log
        int dayNum = itinerary.getDayNumber();
        String tourCode = itinerary.getTour().getCode();

        tourItineraryRepository.delete(itinerary);

        saveLog("LỊCH TRÌNH", "Xóa lịch trình Ngày " + dayNum + " của tour " + tourCode);
        return "redirect:/admin/tours/itinerary/" + tourId;
    }

    // ======================== QUẢN LÝ HÌNH ẢNH ========================
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

        // Lấy mã tour trước khi xóa
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

    // ======================== QUẢN LÝ ĐƠN HÀNG ========================
    @GetMapping("/bookings")
    public String listBookings(Model model, @RequestParam(value = "search", required = false) String searchCode) {
        if (searchCode != null && !searchCode.trim().isEmpty()) {
            // Dùng method findByBookingCode trong Repository
            // Lưu ý: findByBookingCode trả về Optional, ta chuyển thành List để tái sử dụng view
            Booking booking = bookingRepository.findByBookingCode(searchCode.trim()).orElse(null);
            model.addAttribute("bookings", booking == null ? List.of() : List.of(booking));
            model.addAttribute("searchCode", searchCode);
        } else {
            // Nếu không tìm kiếm thì hiện tất cả (Sắp xếp mới nhất lên đầu)
            List<Booking> list = bookingRepository.findAll();
            // Đảo ngược danh sách thủ công hoặc dùng query orderBy
            Collections.reverse(list);
            model.addAttribute("bookings", list);
        }
        return "admin/booking-list";
    }

    @GetMapping("/bookings/detail/{id}")
    public String viewBookingDetail(@PathVariable("id") Integer id, Model model) {
        Booking booking = bookingRepository.findById(id).orElseThrow();
        model.addAttribute("booking", booking);
        return "admin/booking-detail";
    }

    @PostMapping("/bookings/update-status")
    public String updateBookingStatus(@RequestParam("id") Integer id, @RequestParam("status") String status) {
        Booking booking = bookingRepository.findById(id).orElseThrow();

        // Lưu trạng thái cũ để ghi log
        String oldStatus = booking.getStatus();

        booking.setStatus(status);
        bookingRepository.save(booking);

        saveLog("DUYỆT ĐƠN", "Đổi trạng thái đơn " + booking.getBookingCode() + ": " + oldStatus + " -> " + status);
        return "redirect:/admin/bookings/detail/" + id;
    }

    // ======================== QUẢN LÝ VOUCHER ========================
    @GetMapping("/vouchers")
    public String listVouchers(Model model) {
        model.addAttribute("vouchers", voucherRepository.findAll());
        model.addAttribute("newVoucher", new Voucher());
        model.addAttribute("today", LocalDate.now());
        return "admin/voucher";
    }

    @PostMapping("/vouchers/create")
    public String createVoucher(@ModelAttribute("newVoucher") Voucher voucher, Model model) {

        // --- VALIDATE LOGIC THỰC TẾ ---

        // 1. Kiểm tra số lượng và giá trị giảm không được âm
        if (voucher.getQuantity() < 1 || voucher.getDiscountValue() <= 0) {
            model.addAttribute("error", "Lỗi: Số lượng và Giá trị giảm phải lớn hơn 0!");
            loadVoucherData(model); // Hàm phụ load lại data (xem bên dưới)
            return "admin/voucher";
        }

        // 2. Kiểm tra Logic Phần trăm (Không được quá 100%)
        if (Boolean.TRUE.equals(voucher.getIsPercent()) && voucher.getDiscountValue() > 100) {
            model.addAttribute("error", "Lỗi: Giảm giá theo phần trăm không được vượt quá 100%!");
            loadVoucherData(model);
            return "admin/voucher";
        }

        // 3. Kiểm tra Ngày quá khứ (Code cũ của bạn)
        if (voucher.getExpiryDate().isBefore(LocalDate.now())) {
            model.addAttribute("error", "Lỗi: Hạn sử dụng không được nhỏ hơn ngày hiện tại!");
            loadVoucherData(model);
            return "admin/voucher";
        }

        // 4. Kiểm tra Trùng mã (Code cũ của bạn)
        if (voucherRepository.existsByCode(voucher.getCode())) {
            model.addAttribute("error", "Lỗi: Mã '" + voucher.getCode() + "' đã tồn tại!");
            loadVoucherData(model);
            return "admin/voucher";
        }

        voucherRepository.save(voucher);
        saveLog("KHUYẾN MÃI", "Tạo mã giảm giá mới: " + voucher.getCode());
        return "redirect:/admin/vouchers";
    }

    // Hàm phụ để đỡ viết lặp lại code load data
    private void loadVoucherData(Model model) {
        model.addAttribute("vouchers", voucherRepository.findAll());
        model.addAttribute("today", LocalDate.now());
    }

    @GetMapping("/vouchers/delete/{id}")
    public String deleteVoucher(@PathVariable("id") Integer id) {
        // Tìm voucher để lấy Code trước khi xóa
        Voucher v = voucherRepository.findById(id).orElse(null);
        if (v != null) {
            String code = v.getCode();
            voucherRepository.delete(v);
            saveLog("KHUYẾN MÃI", "Xóa mã giảm giá: " + code);
        }
        return "redirect:/admin/vouchers";
    }

    // ======================== CÁC TRANG KHÁC ========================
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

        double revenue = allBookings.stream().filter(b -> "PAID_FULL".equals(b.getStatus())).mapToDouble(Booking::getTotalAmount).sum();
        long pendingOrders = allBookings.stream().filter(b -> "PENDING".equals(b.getStatus())).count();
        List<DepartureSchedule> upcomingSchedules = allSchedules.stream().filter(s -> s.getStartDate().isAfter(LocalDate.now())).sorted(Comparator.comparing(DepartureSchedule::getStartDate)).limit(5).collect(Collectors.toList());
        List<DepartureSchedule> hotSchedules = allSchedules.stream().filter(s -> s.getStartDate().isAfter(LocalDate.now())).filter(s -> (double) s.getBooked() / s.getQuota() > 0.8).limit(5).collect(Collectors.toList());
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        Long newBookingsToday = bookingRepository.countByBookingDateAfter(startOfToday);
        List<String> chartLabels = new ArrayList<>();
        List<Double> chartData = new ArrayList<>();

        LocalDate today = LocalDate.now();
        // Lặp qua 6 tháng gần nhất (từ 5 tháng trước đến tháng hiện tại)
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

            // Tạo nhãn: "T1", "T2", ...
            chartLabels.add("T" + monthStart.getMonthValue());

            // Tính tổng tiền các đơn hàng ĐÃ THANH TOÁN (PAID_FULL) trong tháng đó
            double monthlyRevenue = allBookings.stream()
                    .filter(b -> "PAID_FULL".equals(b.getStatus())) // Chỉ tính đơn đã trả tiền
                    .filter(b -> !b.getBookingDate().toLocalDate().isBefore(monthStart) &&
                            !b.getBookingDate().toLocalDate().isAfter(monthEnd))
                    .mapToDouble(Booking::getTotalAmount)
                    .sum();

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

        List<Booking> recentBookings = allBookings.stream().sorted(Comparator.comparing(Booking::getBookingDate).reversed()).limit(5).collect(Collectors.toList());
        model.addAttribute("recentBookings", recentBookings);

        return "admin/dashboard";
    }
    // --- 1. HIỂN THỊ FORM TẠO MỚI (Thêm hàm này) ---
    @GetMapping("/users/create")
    public String showCreateUserForm(Model model) {
        model.addAttribute("user", new User()); // Gửi user rỗng sang form
        model.addAttribute("listRoles", roleRepository.findAll()); // Để chọn quyền (Admin/Staff)
        return "admin/user-form";
    }

    // --- 2. NÂNG CẤP HÀM LƯU USER (Thay thế hàm saveUser cũ) ---
    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute("user") User userForm,
                           @RequestParam(value = "roleIds", required = false) List<Integer> roleIds,
                           // 👇 BẠN ĐANG THIẾU DÒNG NÀY 👇
                           @RequestParam(value = "newPassword", required = false) String newPassword,
                           Model model) {

        // --- 1. VALIDATE (Giữ nguyên) ---
        // A. Kiểm tra Email (Phải đuôi @gmail.com)
        if (userForm.getId() == null) {
            if (userForm.getEmail() == null || !userForm.getEmail().toLowerCase().endsWith("@gmail.com")) {
                model.addAttribute("error", "Lỗi: Email phải thuộc tên miền @gmail.com!");
                model.addAttribute("listRoles", roleRepository.findAll());
                return "admin/user-form";
            }
        }

        // B. Kiểm tra Số điện thoại
        String phoneRegex = "^(03|05|07|08|09)\\d{8,9}$";
        if (userForm.getPhone() == null || !userForm.getPhone().matches(phoneRegex)) {
            model.addAttribute("error", "Lỗi: SĐT không hợp lệ! Phải là số VN (đầu 03,05,07,08,09) và có 10-11 số.");
            model.addAttribute("listRoles", roleRepository.findAll());
            // Fix lỗi mất email khi reload form
            if (userForm.getId() != null) {
                User oldUser = userRepository.findById(userForm.getId()).orElse(new User());
                userForm.setEmail(oldUser.getEmail());
            }
            return "admin/user-form";
        }

        // --- 2. XỬ LÝ LƯU ---

        // A. TRƯỜNG HỢP: THÊM MỚI (ID là null)
        if (userForm.getId() == null) {
            if (userRepository.existsByEmail(userForm.getEmail())) {
                model.addAttribute("error", "Email đã tồn tại!");
                model.addAttribute("listRoles", roleRepository.findAll());
                return "admin/user-form";
            }

            userForm.setPassword("{noop}123456"); // Mật khẩu mặc định

            if (roleIds != null) {
                userForm.setRoles(new HashSet<>(roleRepository.findAllById(roleIds)));
            } else {
                roleRepository.findByName("STAFF").ifPresent(userForm::addRole);
            }

            userRepository.save(userForm);
            saveLog("QUẢN LÝ USER", "Đã tạo tài khoản mới: " + userForm.getEmail());
        }

        // B. TRƯỜNG HỢP: CẬP NHẬT (Đã có ID)
        else {
            User existing = userRepository.findById(userForm.getId()).orElse(userForm);
            existing.setFullName(userForm.getFullName());
            existing.setPhone(userForm.getPhone());
            existing.setLocked(userForm.getLocked()); // Cập nhật trạng thái khóa nếu có

            // 👇 LOGIC ĐỔI MẬT KHẨU MỚI (Giờ biến newPassword đã được khai báo ở trên)
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                existing.setPassword("{noop}" + newPassword);
            }

            // Cập nhật quyền
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
        // Kiểm tra xem có đơn hàng (Booking) nào đang dùng tour này không trước khi xóa
        // Nếu có đơn hàng, thực tế nên dùng tour.setActive(false) thay vì xóa vĩnh viễn
        tourRepository.deleteById(id); // ✅ Dùng deleteById thay vì delete(id)
        saveLog("XÓA TOUR", "Đã xóa tour ID: " + id);
        return "redirect:/admin/tours";
    }
    // --- HIỂN THỊ FORM THÊM LỊCH KHỞI HÀNH ---
    @GetMapping("/schedules/add")
    public String showAddScheduleForm(@RequestParam(value = "tourId", required = false) Integer tourId, Model model) {
        DepartureSchedule schedule = new DepartureSchedule();

        // Nếu đi từ nút "Thêm lịch" ở một Tour cụ thể, tự động gán Tour đó vào
        if (tourId != null) {
            tourRepository.findById(tourId).ifPresent(schedule::setTour);
        }

        model.addAttribute("schedule", schedule);
        model.addAttribute("tours", tourRepository.findAll()); // Để chọn Tour trong dropdown
        return "admin/schedule-form"; // Tên file HTML bạn cần tạo ở Bước 2
    }

    // --- LƯU LỊCH KHỞI HÀNH ---
    @PostMapping("/schedules/save")
    public String saveSchedule(@ModelAttribute("schedule") DepartureSchedule schedule) {
        // Ràng buộc thực tế: booked mặc định là 0 khi tạo mới
        if (schedule.getId() == null) {
            schedule.setBooked(0);
        }

        scheduleRepository.save(schedule);

        // Ghi log
        String tourCode = schedule.getTour() != null ? schedule.getTour().getCode() : "N/A";
        saveLog("LỊCH KHỞI HÀNH", "Đã lưu lịch khởi hành ngày " + schedule.getStartDate() + " cho tour " + tourCode);

        return "redirect:/admin/schedules";
    }
}