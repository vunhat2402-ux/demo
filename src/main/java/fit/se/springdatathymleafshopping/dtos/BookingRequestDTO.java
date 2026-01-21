package fit.se.springdatathymleafshopping.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {
    // Các getter mà BookingService gọi: getScheduleId, getAdultCount, getChildCount, getInfantCount, getPassengers, getUserId, getPaymentMethod, getCustomerName, getCustomerPhone, getCustomerEmail, getNotes
    private Integer scheduleId;

    private Integer adultCount;
    private Integer childCount;
    private Integer infantCount;

    private List<PassengerDTO> passengers;

    private Integer userId;

    @NotBlank
    private String paymentMethod; // "VNPAY", "CASH", ... (service sẽ parse)

    @NotBlank
    private String customerName;

    @Email
    private String customerEmail;

    private String customerPhone;

    private String notes;
}
