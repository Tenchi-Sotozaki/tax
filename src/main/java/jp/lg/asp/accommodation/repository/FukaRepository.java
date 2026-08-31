package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaId;

@Repository
public interface FukaRepository extends JpaRepository<Fuka, FukaId> {

	// 基本の検索（全件表示用）
	@Query("""
			SELECT f FROM Fuka f
			WHERE f.jichitaiCd = :jichitaiCd
			AND f.shiteiNo = :shiteiNo
			AND f.nendo = :nendo
			AND f.newFlg = "1"
			AND f.delFlg = "0"
			ORDER BY f.kibetsu ASC
			""")
	List<Fuka> findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
			String jichitaiCd, String shiteiNo, String nendo);

	@Query("""
			SELECT f FROM Fuka f
			WHERE f.jichitaiCd = :jichitaiCd
			AND f.shiteiNo = :shiteiNo
			AND f.taishoYm = :taishoYm
			AND f.newFlg = '1'
			AND f.delFlg = '0'
			ORDER BY f.kibetsu ASC
			""")
	List<Fuka> findByJichitaiCdAndShiteiNoAndTaishoYmOrderByKibetsuAsc(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo,
			@Param("taishoYm") String taishoYm);

	@Query("SELECT f FROM Fuka f WHERE f.jichitaiCd = :jichitaiCd AND f.shiteiNo = :shiteiNo AND f.nendo = :nendo AND f.kibetsu = :kibetsu AND f.newFlg = '1' AND f.delFlg = '0' ORDER BY f.rno DESC")
	List<Fuka> findByJichitaiCdAndShiteiNoAndNendoAndKibetsu(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo,
			@Param("nendo") String nendo,
			@Param("kibetsu") Integer kibetsu);

	// 対象年度の賦課情報を取得
	List<Fuka> findByJichitaiCdAndNendoAndNewFlgAndDelFlg(
			String jichitaiCd, String nendo, String newFlg, String delFlg);

	List<Fuka> findByJichitaiCdAndShiteiNoAndNendoAndKibetsuAndNewFlgAndDelFlg(
			String jichitaiCd, String shiteiNo, String nendo, Integer kibetsu, String newFlg, String delFlg);

	@Query("SELECT f FROM Fuka f WHERE f.jichitaiCd = :jichitaiCd AND f.shiteiNo = :shiteiNo ORDER BY f.shinkokuYmd DESC, f.rno DESC")
	List<Fuka> findByJichitaiCdAndShiteiNo(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo);

	@Query("SELECT f FROM Fuka f WHERE f.jichitaiCd = :jichitaiCd AND f.shiteiNo = :shiteiNo AND f.nendo = :nendo AND f.kibetsu = :kibetsu AND f.newFlg = '1' AND f.delFlg = '0' ORDER BY f.rno DESC")
	List<Fuka> findLatestByNendoAndKibetsu(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo,
			@Param("nendo") String nendo,
			@Param("kibetsu") Integer kibetsu);

	@Query("SELECT MAX(f.rno) FROM Fuka f WHERE f.jichitaiCd = :jichitaiCd AND f.shiteiNo = :shiteiNo AND f.nendo = :nendo AND f.kibetsu = :kibetsu")
	Optional<Integer> findMaxRno(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo,
			@Param("nendo") String nendo,
			@Param("kibetsu") Integer kibetsu);

	@Query("SELECT MIN(f.rno) FROM Fuka f WHERE f.jichitaiCd = :jichitaiCd AND f.shiteiNo = :shiteiNo AND f.nendo = :nendo AND f.kibetsu = :kibetsu AND f.delFlg = '0'")
	Optional<Integer> findMinRno(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo,
			@Param("nendo") String nendo,
			@Param("kibetsu") Integer kibetsu);

	@Query("SELECT f FROM Fuka f WHERE f.jichitaiCd = :jichitaiCd AND f.shiteiNo = :shiteiNo AND f.nendo = :nendo AND f.kibetsu = :kibetsu AND f.rno = :rno AND f.delFlg = '0'")
	Optional<Fuka> findByRno(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo,
			@Param("nendo") String nendo,
			@Param("kibetsu") Integer kibetsu,
			@Param("rno") Integer rno);

	/**
	 * 指定年度の賦課情報を取得（一括算出用）
	 */
	@Query("""
			SELECT f FROM Fuka f
			WHERE f.jichitaiCd = :jichitaiCd
			AND f.shiteiNo = :shiteiNo
			AND f.nendo = :nendo
			AND f.delFlg = :delFlg
			AND f.newFlg = :newFlg
			""")
	List<Fuka> findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo,
			@Param("nendo") String nendo,
			@Param("delFlg") String delFlg,
			@Param("newFlg") String newFlg);
	/**
	 * 指定番号と年度で最新のレコードを取得（自治体コード付き）
	 */
	Optional<Fuka> findTopByJichitaiCdAndShiteiNoAndNendoAndNewFlgAndDelFlgOrderByRnoDesc(
			String jichitaiCd, String shiteiNo, String nendo, String newFlg, String delFlg);

	/**
	 * 指定番号の taisho_ym 一覧を重複なし星順で取得
	 */
	@Query("""
			SELECT DISTINCT f.taishoYm FROM Fuka f
			WHERE f.jichitaiCd = :jichitaiCd
			AND f.shiteiNo = :shiteiNo
			AND f.newFlg = '1'
			AND f.delFlg = '0'
			AND f.taishoYm IS NOT NULL
			ORDER BY f.taishoYm ASC
			""")
	List<String> findTaishoYmListByJichitaiCdAndShiteiNo(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo);

	/**
	 * 指定番号ごとの申告済みレコードを、申告日の新しい順で一括取得する。
	 * 呼び出し側で指定番号ごとの先頭行を採用することで「最終申告」を得る。
	 */
	@Query("""
			SELECT f FROM Fuka f
			WHERE f.jichitaiCd = :jichitaiCd
			AND f.shiteiNo IN :shiteiNos
			AND f.newFlg = '1' AND f.delFlg = '0'
			AND f.shinkokuYmd IS NOT NULL
			ORDER BY f.shiteiNo, f.shinkokuYmd DESC, f.rno DESC
			""")
	List<Fuka> findDeclaredByShiteiNoInOrderByShinkokuYmdDesc(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNos") List<String> shiteiNos);
}
