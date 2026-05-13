package jp.lg.asp.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ChoshuGenboUchi;

@Repository
public interface ChoshuGenboUchiRepository extends JpaRepository<ChoshuGenboUchi, Long> {

	@Query("SELECT COALESCE(MAX(c.uchiIdx), 0) FROM ChoshuGenboUchi c")
	Long getMaxUchiIdx();
}