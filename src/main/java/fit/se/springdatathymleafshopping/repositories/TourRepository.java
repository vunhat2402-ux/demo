package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.Tour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TourRepository extends JpaRepository<Tour, Integer> {

    // 1. Tìm theo SEO Slug
    Optional<Tour> findBySlug(String slug);

    boolean existsByCode(String code);

    // Tìm tour theo mã (để phục vụ logic sửa)

    Tour findByCode(String code);

    // 2. Các hàm tìm kiếm cơ bản
    List<Tour> findByIsHotTrueAndIsActiveTrue(); // Cần đảm bảo Tour có field isHot, isActive
    List<Tour> findByIsPromotedTrueAndIsActiveTrue();

    // 3. Tìm kiếm theo từ khóa (Cho Admin)
    @Query("SELECT t FROM Tour t WHERE t.name LIKE %:keyword%")
    Page<Tour> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Vì trong Tour.java bạn khai báo: private String destination; nên dùng hàm này:
    List<Tour> findByDestination_NameContainingIgnoreCase(String destinationName);

    Page<Tour> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // 5. Hàm tìm kiếm nâng cao (Sửa lỗi cú pháp bị thiếu tham số toDate)
    @Query("SELECT DISTINCT t FROM Tour t JOIN t.schedules s " +
            "WHERE s.startDate BETWEEN :fromDate AND :toDate")
    List<Tour> findToursWithDepartureDate(@Param("fromDate") LocalDate fromDate,
                                          @Param("toDate") LocalDate toDate);

    @Query("SELECT DISTINCT t FROM Tour t " +
            "LEFT JOIN t.schedules s " +
            "WHERE (:keyword IS NULL OR t.name LIKE %:keyword% OR t.description LIKE %:keyword%) " +
            "AND (:destinationId IS NULL OR t.destination.id = :destinationId) " +
            "AND (:priceMin IS NULL OR s.priceAdult >= :priceMin) " +
            "AND (:priceMax IS NULL OR s.priceAdult <= :priceMax) " +
            "AND (:startDate IS NULL OR s.startDate >= :startDate)")
    List<Tour> searchToursAdvanced(@Param("keyword") String keyword,
                                   @Param("destinationId") Integer destinationId,
                                   @Param("priceMin") Double priceMin,
                                   @Param("priceMax") Double priceMax,
                                   @Param("startDate") LocalDate startDate);

    Page<Tour> findByDestinationId(Integer destinationId, Pageable pageable);

    Page<Tour> findByCategoryId(Integer categoryId, Pageable pageable);
}