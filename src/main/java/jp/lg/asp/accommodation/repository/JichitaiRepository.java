package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Jichitai;

@Repository
public interface JichitaiRepository extends JpaRepository<Jichitai, String> {

	@Query("SELECT j FROM Jichitai j WHERE "
			+ "(:jichitaiCd IS NULL OR :jichitaiCd = '' OR j.jichitaiCd = :jichitaiCd) AND "
			+ "(:name IS NULL OR :name = '' OR "
			+ "  (:nameMatchType = 'prefix' AND j.name LIKE CONCAT(:name, '%')) OR "
			+ "  (:nameMatchType = 'partial' AND j.name LIKE CONCAT('%', :name, '%')) OR "
			+ "  (:nameMatchType = 'exact' AND j.name = :name)) AND "
			+ "(:kbnName IS NULL OR :kbnName = '' OR "
			+ "  (:kbnNameMatchType = 'prefix' AND j.kbnName LIKE CONCAT(:kbnName, '%')) OR "
			+ "  (:kbnNameMatchType = 'partial' AND j.kbnName LIKE CONCAT('%', :kbnName, '%')) OR "
			+ "  (:kbnNameMatchType = 'exact' AND j.kbnName = :kbnName)) "
			+ "ORDER BY j.jichitaiCd")
	List<Jichitai> search(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("name") String name,
			@Param("nameMatchType") String nameMatchType,
			@Param("kbnName") String kbnName,
			@Param("kbnNameMatchType") String kbnNameMatchType);

	/**
	 * クエリパラメータ文字列から自治体を取得する。
	 * param に一意制約が無いため、複数件ヒットした場合は先頭の1件を返す。
	 *
	 * @param param クエリパラメータ文字列（m_jichitai.param）
	 * @return 該当する自治体。存在しない場合は empty
	 */
	Optional<Jichitai> findFirstByParam(String param);
}