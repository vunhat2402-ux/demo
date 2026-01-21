package fit.se.springdatathymleafshopping.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departure_schedules")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DepartureSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Version
    @Column(nullable = false)
    private Integer version = 0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "price_adult", precision = 15, scale = 2)
    private BigDecimal priceAdult = BigDecimal.ZERO;

    @Column(name = "price_child", precision = 15, scale = 2)
    private BigDecimal priceChild = BigDecimal.ZERO;

    @Column(name = "price_infant", precision = 15, scale = 2)
    private BigDecimal priceInfant = BigDecimal.ZERO;

    @Column(name = "single_supplement", precision = 15, scale = 2)
    private BigDecimal singleSupplement = BigDecimal.ZERO;

    private Integer quota;
    private Integer booked = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @OneToMany(mappedBy = "schedule")
    private List<Booking> bookings = new ArrayList<>();

    public boolean isAvailable(int totalGuests) {
        int q = this.quota == null ? 0 : this.quota;
        int b = this.booked == null ? 0 : this.booked;
        return (q - b) >= totalGuests;
    }

    public BigDecimal getPriceAdult() {
        return priceAdult != null ? priceAdult : BigDecimal.ZERO;
    }
}
