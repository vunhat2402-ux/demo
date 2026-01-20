package fit.se.springdatathymleafshopping.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultation_requests")
@Data
public class ConsultationRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String phone;
    private String email;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String tourOfInterest; // Khách đang quan tâm tour nào (nếu có)

    private LocalDateTime createdDate;
    private Boolean isProcessed = false; // Đã có nhân viên gọi chưa
}