package fit.se.springdatathymleafshopping.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tour_images")
@Data
public class TourImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "tour_id")
    private Tour tour;
}