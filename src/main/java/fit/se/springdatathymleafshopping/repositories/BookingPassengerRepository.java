package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.BookingPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Integer> {

    // Lấy danh sách người đi của một đơn hàng
    List<BookingPassenger> findByBookingId(Integer bookingId);

    // (Nâng cao) Kiểm tra xem số Passport này đã từng đi tour nào chưa (Check lịch sử khách VIP/Blacklist)
    List<BookingPassenger> findByPassportNumber(String passportNumber);
}