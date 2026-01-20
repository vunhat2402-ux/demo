package fit.se.springdatathymleafshopping.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDTO {
    private String status;  // "OK", "FAILED"
    private String message; // Nội dung thông báo
    private String paymentUrl; // Link thanh toán (nếu có)
}