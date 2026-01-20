package fit.se.springdatathymleafshopping.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String bookingCode; // Mã đơn: BK-20241020-01

    private LocalDateTime bookingDate;

    // Status: PENDING, DEPOSITED (Đã cọc), PAID (Đã xong), CANCELLED
    private String status;

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // TÀI CHÍNH
    private Double totalAmount;    // Tổng tiền phải trả
    private Double paidAmount;     // Khách đã trả
    private Double discountAmount; // Giảm giá voucher

    private String note;

    // LIÊN KẾT
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Người đặt

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "schedule_id")
    private DepartureSchedule schedule; // Đi ngày nào

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingPassenger> passengers; // Danh sách người đi

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<PaymentTransaction> transactions; // Lịch sử trả tiền

    @Column(name = "payment_method")
    private String paymentMethod;// VNPAY, CASH, TRANSFER

    @PrePersist
    public void prePersist() {
        if (this.bookingDate == null) {
            this.bookingDate = LocalDateTime.now();
        }
    }
}