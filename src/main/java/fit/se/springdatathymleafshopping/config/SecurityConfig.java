package fit.se.springdatathymleafshopping.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher; // Import quan trọng cho matcher

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // ======================================================
    // 1. CẤU HÌNH CHO ADMIN (Ưu tiên chạy trước - Order 1)
    // ======================================================
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**") // Chỉ áp dụng cho link bắt đầu bằng /admin/
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login", "/admin/logout", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .failureUrl("/admin/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable()); // Admin nội bộ có thể tắt hoặc bật tùy nhu cầu

        return http.build();
    }

    // ======================================================
    // 2. CẤU HÌNH CHO KHÁCH & API (Chạy sau - Order 2)
    // ======================================================
    @Bean
    @Order(2)
    public SecurityFilterChain userFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CẤU HÌNH CSRF: Chỉ tắt cho API để test POST dễ dàng
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/api/**"))
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/booking/create")) // Nếu booking dùng fetch/ajax
                )

                // 2. PHÂN QUYỀN (AUTHORIZATION)
                .authorizeHttpRequests(auth -> auth
                        // Tài nguyên tĩnh
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/ai/**").permitAll()

                        // Các trang công khai
                        .requestMatchers("/", "/login", "/register", "/tour/**", "/tours/**").permitAll()
                        .requestMatchers("/news/**", "/contact/**", "/about", "/policy").permitAll()

                        // API & Booking (QUAN TRỌNG: Phải permitAll để không bị redirect về login)
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/booking/create").permitAll()

                        // Còn lại bắt buộc đăng nhập
                        .anyRequest().authenticated()
                )

                // 3. LOGIN FORM
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // 4. LOGOUT
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}