package jp.lg.asp.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.ChoshuGenboUchi;

@Repository
public interface ChoshuGenboUchiRepository extends JpaRepository<ChoshuGenboUchi, Long> {

    /**
     * 💡 シーケンス発行（ID採番）
     * DB固有の関数を叩くため、nativeQuery = true を指定する
     */
    @Query(value = "SELECT nextval('seq_uchi_idx')", nativeQuery = true)
    Long getNextUchiIdx();
}