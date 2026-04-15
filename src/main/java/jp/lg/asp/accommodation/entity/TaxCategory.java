package jp.lg.asp.accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "tax_category")
@Getter @Setter
public class TaxCategory {

    @Id
    @Column(name = "tax_category_code", length = 2)
    private String taxCategoryCode;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 0)
    private BigDecimal taxAmount;
}
