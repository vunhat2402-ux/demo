package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // 1. Tìm đơn hàng theo Mã Code
    Optional<Booking> findByBookingCode(String bookingCode);

    // 2. Lịch sử đặt tour của User (Phân trang)
    Page<Booking> findByUserIdOrderByBookingDateDesc(Integer userId, Pageable pageable);

    // 3. Lọc đơn hàng theo trạng thái
    Page<Booking> findByStatus(String status, Pageable pageable);

    // 4. Tìm kiếm đơn hàng Admin
    @Query("SELECT b FROM Booking b JOIN b.user u WHERE u.phone LIKE %:keyword% OR u.email LIKE %:keyword%")
    Page<Booking> searchByCustomerInfo(@Param("keyword") String keyword, Pageable pageable);

    // 5. Thống kê doanh thu
    @Query("SELECT SUM(b.totalAmount) FROM Booking b WHERE b.status = 'PAID' AND b.bookingDate BETWEEN :startDate AND :endDate")
    Double calculateRevenue(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 6. Đếm đơn mới trong ngày
    Long countByBookingDateAfter(LocalDateTime date);

    // 7. Check user đã đi tour chưa
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b JOIN b.schedule s WHERE b.user.id = :userId AND s.tour.id = :tourId AND b.status = :status")
    boolean existsByUserIdAndSchedule_Tour_IdAndStatus(@Param("userId") Integer userId, @Param("tourId") Integer tourId, @Param("status") String status);

    // 8. Đếm trạng thái (Fix lỗi Admin Stats)
    long countByStatus(String status);

    // 9. Lấy danh sách booking đơn giản
    List<Booking> findByUserId(Integer userId);

    // 👇 [QUAN TRỌNG] HÀM NÀY FIX LỖI 500 KHI XEM LỊCH SỬ (Load luôn Tour & Schedule)
    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.schedule s " +
            "LEFT JOIN FETCH s.tour t " +
            "WHERE b.user.id = :userId " +
            "ORDER BY b.bookingDate DESC")
    List<Booking> findByUserIdWithScheduleAndTour(@Param("userId") Integer userId);
}