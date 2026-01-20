package fit.se.springdatathymleafshopping.config;

import fit.se.springdatathymleafshopping.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired private CategoryRepository categoryRepository;

    @ModelAttribute("globalCategories") // Biến này sẽ dùng được ở client-layout.html
    public List<?> globalCategories() {
        return categoryRepository.findAll();
    }
}