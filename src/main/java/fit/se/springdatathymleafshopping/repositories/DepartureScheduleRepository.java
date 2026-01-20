package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.DepartureSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DepartureScheduleRepository extends JpaRepository<DepartureSchedule, Integer> {

    // 1. Lấy tất cả lịch khởi hành của 1 Tour mà chưa khởi hành (Ngày đi > Hiện tại)
    // Sắp xếp ngày đi gần nhất lên trước
    @Query("SELECT s FROM DepartureSchedule s WHERE s.tour.id = :tourId AND s.startDate >= CURRENT_DATE ORDER BY s.startDate ASC")
    List<DepartureSchedule> findFutureSchedulesByTour(@Param("tourId") Integer tourId);

    // 2. Tìm các lịch khởi hành CÒN CHỖ (booked < quota) của 1 tour
    @Query("SELECT s FROM DepartureSchedule s WHERE s.tour.id = :tourId " +
            "AND s.startDate >= CURRENT_DATE " +
            "AND s.booked < s.quota " +
            "ORDER BY s.startDate ASC")
    List<DepartureSchedule> findAvailableSchedules(@Param("tourId") Integer tourId);

    // 3. Tìm lịch theo khoảng ngày (Để Admin lọc báo cáo xem tháng này có bao nhiêu chuyến đi)
    List<DepartureSchedule> findByStartDateBetween(LocalDate start, LocalDate end);
}