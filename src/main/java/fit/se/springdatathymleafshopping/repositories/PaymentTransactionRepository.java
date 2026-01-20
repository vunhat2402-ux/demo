package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {

    // Tìm giao dịch theo mã tham chiếu (Ví dụ mã trả về từ VNPAY)
    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    // Tìm tất cả giao dịch của một Booking cụ thể
    // (Để tính xem khách đã trả đủ tiền chưa: Cọc + Thanh toán nốt)
    Iterable<PaymentTransaction> findByBookingId(Integer bookingId);
}