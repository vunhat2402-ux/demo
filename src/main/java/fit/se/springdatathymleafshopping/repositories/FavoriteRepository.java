package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {
    List<Favorite> findByUserId(Integer userId);
    boolean existsByUserIdAndTourId(Integer userId, Integer tourId);
}
