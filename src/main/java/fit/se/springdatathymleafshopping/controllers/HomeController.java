package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.Tour;
import fit.se.springdatathymleafshopping.repositories.NewsRepository;
import fit.se.springdatathymleafshopping.repositories.ReviewRepository; // Import thêm
import fit.se.springdatathymleafshopping.repositories.TourRepository;
import fit.se.springdatathymleafshopping.services.TourService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @Autowired private TourService tourService;
    @Autowired private TourRepository tourRepository;
    @Autowired private NewsRepository newsRepository;
    @Autowired private ReviewRepository reviewRepository; // Dùng để lấy rating

    @GetMapping("/")
    public String home(Model model, @RequestParam(value = "keyword", required = false) String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("listTours", tourRepository.findByNameContainingIgnoreCase(keyword, PageRequest.of(0, 10)).getContent());
            model.addAttribute("keyword", keyword);
        } else {
            // Mặc định lấy list thường (hoặc bỏ đi nếu đã có Hot/Promo)
            model.addAttribute("listTours", tourService.findAllTours());
        }

        // 👇 TẬN DỤNG TÀI NGUYÊN: Lấy Tour Hot & Promo
        model.addAttribute("hotTours", tourRepository.findByIsHotTrueAndIsActiveTrue());
        model.addAttribute("promoTours", tourRepository.findByIsPromotedTrueAndIsActiveTrue());

        model.addAttribute("newsList", newsRepository.findAll());
        return "home";
    }

    // 👇 NÂNG CẤP: Dùng Slug thay vì ID (SEO Friendly)
    @GetMapping("/tour/{slug}")
    public String viewTourDetail(@PathVariable("slug") String slug, Model model) {
        // Tìm tour theo slug
        Tour tour = tourService.findTourBySlug(slug).orElse(null);

        // Lấy thống kê đánh giá (Tận dụng Repo Review)
        Double avgRating = reviewRepository.getAverageRating(tour.getId());
        Long totalReviews = reviewRepository.countByTourIdAndIsApprovedTrue(tour.getId());

        model.addAttribute("tour", tour);
        model.addAttribute("avgRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        model.addAttribute("totalReviews", totalReviews);

        return "tour-detail";
    }
}