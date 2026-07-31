package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jp.lg.asp.accommodation.entity.ShunoRireki;
import jp.lg.asp.accommodation.entity.ShunoRirekiId;

public interface ShunoRirekiRepository extends JpaRepository<ShunoRireki, ShunoRirekiId> {

	@Query("SELECT s FROM ShunoRireki s WHERE s.jichitaiCd = :jichitaiCd AND s.shiteiNo = :shiteiNo AND s.nendo = :nendo AND s.kibetsu = :kibetsu ORDER BY s.rno DESC LIMIT 1")
	Optional<ShunoRireki> findLatest(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo, @Param("nendo") String nendo, @Param("kibetsu") Integer kibetsu);

	@Query("SELECT MAX(s.rno) FROM ShunoRireki s WHERE s.jichitaiCd = :jichitaiCd AND s.shiteiNo = :shiteiNo AND s.nendo = :nendo AND s.kibetsu = :kibetsu")
	Optional<Integer> findMaxRno(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo, @Param("nendo") String nendo, @Param("kibetsu") Integer kibetsu);

	@Query("SELECT COALESCE(SUM(s.nonyugaku), 0) FROM ShunoRireki s WHERE s.jichitaiCd = :jichitaiCd AND s.shiteiNo = :shiteiNo AND s.nendo = :nendo AND s.kibetsu = :kibetsu")
	long sumNonyugaku(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo, @Param("nendo") String nendo, @Param("kibetsu") Integer kibetsu);

	/**
	 * 指定番号・年度・期別ごとの納入額合計を一括取得する。
	 * 戻り値の各要素は [指定番号, 年度, 期別, 納入額合計]。
	 */
	@Query("""
			SELECT s.shiteiNo, s.nendo, s.kibetsu, COALESCE(SUM(s.nonyugaku), 0)
			FROM ShunoRireki s
			WHERE s.jichitaiCd = :jichitaiCd AND s.shiteiNo IN :shiteiNos
			GROUP BY s.shiteiNo, s.nendo, s.kibetsu
			""")
	List<Object[]> sumNonyugakuByShiteiNoIn(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("shiteiNos") List<String> shiteiNos);
}
