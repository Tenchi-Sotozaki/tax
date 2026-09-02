package jp.lg.asp.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Reports;

@Repository
public interface ReportsRepository extends JpaRepository<Reports, String> {

}
