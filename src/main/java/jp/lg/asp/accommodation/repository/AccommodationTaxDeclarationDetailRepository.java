package jp.lg.asp.accommodation.repository;

import jp.lg.asp.accommodation.entity.AccommodationTaxDeclarationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccommodationTaxDeclarationDetailRepository extends JpaRepository<AccommodationTaxDeclarationDetail, Long> {
    List<AccommodationTaxDeclarationDetail> findByDeclaration_DeclarationId(Long declarationId);
}
