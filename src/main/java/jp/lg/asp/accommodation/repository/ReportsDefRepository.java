package jp.lg.asp.accommodation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.ReportsDefId;

@Repository
public interface ReportsDefRepository extends JpaRepository<ReportsDef, ReportsDefId> {

	/**
	 * IDと自治体コードで検索
	 */
	Optional<ReportsDef> findByIdAndJichitaiCd(String id, String jichitaiCd);
}