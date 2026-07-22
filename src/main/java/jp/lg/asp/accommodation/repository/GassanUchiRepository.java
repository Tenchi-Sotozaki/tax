package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.GassanUchiId;

@Repository
public interface GassanUchiRepository extends JpaRepository<GassanUchi, GassanUchiId> {

    @Query("SELECT g FROM GassanUchi g WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo")
    List<GassanUchi> findByJichitaiCdAndGassanShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("gassanShiteiNo") String gassanShiteiNo);

    @Query("SELECT g FROM GassanUchi g WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo IN :gassanShiteiNos")
    List<GassanUchi> findByJichitaiCdAndGassanShiteiNoIn(@Param("jichitaiCd") String jichitaiCd, @Param("gassanShiteiNos") List<String> gassanShiteiNos);

    @Modifying
    @Query("DELETE FROM GassanUchi g WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo")
    void deleteByJichitaiCdAndGassanShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("gassanShiteiNo") String gassanShiteiNo);

    @Query("SELECT g FROM GassanUchi g WHERE g.jichitaiCd = :jichitaiCd AND g.shiteiNo = :shiteiNo")
    List<GassanUchi> findByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);
    
    @Query("SELECT g FROM GassanUchi g JOIN Gassan gs ON g.jichitaiCd = gs.jichitaiCd AND g.gassanShiteiNo = gs.gassanShiteiNo WHERE g.jichitaiCd = :jichitaiCd AND g.shiteiNo IN :shiteiNos AND gs.delFlg = '0'")
    List<GassanUchi> findByJichitaiCdAndShiteiNoIn(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNos") List<String> shiteiNos);
    
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM GassanUchi g WHERE g.jichitaiCd = :jichitaiCd AND g.shiteiNo = :shiteiNo")
    boolean existsByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);
    
    @Modifying
    @Query("DELETE FROM GassanUchi g WHERE g.jichitaiCd = :jichitaiCd AND g.shiteiNo = :shiteiNo")
    void deleteByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);
    
    @Query("SELECT g FROM GassanUchi g WHERE g.jichitaiCd = :jichitaiCd AND (g.shiteiNo = :shiteiNo OR g.gassanShiteiNo = :gassanShiteiNo)")
    List<GassanUchi> findByJichitaiCdAndShiteiNoOrGassanShiteiNo(
        @Param("jichitaiCd") String jichitaiCd,
        @Param("shiteiNo") String shiteiNo,
        @Param("gassanShiteiNo") String gassanShiteiNo
    );
}