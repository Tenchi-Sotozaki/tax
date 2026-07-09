package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.TekiyoNozeiShuki;
import jp.lg.asp.accommodation.entity.TekiyoNozeiShukiId;

@Repository
public interface TekiyoNozeiShukiRepository extends JpaRepository<TekiyoNozeiShuki, TekiyoNozeiShukiId> {

    @Query("SELECT t FROM TekiyoNozeiShuki t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.newFlg = '1' AND t.delFlg = '0'")
    Optional<TekiyoNozeiShuki> findLatestByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);

    @Query("SELECT COALESCE(MAX(t.rno), 0) FROM TekiyoNozeiShuki t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo")
    Integer findMaxRnoByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);

    @Query("SELECT COALESCE(MAX(t.idxRno), 0) FROM TekiyoNozeiShuki t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.rno = :rno")
    Integer findMaxIdxRnoByKey(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo,
            @Param("rno") Integer rno);

    @Modifying
    @Query("UPDATE TekiyoNozeiShuki t SET t.newFlg = '0' WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.newFlg = '1'")
    void updateNewFlgToZero(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);

    @Query("SELECT t FROM TekiyoNozeiShuki t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.delFlg = '0'")
    List<TekiyoNozeiShuki> findActiveByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);

    @Query("SELECT t FROM TekiyoNozeiShuki t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.rno < :rno AND t.delFlg = '0' ORDER BY t.rno DESC, t.idxRno DESC")
    List<TekiyoNozeiShuki> findPreviousRecords(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo,
            @Param("rno") Integer rno);
}
