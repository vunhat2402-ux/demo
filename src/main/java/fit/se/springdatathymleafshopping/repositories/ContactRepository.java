package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Integer> {
    // Có thể thêm hàm tìm theo email hoặc trạng thái nếu cần sau này
}