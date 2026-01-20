package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.ConsultationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Integer> {

    // Admin lọc các yêu cầu chưa xử lý
    Page<ConsultationRequest> findByIsProcessedFalseOrderByCreatedDateDesc(Pageable pageable);

    // Tìm kiếm theo SĐT khách hàng
    Page<ConsultationRequest> findByPhoneContaining(String phone, Pageable pageable);

    // 👇 THÊM HÀM NÀY ĐỂ FIX LỖI ADMIN STATS
    long countByIsProcessedFalse();
}