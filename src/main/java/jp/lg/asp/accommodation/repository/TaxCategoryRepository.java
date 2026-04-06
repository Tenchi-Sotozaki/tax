package jp.lg.asp.accommodation.repository;

import jp.lg.asp.accommodation.entity.TaxCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxCategoryRepository extends JpaRepository<TaxCategory, String> {
}
