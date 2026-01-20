package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.User;
import fit.se.springdatathymleafshopping.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class UserController {

    @Autowired private UserRepository userRepository;
    // @Autowired private PasswordEncoder passwordEncoder; // Nếu dùng mã hóa BCrypt

    // 1. Hiển thị trang hồ sơ
    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        model.addAttribute("user", user);

        return "user-profile"; // Trả về file user-profile.html
    }

    // 2. Xử lý cập nhật thông tin (Form POST)
    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam("fullName") String fullName,
                                @RequestParam("phone") String phone,
                                @RequestParam(value = "newPassword", required = false) String newPassword,
                                Principal principal) {

        if (principal == null) return "redirect:/login";

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user != null) {
            user.setFullName(fullName);
            user.setPhone(phone);

            // Logic đổi mật khẩu (nếu có nhập)
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                // Lưu ý: Giữ nguyên {noop} nếu bạn đang dùng plain text như data.sql cũ
                user.setPassword("{noop}" + newPassword);
            }

            userRepository.save(user);
        }
        return "redirect:/profile?success";
    }
}