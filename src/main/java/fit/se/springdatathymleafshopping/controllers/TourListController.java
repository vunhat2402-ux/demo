package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.Tour;
import fit.se.springdatathymleafshopping.repositories.TourRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TourListController {

    @Autowired
    private TourRepository tourRepository;

    @GetMapping("/tours")
    public String showTours(Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Integer destinationId,
                            @RequestParam(required = false) Integer categoryId) { // Thêm categoryId

        int pageSize = 9;
        Page<Tour> tourPage;

        // Logic lọc ưu tiên
        if (destinationId != null) {
            tourPage = tourRepository.findByDestinationId(destinationId, PageRequest.of(page, pageSize));
            model.addAttribute("currentFilter", "Điểm đến");
        } else if (categoryId != null) {
            tourPage = tourRepository.findByCategoryId(categoryId, PageRequest.of(page, pageSize));
            model.addAttribute("currentFilter", "Danh mục");
        } else if (keyword != null && !keyword.isEmpty()) {
            tourPage = tourRepository.findByNameContainingIgnoreCase(keyword, PageRequest.of(page, pageSize));
            model.addAttribute("keyword", keyword);
        } else {
            tourPage = tourRepository.findAll(PageRequest.of(page, pageSize));
        }

        model.addAttribute("tourPage", tourPage);
        model.addAttribute("currentPage", page);

        // Truyền lại ID để giữ trạng thái khi phân trang
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("destinationId", destinationId);

        return "tour-list";
    }
}