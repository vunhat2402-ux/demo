package fit.se.springdatathymleafshopping.dtos;

import lombok.AllArgsConstructor; // 👈 Thêm dòng này
import lombok.Data;
import lombok.NoArgsConstructor;  // 👈 Thêm dòng này
import java.time.LocalDate;

@Data
@AllArgsConstructor // 👈 Tạo constructor có tham số: new PassengerDTO(name, type, gender, dob)
@NoArgsConstructor  // 👈 Tạo constructor rỗng: new PassengerDTO() (Cần thiết cho Hibernate/Jackson)
public class PassengerDTO {
    private String fullName;
    private String type;     // "ADULT", "CHILD", "INFANT"
    private String gender;   // "MALE", "FEMALE"
    private LocalDate dob;
}