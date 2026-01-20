package fit.se.springdatathymleafshopping.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "vouchers")
@Data
public class Voucher {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String code;

    // SỬA: Đổi tên cho khớp với HTML
    private Double discountValue;     // Giá trị giảm (VD: 10.0 hoặc 50000.0)
    private Double discountAmount; // Số tiền giảm (VD: 50000) hoặc %

    private String discountType; // "FIXED" (trừ tiền) hoặc "PERCENT" (trừ %)

    private LocalDate expiryDate; // Ngày hết hạn

    private Boolean isActive = true;
    private Boolean isPercent;        // True = %, False = Tiền mặt

    private Double minOrderAmount;    // Đơn tối thiểu
    private Double maxDiscountAmount; // Giảm tối đa

    // SỬA: Đổi usageLimit -> quantity cho dễ hiểu
    private Integer quantity;
    private Integer usedCount = 0;
}