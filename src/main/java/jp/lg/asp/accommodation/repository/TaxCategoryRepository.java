package jp.lg.asp.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.lg.asp.accommodation.entity.TaxCategory;

public interface TaxCategoryRepository extends JpaRepository<TaxCategory, String> {
	
}
