package fit.se.springdatathymleafshopping.controllers;

import fit.se.springdatathymleafshopping.entities.News;
import fit.se.springdatathymleafshopping.repositories.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NewsClientController {

    @Autowired private NewsRepository newsRepository;

    // Trang danh sách tin
    @GetMapping("/news")
    public String listNews(Model model) {
        model.addAttribute("newsList", newsRepository.findAll());
        return "news-list"; // Bạn cần tạo file templates/news-list.html
    }

    // Trang đọc bài chi tiết
    @GetMapping("/news/{id}")
    public String detailNews(@PathVariable Integer id, Model model) {
        News news = newsRepository.findById(id).orElse(null);
        if (news == null) return "redirect:/news";

        model.addAttribute("news", news);
        return "news-detail"; // Bạn cần tạo file templates/news-detail.html
    }
}