package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.Review;
import fit.se.springdatathymleafshopping.entities.Tour;
import fit.se.springdatathymleafshopping.entities.User;
import fit.se.springdatathymleafshopping.repositories.ReviewRepository;
import fit.se.springdatathymleafshopping.repositories.TourRepository;
import fit.se.springdatathymleafshopping.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class TourController {

    @Autowired private TourRepository tourRepository;
    @Autowired private ReviewRepository reviewRepository; // Nhớ tạo Repo này nếu chưa có
    @Autowired private UserRepository userRepository;

    // 👇 HÀM XỬ LÝ GỬI ĐÁNH GIÁ (FIX LỖI 405)
    @PostMapping("/tour/comment")
    public String addComment(@RequestParam("tourId") Integer tourId,
                             @RequestParam("content") String content,
                             Principal principal) {
        // 1. Kiểm tra đăng nhập
        if (principal == null) {
            return "redirect:/login";
        }

        // 2. Tìm Tour và User
        Tour tour = tourRepository.findById(tourId).orElseThrow();
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();

        // 3. Tạo Review mới
        Review review = new Review();
        review.setTour(tour);
        review.setUser(user);
        review.setComment(content);
        review.setRating(5);
        review.setCreatedAt(LocalDateTime.now());
        review.setIsApproved(true);

        reviewRepository.save(review);

        // 4. Redirect về trang chi tiết (Dùng Slug hoặc ID)
        return "redirect:/tour/" + (tour.getSlug() != null ? tour.getSlug() : tour.getId());
    }
}