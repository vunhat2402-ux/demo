package fit.se.springdatathymleafshopping.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name; // VD: Miền Bắc, Miền Trung, Biển đảo...

    @JsonIgnore
    @OneToMany(mappedBy = "category")
    private List<Tour> tours;
}