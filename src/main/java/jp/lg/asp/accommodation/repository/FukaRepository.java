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

	List<Fuka> findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
			String jichitaiCd, String shiteiNo, String nendo);

	List<Fuka> findByJichitaiCdAndShiteiNoAndNendoAndShinkokuYmdIsNotNullOrderByKibetsuAsc(
			String jichitaiCd, String shiteiNo, String nendo);

	List<Fuka> findByJichitaiCdAndShiteiNoAndNendoAndShinkokuYmdIsNullOrderByKibetsuAsc(
			String jichitaiCd, String shiteiNo, String nendo);

	@Query("SELECT f FROM Fuka f WHERE f.jichitaiCd = :jichitaiCd AND f.shiteiNo = :shiteiNo AND f.newFlg = '1' AND f.delFlg = '0' ORDER BY f.rno DESC")
	List<Fuka> findLatestByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo);

	@Query(value = "SELECT COALESCE(MAX(rno), 0) FROM t_fuka WHERE jichitai_cd = :jichitaiCd AND shitei_no = :shiteiNo", nativeQuery = true)
	Optional<Integer> findMaxRnoByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo);

	@Query("SELECT f FROM Fuka f WHERE f.jichitaiCd = :jichitaiCd AND f.shiteiNo = :shiteiNo AND f.nendo = :nendo AND f.kibetsu = :kibetsu AND f.newFlg = '1' AND f.delFlg = '0' ORDER BY f.rno DESC")
	List<Fuka> findLatestByNendoAndKibetsu(@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo, @Param("nendo") String nendo, @Param("kibetsu") Integer kibetsu);
}