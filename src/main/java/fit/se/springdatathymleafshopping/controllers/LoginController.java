package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.repositories.RoleRepository;
import fit.se.springdatathymleafshopping.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // 1. Login cho KHÁCH (Link: /login)
    @GetMapping("/login")
    public String showUserLoginForm() {
        return "login"; // templates/login.html
    }

    // 2. Login cho ADMIN (Link: /admin/login)
    @GetMapping("/admin/login")
    public String showAdminLoginForm() {
        return "admin/login"; // templates/admin/login.html
    }


    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String fullName,
                               @RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String phone,
                               @RequestParam String confirmPassword, // Thêm cái này để check
                               Model model) {

        // 1. Kiểm tra mật khẩu nhập lại
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu nhập lại không khớp!");
            return "register";
        }

        // 2. Kiểm tra Email đã tồn tại chưa
        if (userRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "Email này đã được sử dụng!");
            return "register";
        }

        // 3. Tạo User mới
        fit.se.springdatathymleafshopping.entities.User newUser = new fit.se.springdatathymleafshopping.entities.User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPhone(phone);

        // Mã hóa mật khẩu (Thêm {noop} để khớp với SecurityConfig hiện tại của bạn)
        // Hoặc dùng passwordEncoder.encode(password) nếu bạn muốn mã hóa thật
        newUser.setPassword("{noop}" + password);

        newUser.setLocked(false);

        // 4. Gán quyền USER (Role ID = 3 trong data.sql)
        fit.se.springdatathymleafshopping.repositories.RoleRepository roleRepo = roleRepository; // Hoặc dùng @Autowired
        fit.se.springdatathymleafshopping.entities.Role userRole = roleRepository.findByName("USER").orElse(null);

        if (userRole != null) {
            newUser.addRole(userRole);
        }

        userRepository.save(newUser);

        return "redirect:/login?registerSuccess";
    }

    @GetMapping("/403")
    public String accessDenied() { return "403"; }
}