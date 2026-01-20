package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // 1. Lấy danh sách Review của 1 Tour (Chỉ lấy review đã được Duyệt - isApproved=true)
    Page<Review> findByTourIdAndIsApprovedTrueOrderByCreatedAtDesc(Integer tourId, Pageable pageable);

    // 2. (THỐNG KÊ SAO) Tính điểm trung bình rating của 1 Tour (VD: 4.5 sao)
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.tour.id = :tourId AND r.isApproved = true")
    Double getAverageRating(@Param("tourId") Integer tourId);

    // 3. Đếm tổng số đánh giá của 1 tour
    Long countByTourIdAndIsApprovedTrue(Integer tourId);
    List<Review> findByTourId(Integer tourId);
}