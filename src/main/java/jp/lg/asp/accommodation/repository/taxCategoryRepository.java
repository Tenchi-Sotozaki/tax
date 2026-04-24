package jp.lg.asp.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.lg.asp.accommodation.entity.taxCategory;

public interface taxCategoryRepository extends JpaRepository<taxCategory, String> {
	
}
