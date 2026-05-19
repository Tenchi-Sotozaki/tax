package jp.lg.asp.accommodation.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsu;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsuId;

@Repository
public interface ZeiritsuTeiritsuRepository extends JpaRepository<ZeiritsuTeiritsu, ZeiritsuTeiritsuId> {

	@Query("SELECT t FROM ZeiritsuTeiritsu t WHERE t.jichitaiCd = :jichitaiCd AND t.seq = :seq AND t.delFlg = '0'")
	List<ZeiritsuTeiritsu> findActiveBySeq(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("seq") BigDecimal seq);

	@Query("SELECT t FROM Zeiritsu z INNER JOIN ZeiritsuTeiritsu t ON z.jichitaiCd = t.jichitaiCd AND z.seq = t.seq WHERE z.jichitaiCd = :jichitaiCd AND z.taishoKbn = :taishoKbn AND TO_DATE(z.tekiyoStYm, 'YYYYMM') <= TO_DATE(:tekiyoYm, 'YYYYMM') AND TO_DATE(:tekiyoYm, 'YYYYMM') <= TO_DATE(COALESCE(z.tekiyoEdYm, '999912'), 'YYYYMM') AND z.delFlg = '0' AND t.delFlg = '0'")
	List<ZeiritsuTeiritsu> findActiveByTaishoKbnAndTekiyoYm(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("taishoKbn") String taishoKbn,
			@Param("tekiyoYm") String tekiyoYm);
}
