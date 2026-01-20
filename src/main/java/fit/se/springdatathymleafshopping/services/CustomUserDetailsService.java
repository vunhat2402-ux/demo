package fit.se.springdatathymleafshopping.services;

import fit.se.springdatathymleafshopping.entities.User;
import fit.se.springdatathymleafshopping.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Tìm user trong DB bằng EMAIL (Dùng hàm vừa sửa ở Bước 1)
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            throw new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email);
        }

        // 2. Chuyển đổi User của mình thành User của Spring Security
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),          // Tên đăng nhập là Email
                user.getPassword(),       // Mật khẩu (đã mã hóa)
                user.getRoles().stream()  // Lấy danh sách quyền (Roles)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())) // Thêm tiền tố ROLE_
                        .collect(Collectors.toList())
        );
    }
}