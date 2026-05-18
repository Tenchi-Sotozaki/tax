package jp.lg.asp.accommodation.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.entity.EltaxRenkeiId;

@Repository
public interface EltaxRenkeiRepository extends JpaRepository<EltaxRenkei, EltaxRenkeiId> {

	@Query("SELECT e FROM EltaxRenkei e WHERE e.jichitaiCd = :jichitaiCd ORDER BY e.seq DESC")
	List<EltaxRenkei> findByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);

	@Query("SELECT COALESCE(MAX(e.seq), 0) + 1 FROM EltaxRenkei e WHERE e.jichitaiCd = :jichitaiCd")
	BigDecimal findNextSeq(@Param("jichitaiCd") String jichitaiCd);
}
