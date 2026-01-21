package fit.se.springdatathymleafshopping.dtos;

import fit.se.springdatathymleafshopping.entities.Booking;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDTO {
    private Integer id;
    private String bookingCode;
    private LocalDateTime bookingDate;
    private String status;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal discountAmount;

    // Mở rộng cho view lịch sử
    private String tourName;
    private String tourDuration;
    private LocalDate startDate;

    public static BookingResponseDTO fromEntity(Booking b) {
        if (b == null) return null;
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(b.getId());
        dto.setBookingCode(b.getBookingCode());
        dto.setBookingDate(b.getBookingDate());
        dto.setStatus(b.getStatus() == null ? null : b.getStatus().name());
        dto.setCustomerName(b.getCustomerName());
        dto.setCustomerEmail(b.getCustomerEmail());
        dto.setCustomerPhone(b.getCustomerPhone());
        dto.setTotalAmount(b.getTotalAmount());
        dto.setPaidAmount(b.getPaidAmount());
        dto.setDiscountAmount(b.getDiscountAmount());

        if (b.getSchedule() != null) {
            dto.setStartDate(b.getSchedule().getStartDate());
            if (b.getSchedule().getTour() != null) {
                dto.setTourName(b.getSchedule().getTour().getName());
                dto.setTourDuration(b.getSchedule().getTour().getDuration());
            }
        }
        return dto;
    }
}
