package jp.lg.asp.accommodation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.ReportsDefId;

@Repository
public interface ReportsDefRepository extends JpaRepository<ReportsDef, ReportsDefId> {

	/**
	 * 自治体コードと定義テキストで検索
	 */
	Optional<ReportsDef> findByJichitaiCdAndDefText(String jichitaiCd, String defText);
	
	/**
	 * IDと自治体コードで検索
	 */
	Optional<ReportsDef> findByIdAndJichitaiCd(String id, String jichitaiCd);

	/**
	 * 自治体コードで画像ファイルを検索
	 */
	@Query("SELECT r.defData FROM ReportsDef r WHERE r.jichitaiCd = :jichitaiCd")
	byte[] findDefDataByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);
}