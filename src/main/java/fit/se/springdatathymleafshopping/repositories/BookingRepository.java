package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.Booking;
import fit.se.springdatathymleafshopping.entities.enums.BookingStatus; // 👈 Import Enum
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

    Optional<Booking> findByBookingCode(String bookingCode);

    Page<Booking> findByUserIdOrderByBookingDateDesc(Integer userId, Pageable pageable);

    // 👇 SỬA 1: Đổi String thành BookingStatus
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    @Query("SELECT b FROM Booking b JOIN b.user u WHERE u.phone LIKE %:keyword% OR u.email LIKE %:keyword%")
    Page<Booking> searchByCustomerInfo(@Param("keyword") String keyword, Pageable pageable);

    // 👇 SỬA 2: Đảm bảo @Param khớp với query
    @Query("SELECT SUM(b.totalAmount) FROM Booking b WHERE b.bookingDate BETWEEN :start AND :end AND b.status = 'PAID'")
    Double calculateRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Long countByBookingDateAfter(LocalDateTime date);

    // 👇 SỬA 3: Đổi String thành BookingStatus
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b JOIN b.schedule s WHERE b.user.id = :userId AND s.tour.id = :tourId AND b.status = :status")
    boolean existsByUserIdAndSchedule_Tour_IdAndStatus(@Param("userId") Integer userId,
                                                       @Param("tourId") Integer tourId,
                                                       @Param("status") BookingStatus status);

    // 👇 SỬA 4: QUAN TRỌNG NHẤT (Gây lỗi hiện tại) - Đổi thành BookingStatus
    long countByStatus(BookingStatus status);

    List<Booking> findByUserId(Integer userId);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.schedule s " +
            "LEFT JOIN FETCH s.tour t " +
            "WHERE b.user.id = :userId " +
            "ORDER BY b.bookingDate DESC")
    List<Booking> findByUserIdWithScheduleAndTour(@Param("userId") Integer userId);

    List<Booking> findByBookingCodeContainingIgnoreCase(String bookingCode);
}