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
import jp.lg.asp.accommodation.entity.GassanUchi;

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
            WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo
            AND g.delFlg = '0'
            ORDER BY g.rno DESC
            """)
    List<Gassan> findAllRnoByJichitaiCdAndGassanShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo);

    @Modifying
    @Query("""
            UPDATE Gassan g SET g.newFlg = '0'
            WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo
            AND g.rno = :rno
            """)
    void clearNewFlgByRno(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo,
            @Param("rno") BigDecimal rno);

    @Modifying
    @Query("""
            UPDATE Gassan g SET g.newFlg = '1'
            WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo
            AND g.rno = :rno
            """)
    void setNewFlgByRno(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo,
            @Param("rno") BigDecimal rno);

    @Modifying
    @Query("""
            UPDATE Gassan g SET g.delFlg = '1', g.newFlg = '0'
            WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo
            AND g.rno = :rno
            """)
    void deleteLogicallyByRno(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo,
            @Param("rno") BigDecimal rno);

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

    @Query(value = "SELECT MAX(CAST(SUBSTRING(gassan_shitei_no, LENGTH(:prefix) + 1) AS INTEGER)) FROM t_gassan WHERE jichitai_cd = :jichitaiCd AND SUBSTRING(gassan_shitei_no, 1, LENGTH(:prefix)) = :prefix AND gassan_shitei_no ~ ('^' || :prefix || '[0-9]+$')", nativeQuery = true)
    Optional<Integer> findMaxGassanShiteiNoByJichitaiCdAndPrefix(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("prefix") String prefix);

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
            
    @Query("""
            SELECT g FROM Gassan g
            WHERE g.gassanShiteiNo = :gassanShiteiNo
            AND g.newFlg = '1' AND g.delFlg = '0'
            ORDER BY g.rno
            """)
    List<Gassan> findByGassanShiteiNoOrderByRno(@Param("gassanShiteiNo") String gassanShiteiNo);

    @Query("""
            SELECT g FROM Gassan g
            WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo
            AND g.rno = :rno AND g.delFlg = '0'
            """)
    Optional<Gassan> findByJichitaiCdAndGassanShiteiNoAndRno(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo,
            @Param("rno") BigDecimal rno);

    @Query("SELECT COALESCE(MAX(g.rno), 0) FROM Gassan g WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo AND g.delFlg = '0'")
    BigDecimal findMaxRnoByJichitaiCdAndGassanShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo);

    @Query("SELECT COUNT(g) FROM Gassan g WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo AND g.delFlg = '0'")
    BigDecimal countValidRnoByJichitaiCdAndGassanShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo);

    @Query("SELECT COUNT(g) FROM Gassan g WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo AND g.rno <= :rno AND g.delFlg = '0'")
    BigDecimal countValidRnoByJichitaiCdAndGassanShiteiNoAndRnoLe(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo,
            @Param("rno") BigDecimal rno);

    @Query("SELECT COALESCE(MAX(g.rno), 0) FROM Gassan g WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo")
    BigDecimal findMaxRnoIncludingDeletedByJichitaiCdAndGassanShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo);

    @Query("SELECT COALESCE(MIN(g.rno), 0) FROM Gassan g WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo AND g.delFlg = '0'")
    BigDecimal findMinRnoByJichitaiCdAndGassanShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo);

    @Query("SELECT COALESCE(MAX(g.rno), 0) FROM Gassan g WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo AND g.rno < :rno AND g.delFlg = '0'")
    BigDecimal findPrevRnoByJichitaiCdAndGassanShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo,
            @Param("rno") BigDecimal rno);

    @Query("SELECT COALESCE(MIN(g.rno), 0) FROM Gassan g WHERE g.jichitaiCd = :jichitaiCd AND g.gassanShiteiNo = :gassanShiteiNo AND g.rno > :rno AND g.delFlg = '0'")
    BigDecimal findNextRnoByJichitaiCdAndGassanShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("gassanShiteiNo") String gassanShiteiNo,
            @Param("rno") BigDecimal rno);
}
