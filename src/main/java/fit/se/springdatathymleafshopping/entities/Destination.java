package fit.se.springdatathymleafshopping.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "destinations")
@Data
public class Destination {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name; // VD: Đà Nẵng, Thái Lan
    private String slug; // da-nang (SEO)
    private String image; // Ảnh đại diện địa điểm

    @Column(columnDefinition = "TEXT")
    private String description;

    private String type; // "PROVINCE" (Tỉnh) hoặc "COUNTRY" (Quốc gia)

    @JsonIgnore
    @OneToMany(mappedBy = "destination")
    private List<Tour> tours;
}