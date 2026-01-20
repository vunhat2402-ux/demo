package fit.se.springdatathymleafshopping.controllers.api;

import fit.se.springdatathymleafshopping.entities.Category;
import fit.se.springdatathymleafshopping.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@CrossOrigin(origins = "*")
public class CategoryApiController {

    @Autowired private CategoryRepository categoryRepository;

    @GetMapping
    public List<Category> getAllCategories() {
        // App sẽ gọi API này để lấy danh sách hiển thị lên Menu
        return categoryRepository.findAll();
    }
}