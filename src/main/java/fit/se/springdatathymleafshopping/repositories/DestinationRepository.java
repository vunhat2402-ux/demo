package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Integer> {

    // Tìm theo Slug (VD: /dia-diem/da-nang)
    Optional<Destination> findBySlug(String slug);

    // Lấy danh sách Tỉnh/Thành phố trong nước (Type = PROVINCE)
    List<Destination> findByType(String type);

    // Tìm kiếm địa điểm theo tên (Dùng cho ô search autocomplete)
    List<Destination> findByNameContainingIgnoreCase(String name);
}