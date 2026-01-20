package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.TourImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourImageRepository extends JpaRepository<TourImage, Integer> {
    List<TourImage> findByTourId(Integer tourId);
}
