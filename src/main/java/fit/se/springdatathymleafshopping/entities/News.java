package fit.se.springdatathymleafshopping.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "news")
@Data
public class News {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;       // Tiêu đề bài viết

    @Column(columnDefinition = "TEXT")
    private String shortDescription; // Mô tả ngắn

    @Column(columnDefinition = "LONGTEXT")
    private String content;     // Nội dung chi tiết

    @Column(columnDefinition = "TEXT")
    private String image;       // Ảnh bìa bài viết
    private LocalDate createdDate;
}