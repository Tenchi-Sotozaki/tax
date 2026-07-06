package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.OperationLog;
import jp.lg.asp.accommodation.entity.OperationLogId;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, OperationLogId> {

	@Query("SELECT COALESCE(MAX(o.seq), 0) + 1 FROM OperationLog o WHERE o.jichitaiCd = :jichitaiCd")
	Long findNextSeq(@Param("jichitaiCd") String jichitaiCd);

	@Query("""
			SELECT o FROM OperationLog o
			WHERE o.jichitaiCd = :jichitaiCd
			AND (:screenId IS NULL OR o.screenId = :screenId)
			AND (:sousa IS NULL OR o.sousa LIKE CONCAT('%', :sousa, '%'))
			AND (:opeUser IS NULL OR o.opeUser LIKE CONCAT(:opeUser, '%'))
			AND (:opeDtFrom IS NULL OR o.opeDt >= TO_DATE(:opeDtFrom, 'yyyy/MM/dd HH:mm:ss'))
			AND (:opeDtTo IS NULL OR o.opeDt <= TO_DATE(:opeDtTo, 'yyyy/MM/dd HH:mm:ss'))
			AND (:param IS NULL OR o.param LIKE CONCAT('%', :param, '%'))
			ORDER BY o.opeDt DESC
			""")
	List<OperationLog> findByConditions(
			@Param("jichitaiCd") String jichitaiCd,
			@Param("screenId") String screenId,
			@Param("sousa") String sousa,
			@Param("opeUser") String opeUser,
			@Param("opeDtFrom") String opeDtFrom,
			@Param("opeDtTo") String opeDtTo,
			@Param("param") String param);
}
