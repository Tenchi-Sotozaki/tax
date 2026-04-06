package jp.lg.asp.accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "accommodation_tax_declaration_detail")
@Getter @Setter
public class AccommodationTaxDeclarationDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false)
    private AccommodationTaxDeclaration declaration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_category_code", nullable = false)
    private TaxCategory taxCategory;

    /** 課税対象宿泊数 */
    @Column(name = "taxable_nights", nullable = false)
    private Integer taxableNights = 0;

    /** 適用税額スナップショット（マスタ変更の影響を受けない） */
    @Column(name = "tax_amount_per_night", nullable = false, precision = 10, scale = 0)
    private BigDecimal taxAmountPerNight;

    /** 小計 = taxableNights × taxAmountPerNight */
    @Column(name = "subtotal_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    public void calculateSubtotal() {
        this.subtotalAmount = this.taxAmountPerNight
                .multiply(BigDecimal.valueOf(this.taxableNights));
    }
}
