package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.RptStatus;
import jp.lg.asp.accommodation.entity.RptStatusId;

@Repository
public interface RptStatusRepository extends JpaRepository<RptStatus, RptStatusId> {

	List<RptStatus> findByJichitaiCd(String jichitaiCd);

	Optional<RptStatus> findByJichitaiCdAndShiteiNoAndRptId(String jichitaiCd, String shiteiNo, String rptId);

}
