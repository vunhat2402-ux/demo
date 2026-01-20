package fit.se.springdatathymleafshopping.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "booking_passengers")
@Data
public class BookingPassenger {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String fullName;
    private LocalDate dob; // Ngày sinh (Bắt buộc để tính vé/bảo hiểm)
    private String gender;

    // Loại khách: ADULT, CHILD, INFANT
    private String type;

    // Giấy tờ tùy thân (Cho tour nước ngoài)
    private String passportNumber;
    private LocalDate passportExpiry;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;
}