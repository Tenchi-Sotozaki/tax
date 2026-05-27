package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ChoshuGenboUchi;
import jp.lg.asp.accommodation.entity.ChoshuGenboUchiId;

@Repository
public interface ChoshuGenboUchiRepository extends JpaRepository<ChoshuGenboUchi, ChoshuGenboUchiId> {

	@Query("SELECT COALESCE(MAX(c.uchiIdx), 0) FROM ChoshuGenboUchi c")
	Long getMaxUchiIdx();

	List<ChoshuGenboUchi> findByUchiIdxIn(List<Long> uchiIndices);
}