package fit.se.springdatathymleafshopping.services;

import fit.se.springdatathymleafshopping.entities.Tour;
import fit.se.springdatathymleafshopping.repositories.TourRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TourService {

    @Autowired
    private TourRepository tourRepository;

    public List<Tour> findAllTours() {
        return tourRepository.findAll();
    }

    public Optional<Tour> findTourById(Integer id) {
        return tourRepository.findById(id);
    }

    // Nghiệp vụ: Tìm kiếm thông minh cho App Mobile
    public List<Tour> searchTours(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return tourRepository.findAll();
        }
        // Ưu tiên tìm theo địa điểm, nếu không có thì tìm theo tên tour
        List<Tour> byDest = tourRepository.findByDestination_NameContainingIgnoreCase(keyword);
        if (!byDest.isEmpty()) {
            return byDest;
        }
        return tourRepository.findByNameContainingIgnoreCase(keyword, PageRequest.of(0, 10)).getContent();
    }

    public void saveTour(Tour tour) {
        tourRepository.save(tour);
    }

    public void deleteTour(Integer id) {
        tourRepository.deleteById(id);
    }

    public List<Tour> searchToursAdvanced(String keyword, Integer destId, Double minPrice, Double maxPrice, java.time.LocalDate startDate) {
        // Nếu có keyword thì thêm dấu % để tìm kiếm tương đối (LIKE %keyword%)
        // Tuy nhiên, nếu dùng JPQL hiện đại, Spring có thể tự xử lý.
        // Để chắc chắn, ta xử lý thủ công:
        /* String kw = (keyword != null && !keyword.isEmpty()) ? "%" + keyword + "%" : null; */
        // Nhưng ở Repository tôi đã viết logic :keyword IS NULL, nên ta cứ truyền nguyên bản

        return tourRepository.searchToursAdvanced(keyword, destId, minPrice, maxPrice, startDate);
    }
    public Optional<Tour> findTourBySlug(String slug) {
        return tourRepository.findBySlug(slug);
    }
}