package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Integer> {

    // Tìm chi tiết của một mã đặt chỗ cụ thể
    List<BookingDetail> findByBookingId(Integer bookingId);
}