package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.News;
import fit.se.springdatathymleafshopping.repositories.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/news")
public class NewsController {

    @Autowired
    private NewsRepository newsRepository;

    // --- 1. HIỂN THỊ DANH SÁCH ---
    @GetMapping
    public String listNews(Model model) {
        // Lấy tất cả bài viết, sắp xếp cái mới nhất lên đầu (nếu muốn logic này cần sửa Repository chút, tạm thời lấy all)
        model.addAttribute("newsList", newsRepository.findAll());
        return "admin/news-list";
    }

    // --- 2. FORM THÊM MỚI ---
    @GetMapping("/add")
    public String showAddForm(Model model) {
        News news = new News();
        news.setCreatedDate(LocalDate.now()); // Mặc định ngày tạo là hôm nay
        model.addAttribute("news", news);
        return "admin/news-form";
    }

    // --- 3. FORM CHỈNH SỬA ---
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết ID: " + id));
        model.addAttribute("news", news);
        return "admin/news-form"; // Dùng chung form với thêm mới
    }

    // --- 4. LƯU BÀI VIẾT (XỬ LÝ CẢ THÊM & SỬA) ---
    @PostMapping("/save")
    public String saveNews(@ModelAttribute("news") News news) {
        // Logic: Nếu ngày tạo bị null (do form không gửi lên), tự set lại
        if (news.getCreatedDate() == null) {
            news.setCreatedDate(LocalDate.now());
        }

        // Nếu tiêu đề trống -> có thể validate ở đây
        if (news.getTitle() == null || news.getTitle().isEmpty()) {
            return "redirect:/admin/news/add?error";
        }

        newsRepository.save(news);
        return "redirect:/admin/news";
    }

    // --- 5. XÓA BÀI VIẾT ---
    @GetMapping("/delete/{id}")
    public String deleteNews(@PathVariable("id") Integer id) {
        newsRepository.deleteById(id);
        return "redirect:/admin/news";
    }
}