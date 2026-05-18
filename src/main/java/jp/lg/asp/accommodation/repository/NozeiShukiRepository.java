package jp.lg.asp.accommodation.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.NozeiShuki;
import jp.lg.asp.accommodation.entity.NozeiShukiId;

@Repository
public interface NozeiShukiRepository extends JpaRepository<NozeiShuki, NozeiShukiId> {

    @Query("SELECT n FROM NozeiShuki n WHERE n.jichitaiCd = :jichitaiCd AND n.delFlg = '0' ORDER BY n.seq")
    List<NozeiShuki> findActiveByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);
    
    @Query("SELECT n FROM NozeiShuki n WHERE n.jichitaiCd = :jichitaiCd AND n.shuki = :shuki AND n.delFlg = '0' ORDER BY n.seq")
    List<NozeiShuki> findActiveByJichitaiCdAndShuki(@Param("jichitaiCd") String jichitaiCd, @Param("shuki") BigDecimal shuki);
    
    @Query("SELECT COUNT(n) FROM NozeiShuki n WHERE n.jichitaiCd = :jichitaiCd AND n.shuki = :shuki AND n.delFlg = '0'")
    long countActiveByJichitaiCdAndShuki(@Param("jichitaiCd") String jichitaiCd, @Param("shuki") BigDecimal shuki);

    @Query("SELECT COUNT(n) FROM NozeiShuki n WHERE n.jichitaiCd = :jichitaiCd AND n.shuki = :shuki AND n.seq <> :seq AND n.delFlg = '0'")
    long countActiveByJichitaiCdAndShukiExcludeSeq(@Param("jichitaiCd") String jichitaiCd, @Param("shuki") BigDecimal shuki, @Param("seq") BigDecimal seq);

    @Query("SELECT COALESCE(MAX(n.seq), 0) + 1 FROM NozeiShuki n WHERE n.jichitaiCd = :jichitaiCd")
    BigDecimal findNextSeq(@Param("jichitaiCd") String jichitaiCd);
}
