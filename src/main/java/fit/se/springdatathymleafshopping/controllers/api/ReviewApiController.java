package fit.se.springdatathymleafshopping.controllers.api;

import fit.se.springdatathymleafshopping.entities.Review;
import fit.se.springdatathymleafshopping.entities.User;
import fit.se.springdatathymleafshopping.entities.enums.BookingStatus;
import fit.se.springdatathymleafshopping.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewApiController {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TourRepository tourRepository;
    @Autowired private DepartureScheduleRepository scheduleRepo;

    // 1. API: VIẾT ĐÁNH GIÁ
    @PostMapping("/add")
    public ResponseEntity<?> addReview(@RequestBody Map<String, Object> payload) {
        try {
            // Lấy dữ liệu từ App gửi lên
            String email = (String) payload.get("email");
            Integer tourId = Integer.parseInt(payload.get("tourId").toString());
            Integer rating = Integer.parseInt(payload.get("rating").toString());
            String comment = (String) payload.get("comment");

            // Tìm User
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return ResponseEntity.badRequest().body("User không tồn tại");

            // CHECK LOGIC: Đã đi tour này chưa?
            boolean hasBooked = bookingRepository.existsByUserIdAndSchedule_Tour_IdAndStatus(
                    user.getId(),
                    tourId,
                    BookingStatus.PAID
            );

            if (!hasBooked) {
                return ResponseEntity.badRequest().body("Bạn chưa hoàn thành tour này!");
            }

            // Lưu đánh giá
            Review review = new Review();
            review.setUser(user);
            review.setTour(tourRepository.findById(tourId).orElse(null));
            review.setRating(rating);
            review.setComment(comment);
            review.setCreatedAt(LocalDateTime.now());

            reviewRepository.save(review);

            return ResponseEntity.ok(Map.of("message", "Đánh giá thành công!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi server: " + e.getMessage());
        }
    }

    // 2. API: XEM ĐÁNH GIÁ CỦA 1 TOUR
    @GetMapping("/tour/{tourId}")
    public ResponseEntity<?> getReviewsByTour(@PathVariable Integer tourId) {
        List<Review> reviews = reviewRepository.findByTourId(tourId);
        return ResponseEntity.ok(reviews);
    }
}