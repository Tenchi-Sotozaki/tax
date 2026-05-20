package jp.lg.asp.accommodation.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.GassanId;

@Repository
public interface GassanRepository extends JpaRepository<Gassan, GassanId> {

    @Query("""
            SELECT g FROM Gassan g
            WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo
            AND g.newFlg = '1' AND g.delFlg = '0'
            ORDER BY g.rno DESC
            """)
    List<Gassan> findByJichitaiCdAndGassanShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo);

    @Query("""
            SELECT g FROM Gassan g
            WHERE g.jichitaiCd = :jichitaiCd AND g.atenaNo = :atenaNo
            AND g.newFlg = '1' AND g.delFlg = '0'
            ORDER BY g.gassanShiteiNo
            """)
    List<Gassan> findByJichitaiCdAndAtenaNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("atenaNo") BigDecimal atenaNo);

    @Query("""
            SELECT g FROM Gassan g
            WHERE g.jichitaiCd = :jichitaiCd
            AND g.newFlg = '1' AND g.delFlg = '0'
            ORDER BY g.gassanShiteiNo
            """)
    List<Gassan> findAllByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);

    @Query(value = "SELECT MAX(CAST(gassan_shitei_no AS INTEGER)) FROM t_gassan WHERE jichitai_cd = :jichitaiCd AND gassan_shitei_no ~ '^[0-9]+$'", nativeQuery = true)
    Optional<Integer> findMaxGassanShiteiNoByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);

    @Modifying
    @Query("""
            UPDATE Gassan g SET g.delFlg = '1'
            WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo
            """)
    void deleteLogicallyByJichitaiCdAndGassanShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo);

    @Query("""
            SELECT g FROM Gassan g
            WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo IN (
                SELECT u.gassanShiteiNo FROM GassanUchi u
                WHERE u.jichitaiCd = :jichitaiCd AND u.shiteiNo = :shiteiNo
            )
            AND g.newFlg = '1' AND g.delFlg = '0'
            ORDER BY g.gassanShiteiNo
            """)
    List<Gassan> findByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);
}
