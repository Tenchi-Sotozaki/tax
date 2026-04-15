package jp.lg.asp.accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "accommodation_facility")
@Getter @Setter
public class AccommodationFacility {

    @Id
    @Column(name = "facility_id", length = 20)
    private String facilityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id", nullable = false)
    private SpecialCollector collector;

    @Column(name = "facility_name", nullable = false, length = 200)
    private String facilityName;

    @Column(length = 500)
    private String address;

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
