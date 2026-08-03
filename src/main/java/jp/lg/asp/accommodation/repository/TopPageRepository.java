package jp.lg.asp.accommodation.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.TopPage;
import jp.lg.asp.accommodation.entity.TopPageId;

@Repository
public interface TopPageRepository extends JpaRepository<TopPage, TopPageId> {

	/**
	 * 掲載期間内の項目を掲載開始日の新しい順で取得する。
	 * 掲載終了日が未設定の場合は終了日なしとして扱う。
	 *
	 * @param jichitaiCd 自治体コード
	 * @param today 基準日
	 * @return 掲載中の項目
	 */
	@Query("""
			SELECT t FROM TopPage t
			WHERE t.jichitaiCd = :jichitaiCd
			AND (t.keisaiStYmd IS NULL OR t.keisaiStYmd <= :today)
			AND (t.keisaiEdYmd IS NULL OR t.keisaiEdYmd >= :today)
			ORDER BY t.keisaiStYmd DESC, t.seq DESC
			""")
	List<TopPage> findPublished(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("today") LocalDate today);

	/**
	 * 登録済みの項目を連番の新しい順で取得する。
	 *
	 * @param jichitaiCd 自治体コード
	 * @return 登録済みの項目
	 */
	List<TopPage> findByJichitaiCdOrderBySeqDesc(String jichitaiCd);

	/**
	 * 採番済みの最大連番を取得する。未登録の場合は0を返す。
	 *
	 * @param jichitaiCd 自治体コード
	 * @return 最大連番
	 */
	@Query("SELECT COALESCE(MAX(t.seq), 0) FROM TopPage t WHERE t.jichitaiCd = :jichitaiCd")
	BigDecimal findMaxSeq(@Param("jichitaiCd") String jichitaiCd);
}
