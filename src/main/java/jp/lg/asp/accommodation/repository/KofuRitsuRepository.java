package jp.lg.asp.accommodation.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.KofuRitsu;
import jp.lg.asp.accommodation.entity.KofuRitsuId;

@Repository
public interface KofuRitsuRepository extends JpaRepository<KofuRitsu, KofuRitsuId> {

	@Query("SELECT k FROM KofuRitsu k WHERE k.jichitaiCd = :jichitaiCd ORDER BY k.rno DESC")
	List<KofuRitsu> findAllByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);

	@Query("SELECT k FROM KofuRitsu k WHERE k.jichitaiCd = :jichitaiCd AND k.newFlg = 1")
	Optional<KofuRitsu> findCurrentByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);

	@Query("SELECT COALESCE(MAX(k.rno), 0) + 1 FROM KofuRitsu k WHERE k.jichitaiCd = :jichitaiCd")
	BigDecimal findNextRno(@Param("jichitaiCd") String jichitaiCd);
}
