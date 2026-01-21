package fit.se.springdatathymleafshopping.dtos;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class PassengerDTO {
    // Constructor và getters matching BookingService usage
    private String fullName;
    private String type; // "ADULT", "CHILD", "INFANT"
    private String gender;
    private LocalDate dob;

}
