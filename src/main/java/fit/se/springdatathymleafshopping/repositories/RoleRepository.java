package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    // Hàm tìm quyền theo tên (ví dụ tìm "ADMIN")
    Optional<Role> findByName(String name);
}