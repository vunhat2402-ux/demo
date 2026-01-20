package fit.se.springdatathymleafshopping.config; // Sửa lại package cho đúng

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Tạo đường dẫn tuyệt đối tới thư mục "uploads" nằm ngay trong project
        Path uploadDir = Paths.get("./uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // Cấu hình: Khi gọi http://localhost:8080/uploads/ten_anh.jpg
        // -> Sẽ tìm trong thư mục uploads của máy tính
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + uploadPath + "/");
    }
}