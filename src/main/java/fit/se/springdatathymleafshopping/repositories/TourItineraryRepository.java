package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.TourItinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourItineraryRepository extends JpaRepository<TourItinerary, Integer> {
    List<TourItinerary> findByTourIdOrderByDayNumberAsc(Integer tourId);

    TourItinerary findByTourIdAndDayNumber(Integer tourId, Integer dayNumber);
}