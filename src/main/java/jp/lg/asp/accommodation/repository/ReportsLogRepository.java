package jp.lg.asp.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ReportsLog;
import jp.lg.asp.accommodation.entity.ReportsLogId;

@Repository
public interface ReportsLogRepository extends JpaRepository<ReportsLog, ReportsLogId> {

}
