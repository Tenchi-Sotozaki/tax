package jp.lg.asp.accommodation.repository;

import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.GassanUchiId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GassanUchiRepository extends JpaRepository<GassanUchi, GassanUchiId> {

    @Query("SELECT g FROM GassanUchi g WHERE g.jichitaiCd = :jichitaiCd AND g.shiteiNo = :shiteiNo")
    List<GassanUchi> findByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);
    
    @Query("SELECT g FROM GassanUchi g WHERE g.jichitaiCd = :jichitaiCd AND g.shiteiNo IN :shiteiNos")
    List<GassanUchi> findByJichitaiCdAndShiteiNoIn(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNos") List<String> shiteiNos);
    
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM GassanUchi g WHERE g.jichitaiCd = :jichitaiCd AND g.shiteiNo = :shiteiNo")
    boolean existsByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);
    
    @Modifying
    @Query("DELETE FROM GassanUchi g WHERE g.jichitaiCd = :jichitaiCd AND g.shiteiNo = :shiteiNo")
    void deleteByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);
}