package fit.se.springdatathymleafshopping.dtos;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class BookingRequestDTO {
    private Integer scheduleId;
    private Integer userId;

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    private Integer adultCount;
    private Integer childCount;
    private Integer infantCount;

    private String notes;
    private String paymentMethod;
    private String voucherCode;

    // 👇 Sử dụng PassengerDTO từ file riêng
    private List<PassengerDTO> passengers = new ArrayList<>();
}