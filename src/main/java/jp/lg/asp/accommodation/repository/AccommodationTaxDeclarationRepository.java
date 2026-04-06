package jp.lg.asp.accommodation.repository;

import jp.lg.asp.accommodation.entity.AccommodationTaxDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AccommodationTaxDeclarationRepository extends JpaRepository<AccommodationTaxDeclaration, Long> {

    Optional<AccommodationTaxDeclaration> findByCollector_CollectorIdAndFacility_FacilityIdAndPaymentYearMonth(
            String collectorId, String facilityId, String paymentYearMonth);

    /** 明細を一括フェッチ（N+1防止） */
    @Query("""
            SELECT d FROM AccommodationTaxDeclaration d
            LEFT JOIN FETCH d.details det
            LEFT JOIN FETCH det.taxCategory
            WHERE d.declarationId = :id
            """)
    Optional<AccommodationTaxDeclaration> findByIdWithDetails(@Param("id") Long id);
}
