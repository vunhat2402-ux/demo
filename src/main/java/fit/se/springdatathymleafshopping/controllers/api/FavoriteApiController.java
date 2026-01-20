package fit.se.springdatathymleafshopping.controllers.api;

import fit.se.springdatathymleafshopping.entities.Favorite;
import fit.se.springdatathymleafshopping.entities.Tour;
import fit.se.springdatathymleafshopping.entities.User;
import fit.se.springdatathymleafshopping.repositories.FavoriteRepository;
import fit.se.springdatathymleafshopping.repositories.TourRepository;
import fit.se.springdatathymleafshopping.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
@CrossOrigin(origins = "*")
public class FavoriteApiController {

    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TourRepository tourRepository;

    // 1. Toggle Like (Thả tim / Bỏ tim) - Dùng Principal để bảo mật
    @PostMapping("/toggle")
    public ResponseEntity<?> toggleFavorite(@RequestBody Map<String, Integer> payload, Principal principal) {
        // Kiểm tra đăng nhập
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập"));
        }

        Integer tourId = payload.get("tourId");

        // Lấy User từ Session (An toàn hơn lấy từ payload)
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        Tour tour = tourRepository.findById(tourId).orElseThrow(() -> new RuntimeException("Tour not found"));

        // Kiểm tra đã thích chưa
        boolean exists = favoriteRepository.existsByUserIdAndTourId(user.getId(), tourId);

        if (exists) {
            // Nếu có rồi -> Xóa (Unlike)
            // Tìm bản ghi để xóa
            Favorite fav = favoriteRepository.findByUserId(user.getId()).stream()
                    .filter(f -> f.getTour().getId().equals(tourId))
                    .findFirst()
                    .orElseThrow();

            favoriteRepository.delete(fav);
            return ResponseEntity.ok(Map.of("status", "removed", "message", "Đã bỏ thích"));
        } else {
            // Chưa có -> Thêm mới (Like)
            Favorite fav = new Favorite();
            fav.setUser(user);
            fav.setTour(tour);
            fav.setLikedAt(LocalDateTime.now());
            favoriteRepository.save(fav);
            return ResponseEntity.ok(Map.of("status", "added", "message", "Đã thêm vào yêu thích"));
        }
    }
}