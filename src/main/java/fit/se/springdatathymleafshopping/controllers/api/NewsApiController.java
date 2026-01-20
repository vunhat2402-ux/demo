package fit.se.springdatathymleafshopping.controllers.api;

import fit.se.springdatathymleafshopping.entities.News;
import fit.se.springdatathymleafshopping.repositories.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/news")
@CrossOrigin(origins = "*") // Cho phép App gọi thoải mái
public class NewsApiController {

    @Autowired private NewsRepository newsRepository;

    // 1. Lấy danh sách (Nên sắp xếp bài mới nhất lên đầu)
    @GetMapping
    public List<News> getAllNews() {
        // Sort DESC theo id hoặc createdDate để tin mới nhất hiện lên đầu App
        return newsRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    // 2. Lấy chi tiết bài viết (Cho màn hình chi tiết trên App)
    @GetMapping("/{id}")
    public ResponseEntity<News> getNewsDetail(@PathVariable Integer id) {
        return newsRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}