package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    // Tìm voucher theo mã code và còn hạn sử dụng
    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code); // Thêm hàm này

    // (Nâng cao) Tìm mã còn hiệu lực
    Optional<Voucher> findByCodeAndExpiryDateAfter(String code, LocalDate date);
}