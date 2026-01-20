package fit.se.springdatathymleafshopping.specifications;

import fit.se.springdatathymleafshopping.entities.Tour;
import org.springframework.data.jpa.domain.Specification;

public class TourSpecification {

    // Lọc theo từ khóa (Tên hoặc Mô tả)
    public static Specification<Tour> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isEmpty()) return null;
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern)
            );
        };
    }

    // Lọc theo khoảng giá (Từ min đến max)
    public static Specification<Tour> hasPriceRange(Double minPrice, Double maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null && maxPrice == null) return null;
            if (minPrice != null && maxPrice != null) return criteriaBuilder.between(root.get("price"), minPrice, maxPrice);
            if (minPrice != null) return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
            return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }
}