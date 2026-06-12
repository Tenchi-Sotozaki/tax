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

	Optional<Fuka> findByJichitaiCdAndShiteiNoAndKibetsu(String jichitaiCd, String shiteiNo, Integer kibetsu);

	/**
	 * 指定番号と対象年月をキーに、最新の履歴番号（RNO）のデータを取得する
	 * 💡 Derived Query（派生クエリ：メソッド名からSpring Data JPAが自動でSQLを生成する仕組み）
	 */
	Optional<Fuka> findFirstByJichitaiCdAndShiteiNoAndTaishoYmOrderByRnoDesc(
			String jichitaiCd, String shiteiNo, String taishoYm);

	/**
	 * 指定されたキー（自治体・指定番号・年度・期別）の中で最新の（RNOが最大の）レコードを1件取得する
	 * 💡 findFirst...OrderByRnoDesc という命名により、自動的に MAX(RNO) の行が選ばれるぜ。
	 */
	Optional<Fuka> findFirstByJichitaiCdAndShiteiNoAndNendoAndKibetsuOrderByRnoDesc(
			String jichitaiCd, String shiteiNo, String nendo, Integer kibetsu);

	Optional<Fuka> findByJichitaiCdAndShiteiNoAndNendoAndKibetsu(
			String jichitaiCd, String shiteiNo, String nendo, Integer kibetsu);

	// 対象年度の賦課情報を取得
	List<Fuka> findByJichitaiCdAndNendoAndNewFlgAndDelFlg(
			String jichitaiCd, String nendo, String newFlg, String delFlg);

	@Query("SELECT f FROM Fuka f WHERE f.jichitaiCd = :jichitaiCd AND f.shiteiNo = :shiteiNo ORDER BY f.shinkokuYmd DESC, f.rno DESC")
	List<Fuka> findByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);

	@Query("SELECT f FROM Fuka f WHERE f.jichitaiCd = :jichitaiCd AND f.shiteiNo = :shiteiNo AND f.nendo = :nendo AND f.kibetsu = :kibetsu AND f.newFlg = '1' AND f.delFlg = '0' ORDER BY f.rno DESC")
	List<Fuka> findLatestByNendoAndKibetsu(@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo, @Param("nendo") String nendo, @Param("kibetsu") Integer kibetsu);

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

}