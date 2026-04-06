package jp.lg.asp.accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accommodation_tax_declaration")
@Getter @Setter
public class AccommodationTaxDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "declaration_id")
    private Long declarationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id", nullable = false)
    private SpecialCollector collector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private AccommodationFacility facility;

    /** 納入年月 YYYYMM */
    @Column(name = "payment_year_month", nullable = false, length = 6)
    private String paymentYearMonth;

    @Column(name = "total_nights", nullable = false)
    private Integer totalNights = 0;

    @Column(name = "exempt_nights", nullable = false)
    private Integer exemptNights = 0;

    @Column(name = "total_payment_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal totalPaymentAmount = BigDecimal.ZERO;

    /** DRAFT / SUBMITTED / CONFIRMED */
    @Column(name = "status", nullable = false, length = 10)
    private String status = "DRAFT";

    @OneToMany(mappedBy = "declaration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccommodationTaxDeclarationDetail> details = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
