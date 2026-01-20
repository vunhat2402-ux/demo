package fit.se.springdatathymleafshopping.repositories;

import fit.se.springdatathymleafshopping.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
