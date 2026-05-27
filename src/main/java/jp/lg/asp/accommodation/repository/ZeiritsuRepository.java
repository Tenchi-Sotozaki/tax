package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Zeiritsu;
import jp.lg.asp.accommodation.entity.ZeiritsuId;

@Repository
public interface ZeiritsuRepository extends JpaRepository<Zeiritsu, ZeiritsuId> {

	@Query("SELECT z FROM Zeiritsu z WHERE z.jichitaiCd = :jichitaiCd AND z.delFlg = '0' ORDER BY z.tekiyoStYm")
	List<Zeiritsu> findActiveByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);

	@Query("SELECT z FROM Zeiritsu z INNER JOIN ZeiritsuTeiritsu t ON z.jichitaiCd = t.jichitaiCd AND z.seq = t.seq WHERE z.jichitaiCd = :jichitaiCd AND z.taishoKbn = :taishoKbn AND TO_DATE(z.tekiyoStYm, 'YYYYMM') <= TO_DATE(:tekiyoYm, 'YYYYMM') AND TO_DATE(:tekiyoYm, 'YYYYMM') <= TO_DATE(COALESCE(z.tekiyoEdYm, '999912'), 'YYYYMM') AND z.delFlg = '0' AND t.delFlg = '0'")
	List<Zeiritsu> findByTaishoKbnAndTekiyoYm(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("taishoKbn") String taishoKbn,
			@Param("tekiyoYm") String tekiyoYm);

	List<Zeiritsu> findByJichitaiCdAndDelFlgOrderBySeqAsc(String jichitaiCd, String delFlg);

	Optional<Zeiritsu> findFirstByJichitaiCdAndFukaKbnAndDelFlgOrderBySeqAsc(
			String jichitaiCd, String fukaKbn, String delFlg);
}