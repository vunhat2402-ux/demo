package fit.se.springdatathymleafshopping.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    // ======================================================
    // 2. CẤU HÌNH CHO KHÁCH (Chạy sau - Order 2)
    // ======================================================
    @Bean
    @Order(2)
    public SecurityFilterChain userFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 1. TÀI NGUYÊN TĨNH & TRANG CHỦ, LOGIN, REGISTER
                        .requestMatchers("/", "/login", "/register", "/tour/**", "/tours/**", "/api/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/ai/**").permitAll()

                        // 2. MỞ KHÓA CHO KHÁCH VÃNG LAI (Tin tức, Liên hệ, Chính sách)
                        .requestMatchers("/news/**").permitAll()
                        .requestMatchers("/contact/**").permitAll()
                        .requestMatchers("/about", "/policy").permitAll()

                        // 3. BOOKING & API
                        .requestMatchers("/booking/create").permitAll()
                        .requestMatchers("/api/v1/bookings").permitAll()

                        // 4. CÁC TRANG CÒN LẠI PHẢI ĐĂNG NHẬP (Profile, History...)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}