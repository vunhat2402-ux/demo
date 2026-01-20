package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.UserLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserLogRepository extends JpaRepository<UserLog, Integer> {
    // Lấy log mới nhất xếp lên đầu
    List<UserLog> findAllByOrderByTimestampDesc();

    void deleteByUserId(Integer userId);
}