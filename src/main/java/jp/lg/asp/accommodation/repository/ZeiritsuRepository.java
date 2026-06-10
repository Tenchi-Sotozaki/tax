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

	@Query("SELECT z FROM Zeiritsu z WHERE z.jichitaiCd = :jichitaiCd AND z.delFlg = '0'"
			+ " AND (z.tekiyoStYm <= :targetYm OR z.tekiyoStYm IS NULL)"
			+ " AND (:targetYm <= z.tekiyoEdYm OR z.tekiyoEdYm IS NULL)"
			+ " ORDER BY z.taishoKbn DESC, z.tekiyoStYm DESC")
	List<Zeiritsu> findActiveByJichitaiCdAndTargetYm(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("targetYm") String targetYm);

	// 💡 【追加】Java側で日付判定を行うため、条件に合う親マスタをリストで取得する自動生成メソッド
	List<Zeiritsu> findByJichitaiCdAndFukaKbnAndTaishoKbnAndDelFlgOrderBySeqAsc(
			String jichitaiCd, String fukaKbn, String taishoKbn, String delFlg);

	List<Zeiritsu> findByJichitaiCdAndDelFlgOrderBySeqAsc(String jichitaiCd, String delFlg);

	Optional<Zeiritsu> findFirstByJichitaiCdAndFukaKbnAndDelFlgOrderBySeqAsc(
			String jichitaiCd, String fukaKbn, String delFlg);

	Optional<Zeiritsu> findFirstByJichitaiCdAndFukaKbnAndTaishoKbnAndDelFlgOrderBySeqAsc(
			String jichitaiCd, String fukaKbn, String taishoKbn, String delFlg);
}