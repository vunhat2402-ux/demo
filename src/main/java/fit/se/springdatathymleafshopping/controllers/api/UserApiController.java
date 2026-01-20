package fit.se.springdatathymleafshopping.controllers.api;

import fit.se.springdatathymleafshopping.entities.User;
import fit.se.springdatathymleafshopping.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserApiController {

    @Autowired private UserRepository userRepository;

    // Lấy thông tin chi tiết
    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Integer id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        // Trả về JSON, nhớ đừng trả về Password
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail()
                // "phone", user.getPhone() // Nếu có
        ));
    }

    // Cập nhật thông tin
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        if (payload.containsKey("fullName")) user.setFullName(payload.get("fullName"));
        if (payload.containsKey("phone")) user.setPhone(payload.get("phone")); // Thêm dòng này
        if (payload.containsKey("address")) user.setAddress(payload.get("address"));

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Cập nhật thành công"));
    }
}