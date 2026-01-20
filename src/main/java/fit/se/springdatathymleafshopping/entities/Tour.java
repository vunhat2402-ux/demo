package fit.se.springdatathymleafshopping.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tours")
@Data
public class Tour {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String code; // Mã tour: "TO-DN-001"

    @Column(nullable = false)
    private String name;

    private String slug; // URL SEO: "tour-da-nang-3n2d"

    @Column(name = "departure_point")
    private String departurePoint;

    private String image; // Ảnh thumbnail chính

    // --- THÔNG TIN BÁN HÀNG ---
    private String transport; // Máy bay, Xe khách
    private String duration;  // 3 ngày 2 đêm
    private String startLocation; // TP.HCM

    @ManyToOne
    @JoinColumn(name = "destination_id")
    private Destination destination; // Điểm đến chính

    @Column(columnDefinition = "TEXT")
    private String summary;   // Mô tả ngắn
    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả chi tiết

    private Boolean isHot = false;
    private Boolean isPromoted = false; // Có đang khuyến mãi không
    private Boolean isActive = true;    // Còn kinh doanh không

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // 1 Tour có nhiều ảnh (Gallery)
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<TourImage> images;

    // 1 Tour có lịch trình chi tiết (Ngày 1, Ngày 2)
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<TourItinerary> itineraries;

    // 1 Tour có nhiều ngày khởi hành (QUAN TRỌNG NHẤT)
    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<DepartureSchedule> schedules;

    @OneToMany(mappedBy = "tour")
    @JsonIgnore
    private List<Review> reviews;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL) // Thêm cascade để xóa tour thì xóa luôn comment
    @JsonIgnore
    private List<Comment> comments;
}
