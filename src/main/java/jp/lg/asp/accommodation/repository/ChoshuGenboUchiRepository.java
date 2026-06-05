package jp.lg.asp.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ChoshuGenboUchi;
import jp.lg.asp.accommodation.entity.ChoshuGenboUchiId;

@Repository
public interface ChoshuGenboUchiRepository extends JpaRepository<ChoshuGenboUchi, ChoshuGenboUchiId> {
}
