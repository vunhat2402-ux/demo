package fit.se.springdatathymleafshopping.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
public class PaymentTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Double amount; // Số tiền gd
    private String method; // VNPAY, MOMO, CASH, BANK_TRANSFER
    private String txnRef; // Mã giao dịch ngân hàng

    private LocalDateTime paymentTime;
    private String status; // SUCCESS, FAILED

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;
}