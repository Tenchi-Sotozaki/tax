package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.ShoreikinId;

@Repository
public interface ShoreikinRepository extends JpaRepository<Shoreikin, ShoreikinId> {

    @Query("SELECT s FROM Shoreikin s WHERE s.jichitaiCd = :jichitaiCd ORDER BY s.nendo DESC, s.shiteiNo ASC")
    List<Shoreikin> findActiveByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);

    Optional<Shoreikin> findByJichitaiCdAndShiteiNoAndNendo(String jichitaiCd, String shiteiNo, String nendo);
}