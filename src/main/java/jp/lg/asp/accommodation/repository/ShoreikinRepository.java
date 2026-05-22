package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.ShoreikinId;

@Repository
public interface ShoreikinRepository extends JpaRepository<Shoreikin, ShoreikinId> {

	/**
	 * 検索条件に合致する t_shoreikin を取得する。
	 * t_tokugimu（new_flg='1', del_flg='0'）と LEFT JOIN し、
	 * m_atena と JOIN して氏名・個人番号・法人番号を参照する。
	 */
	@Query("""
			SELECT s FROM Shoreikin s
			LEFT JOIN Tokugimu t
			  ON s.jichitaiCd = t.jichitaiCd
			  AND s.shiteiNo = t.shiteiNo
			  AND t.newFlg = '1' AND t.delFlg = '0'
			LEFT JOIN Atena a
			  ON t.jichitaiCd = a.jichitaiCd
			  AND t.atenaNo = a.atenaNo
			WHERE s.jichitaiCd = :jichitaiCd
			AND (:nendo IS NULL OR :nendo = '' OR s.nendo = :nendo)
			AND (:shiteiNo IS NULL OR :shiteiNo = '' OR s.shiteiNo = :shiteiNo)
			AND (:shisetsuName IS NULL OR :shisetsuName = '' OR t.shisetsuName LIKE %:shisetsuName%)
			AND (:name IS NULL OR :name = '' OR a.name LIKE %:name%)
			AND (:kyokaShu IS NULL OR :kyokaShu = '' OR :kyokaShu = '999' OR t.kyokaShu = :kyokaShu)
			AND (:kojinNo IS NULL OR :kojinNo = '' OR a.kojinNo = :kojinNo)
			AND (:hojinNo IS NULL OR :hojinNo = '' OR a.hojinNo = :hojinNo)
			ORDER BY s.shiteiNo, s.nendo
			""")
	List<Shoreikin> findBySearchConditions(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("nendo") String nendo,
			@Param("shiteiNo") String shiteiNo,
			@Param("shisetsuName") String shisetsuName,
			@Param("name") String name,
			@Param("kyokaShu") String kyokaShu,
			@Param("kojinNo") String kojinNo,
			@Param("hojinNo") String hojinNo);

	Optional<Shoreikin> findByJichitaiCdAndShiteiNoAndNendo(
			String jichitaiCd, String shiteiNo, String nendo);
}
