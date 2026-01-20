package fit.se.springdatathymleafshopping.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_logs")
@Data
public class UserLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String action;      // Hành động: SỬA TOUR, DUYỆT ĐƠN...

    @Column(columnDefinition = "TEXT")
    private String description; // Chi tiết hành động

    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Ai làm?

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }
}