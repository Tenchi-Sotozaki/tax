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

	@Query("SELECT MAX(f.rno) FROM Fuka f WHERE f.jichitaiCd = :jichitaiCd AND f.shiteiNo = :shiteiNo AND f.nendo = :nendo AND f.kibetsu = :kibetsu")
	Optional<Integer> findMaxRno(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo,
			@Param("nendo") String nendo,
			@Param("kibetsu") Integer kibetsu);

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