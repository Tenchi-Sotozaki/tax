package jp.lg.asp.accommodation.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.entity.TokugimuId;

@Repository
public interface TokugimuRepository extends JpaRepository<Tokugimu, TokugimuId> {

	@Query("""
			SELECT t FROM Tokugimu t
			LEFT JOIN Atena a ON t.jichitaiCd = a.jichitaiCd AND t.atenaNo = a.atenaNo
			WHERE t.newFlg = '1' AND t.delFlg = '0'
			AND (:shiteiNo IS NULL OR :shiteiNo = '' OR t.shiteiNo = :shiteiNo)
			AND (:name IS NULL OR :name = '' OR a.name LIKE %:name%)
			AND (:shisetsuName IS NULL OR :shisetsuName = '' OR t.shisetsuName LIKE %:shisetsuName%)
			AND (:kyokaShu IS NULL OR :kyokaShu = '' OR :kyokaShu = '999' OR t.kyokaShu = :kyokaShu)
			AND (:kojinNo IS NULL OR :kojinNo = '' OR a.kojinNo = :kojinNo)
			AND (:hojinNo IS NULL OR :hojinNo = '' OR a.hojinNo = :hojinNo)
			ORDER BY t.shiteiNo
			""")
	List<Tokugimu> findBySearchConditions(
			@Param("shiteiNo") String shiteiNo,
			@Param("name") String name,
			@Param("shisetsuName") String shisetsuName,
			@Param("kyokaShu") String kyokaShu,
			@Param("kojinNo") String kojinNo,
			@Param("hojinNo") String hojinNo);

	@Query("""
			SELECT t FROM Tokugimu t
			WHERE t.jichitaiCd = :jichitaiCd AND t.atenaNo = :atenaNo
			AND t.newFlg = '1' AND t.delFlg = '0'
			""")
	Optional<Tokugimu> findByJichitaiCdAndAtenaNo(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("atenaNo") BigDecimal atenaNo);

	@Modifying
	@Query("""
			UPDATE Tokugimu t
			SET t.delFlg = '1', t.updDt = CURRENT_TIMESTAMP
			WHERE t.jichitaiCd = :jichitaiCd AND t.atenaNo = :atenaNo
			""")
	void deleteByJichitaiCdAndAtenaNo(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("atenaNo") BigDecimal atenaNo);

	@Query("""
			SELECT t FROM Tokugimu t
			WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo
			AND t.newFlg = '1' AND t.delFlg = '0'
			ORDER BY t.rno DESC
			""")
	Optional<Tokugimu> findByJichitaiCdAndShiteiNo(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNo") String shiteiNo);
}