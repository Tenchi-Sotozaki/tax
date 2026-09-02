package jp.lg.asp.accommodation.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ReportsLog;
import jp.lg.asp.accommodation.entity.ReportsLogId;

@Repository
public interface SofusakiCsvRepository extends JpaRepository<ReportsLog, ReportsLogId> {

    @Query("""
            SELECT r FROM ReportsLog r
            WHERE r.jichitaiCd = :jichitaiCd
            AND r.shiteiNo IS NOT NULL
            AND r.opeDt >= :twoWeeksAgo
            ORDER BY r.opeDt DESC
            """)
    List<ReportsLog> findPrintedLogs(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("twoWeeksAgo") LocalDateTime twoWeeksAgo);

    @Query("""
            SELECT r.rptId, rp.rptName FROM ReportsLog r
            LEFT JOIN Reports rp ON TRIM(rp.rptId) = TRIM(r.rptId)
            WHERE r.jichitaiCd = :jichitaiCd
            AND r.shiteiNo IS NOT NULL
            AND r.opeDt >= :twoWeeksAgo
            ORDER BY r.opeDt DESC
            """)

    List<Object[]> findPrintedLogsWithRptName(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("twoWeeksAgo") LocalDateTime twoWeeksAgo);
}
