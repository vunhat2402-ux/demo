package fit.se.springdatathymleafshopping.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "departure_schedules")
@Data
public class DepartureSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "price_adult")
    private Double priceAdult;

    @Column(name = "price_child")
    private Double priceChild;

    @Column(name = "price_infant")
    private Double priceInfant;

    @Column(name = "single_supplement")
    private Double singleSupplement;

    private Integer quota;
    private Integer booked = 0;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "tour_id") // Đây chính là "liên kết" mà bạn nhắc tới
    private Tour tour;

    @OneToMany(mappedBy = "schedule")
    private List<Booking> bookings;

    public boolean isAvailable(int totalGuests) {
        if (this.quota == null) this.quota = 0;
        if (this.booked == null) this.booked = 0;
        return (this.quota - this.booked) >= totalGuests;
    }

    public Double getPriceAdult() {
        return priceAdult != null ? priceAdult : 0.0;
    }
}