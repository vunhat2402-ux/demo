package fit.se.springdatathymleafshopping.controllers.api;

import fit.se.springdatathymleafshopping.entities.DepartureSchedule;
import fit.se.springdatathymleafshopping.entities.Tour;
import fit.se.springdatathymleafshopping.repositories.DepartureScheduleRepository;
import fit.se.springdatathymleafshopping.repositories.TourRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tours")
@CrossOrigin(origins = "*")
public class TourApiController {
    @Autowired private TourRepository tourRepo;
    @Autowired private DepartureScheduleRepository scheduleRepo;

    // Lấy danh sách Tour (Home page)
    @GetMapping
    public Page<Tour> getAllTours(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        // Chỉ lấy tour đang Active
        return tourRepo.findAll(PageRequest.of(page, size, Sort.by("isHot").descending()));
    }

    // API Chi tiết Tour (Quan trọng: Phải kèm theo Lịch khởi hành)
    @GetMapping("/{id}")
    public ResponseEntity<?> getTourDetail(@PathVariable Integer id) {
        Tour tour = tourRepo.findById(id).orElse(null);
        if (tour == null) return ResponseEntity.notFound().build();

        // Lấy các lịch khởi hành sắp tới còn chỗ
        List<DepartureSchedule> schedules = scheduleRepo.findAvailableSchedules(id);

        Map<String, Object> response = new HashMap<>();
        response.put("tour", tour);
        response.put("schedules", schedules); // Frontend dùng cái này render dropdown chọn ngày
        response.put("images", tour.getImages()); // List ảnh gallery
        response.put("itinerary", tour.getItineraries()); // List lịch trình chi tiết

        return ResponseEntity.ok(response);
    }

    // API Tìm kiếm nâng cao (Cập nhật Repo của bạn phải có hàm search)
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String keyword) {
        return ResponseEntity.ok(tourRepo.searchByKeyword(keyword, PageRequest.of(0, 20)));
    }
    @Autowired private fit.se.springdatathymleafshopping.services.TourService tourService; // Nhớ Autowired Service nếu chưa có

    // 👇 THÊM API MỚI NÀY
    // URL Test: /api/v1/tours/advanced-search?keyword=Hà Giang&priceMax=5000000
    @GetMapping("/advanced-search")
    public ResponseEntity<?> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer destinationId,
            @RequestParam(required = false) Double priceMin,
            @RequestParam(required = false) Double priceMax,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate
    ) {
        // Gọi Service
        List<Tour> result = tourService.searchToursAdvanced(keyword, destinationId, priceMin, priceMax, startDate);
        return ResponseEntity.ok(result);
    }
}