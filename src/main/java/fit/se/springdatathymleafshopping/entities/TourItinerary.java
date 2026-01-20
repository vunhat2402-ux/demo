package fit.se.springdatathymleafshopping.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tour_itineraries")
@Data
public class TourItinerary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer dayNumber; // Ngày 1, Ngày 2...
    private String title;      // VD: Đón sân bay - City Tour

    @Column(columnDefinition = "TEXT")
    private String description;    // Nội dung chi tiết hoạt động

    private String meals;      // Sáng / Trưa / Tối

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "tour_id")
    private Tour tour;
}