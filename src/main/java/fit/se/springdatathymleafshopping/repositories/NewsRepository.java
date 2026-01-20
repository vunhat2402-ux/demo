package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.News;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<News, Integer> {
}
