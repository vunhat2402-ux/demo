package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Đăng nhập bằng Email
    Optional<User> findByEmail(String email);

    // Check trùng Email khi đăng ký
    Boolean existsByEmail(String email);

    // Tìm user theo SĐT (Nghiệp vụ CSKH cần tìm nhanh khách)
    Optional<User> findByPhone(String phone);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'USER'")
    long countCustomers();

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'STAFF'")
    long countStaffs();

    // Lấy danh sách user theo tên quyền
    List<User> findByRoles_Name(String roleName);

}
