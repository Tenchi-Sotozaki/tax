package jp.lg.asp.accommodation.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ZeiritsuTeigaku;
import jp.lg.asp.accommodation.entity.ZeiritsuTeigakuId;

@Repository
public interface ZeiritsuTeigakuRepository extends JpaRepository<ZeiritsuTeigaku, ZeiritsuTeigakuId> {

	@Query("SELECT t FROM ZeiritsuTeigaku t WHERE t.jichitaiCd = :jichitaiCd AND t.seq = :seq AND t.delFlg = '0' ORDER BY t.ryokinSt")
	List<ZeiritsuTeigaku> findActiveBySeq(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("seq") BigDecimal seq);

	@Query("SELECT t FROM Zeiritsu z INNER JOIN ZeiritsuTeigaku t ON z.jichitaiCd = t.jichitaiCd AND z.seq = t.seq WHERE z.jichitaiCd = :jichitaiCd AND z.taishoKbn = :taishoKbn AND TO_DATE(z.tekiyoStYm, 'YYYYMM') <= TO_DATE(:tekiyoYm, 'YYYYMM') AND TO_DATE(:tekiyoYm, 'YYYYMM') <= TO_DATE(COALESCE(z.tekiyoEdYm, '999912'), 'YYYYMM') AND z.delFlg = '0' AND t.delFlg = '0'")
	List<ZeiritsuTeigaku> findActiveByTaishoKbnAndTekiyoYm(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("taishoKbn") String taishoKbn,
			@Param("tekiyoYm") String tekiyoYm);

	@Query("SELECT t FROM Zeiritsu z INNER JOIN ZeiritsuTeigaku t ON z.jichitaiCd = t.jichitaiCd AND z.seq = t.seq WHERE z.jichitaiCd = :jichitaiCd AND z.taishoKbn = :taishoKbn AND TO_DATE(z.tekiyoStYm, 'YYYYMM') <= TO_DATE(:tekiyoYm, 'YYYYMM') AND TO_DATE(:tekiyoYm, 'YYYYMM') <= TO_DATE(COALESCE(z.tekiyoEdYm, '999912'), 'YYYYMM') AND t.ryokinSt <= :ryokin AND :ryokin <= COALESCE(t.ryokinEd, 9999999999999) AND z.delFlg = '0' AND t.delFlg = '0'")
	Optional<ZeiritsuTeigaku> findActiveByTaishoKbnAndTekiyoYmAndRyokin(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("taishoKbn") String taishoKbn,
			@Param("tekiyoYm") String tekiyoYm,
			@Param("ryokin") Long ryokin);

	// 自治体コードで検索し、料金開始額の昇順（安い順）で取得する
	List<ZeiritsuTeigaku> findByJichitaiCdOrderByRyokinStAsc(String jichitaiCd);

	// 自治体コード + 親マスタseq + 削除フラグで絞り込み、料金開始額の昇順で取得する
	List<ZeiritsuTeigaku> findByJichitaiCdAndSeqAndDelFlgOrderByRyokinStAsc(
			String jichitaiCd, BigDecimal seq, String delFlg);
}
