package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ReportsLog;
import jp.lg.asp.accommodation.entity.ReportsLogId;

@Repository
public interface ReportsLogRepository extends JpaRepository<ReportsLog, ReportsLogId> {

	@Query("""
			SELECT r FROM ReportsLog r
			WHERE r.jichitaiCd = :jichitaiCd
			AND (:rptId IS NULL OR :rptId = '' OR r.rptId = :rptId)
			AND (:sousa IS NULL OR :sousa = '' OR r.sousa = :sousa)
			AND (:opeUser IS NULL OR :opeUser = '' OR r.opeUser LIKE CONCAT(:opeUser, '%'))
			AND (:opeDtFrom IS NULL OR :opeDtFrom = '' OR r.opeDt >= TO_DATE(:opeDtFrom, 'yyyy/MM/dd HH:mm:ss'))
			AND (:opeDtTo IS NULL OR :opeDtTo = '' OR r.opeDt <= TO_DATE(:opeDtTo, 'yyyy/MM/dd HH:mm:ss'))
			AND (:shiteiNo IS NULL OR :shiteiNo = '' OR r.shiteiNo = :shiteiNo)
			ORDER BY r.opeDt DESC
			""")
	List<ReportsLog> findByConditions(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("rptId") String rptId,
			@Param("sousa") String sousa,
			@Param("opeUser") String opeUser,
			@Param("opeDtFrom") String opeDtFrom,
			@Param("opeDtTo") String opeDtTo,
			@Param("shiteiNo") String shiteiNo);
}
