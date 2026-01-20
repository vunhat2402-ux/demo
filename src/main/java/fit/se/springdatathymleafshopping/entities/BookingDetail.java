package fit.se.springdatathymleafshopping.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "booking_details")
@Data
public class BookingDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Logic vé: Tách biệt người lớn và trẻ em
    private Integer adults;
    private Integer children;
    private Double price; // Giá chốt tại thời điểm đặt

    @ManyToOne
    @JoinColumn(name = "booking_id")
    @ToString.Exclude
    @JsonIgnore
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "tour_id")
    @ToString.Exclude
    @JsonIgnore
    private Tour tour;
}