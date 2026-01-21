package fit.se.springdatathymleafshopping.entities;

import fit.se.springdatathymleafshopping.entities.enums.PassengerType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "booking_passengers")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BookingPassenger {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    private String fullName;
    private LocalDate dob;
    private String gender;

    @Enumerated(EnumType.STRING)
    private PassengerType type;

    private String passportNumber;
    private LocalDate passportExpiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;
}
