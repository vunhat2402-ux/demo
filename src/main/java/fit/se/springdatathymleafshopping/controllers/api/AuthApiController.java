package fit.se.springdatathymleafshopping.controllers.api;

import fit.se.springdatathymleafshopping.entities.User;
import fit.se.springdatathymleafshopping.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthApiController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // 1. ĐĂNG NHẬP
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Đăng nhập thành công",
                    "userId", user.getId(),
                    "email", user.getEmail(),
                    // 👇 THÊM 2 DÒNG NÀY
                    "fullName", user.getFullName() != null ? user.getFullName() : "",
                    "phone", user.getPhone() != null ? user.getPhone() : "",
                    "role", user.getRoles().stream().findFirst().get().getName()
            ));
        }
        return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Sai email hoặc mật khẩu"));
    }

    // 2. ĐĂNG KÝ
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        String fullName = payload.get("fullName");

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email đã tồn tại!"));
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(password));
        // Mặc định set quyền USER (Bạn cần inject RoleRepository để lấy Role chuẩn)
        // user.addRole(roleRepository.findByName("USER"));

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công!"));
    }
}