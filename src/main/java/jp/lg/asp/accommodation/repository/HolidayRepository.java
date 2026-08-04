package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Kyugyobi;
import jp.lg.asp.accommodation.entity.KyugyobiId;

@Repository
public interface HolidayRepository extends JpaRepository<Kyugyobi, KyugyobiId> {

	List<Kyugyobi> findByJichitaiCdAndNenOrderByKyugyobi(String jichitaiCd, String nen);

	@Modifying
	@Query("DELETE FROM Kyugyobi k WHERE k.jichitaiCd = :jichitaiCd AND k.nen = :nen")
	void deleteByJichitaiCdAndNen(@Param("jichitaiCd") String jichitaiCd, @Param("nen") String nen);

	@Query("SELECT DISTINCT k.nen FROM Kyugyobi k WHERE k.jichitaiCd = :jichitaiCd ORDER BY k.nen")
	List<String> findDistinctNenByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);
}
