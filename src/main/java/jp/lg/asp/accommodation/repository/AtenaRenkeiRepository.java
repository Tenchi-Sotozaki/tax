package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.AtenaRenkei;
import jp.lg.asp.accommodation.entity.AtenaRenkeiId;

@Repository
public interface AtenaRenkeiRepository extends JpaRepository<AtenaRenkei, AtenaRenkeiId> {

    @Query("SELECT COALESCE(MAX(r.seq), 0) FROM AtenaRenkei r WHERE r.jichitaiCd = :jichitaiCd")
    java.math.BigDecimal findMaxSeqByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);

    List<AtenaRenkei> findByJichitaiCdOrderBySeqDesc(String jichitaiCd);
}
