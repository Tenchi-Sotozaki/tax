package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.TekiyoNozeiShuki;
import jp.lg.asp.accommodation.entity.TekiyoNozeiShukiId;

@Repository
public interface TekiyoNozeiShukiRepository extends JpaRepository<TekiyoNozeiShuki, TekiyoNozeiShukiId> {

    @Query("SELECT t FROM TekiyoNozeiShuki t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.delFlg = '0' ORDER BY t.idx DESC")
    List<TekiyoNozeiShuki> findLatestByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);



    @Query("SELECT COALESCE(MAX(t.idx), 0) FROM TekiyoNozeiShuki t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo")
    Integer findMaxIdxByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);

    @Query("SELECT t FROM TekiyoNozeiShuki t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.delFlg = '0'")
    List<TekiyoNozeiShuki> findActiveByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);

    @Query("SELECT t FROM TekiyoNozeiShuki t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.idx < :idx AND t.delFlg = '0' ORDER BY t.idx DESC")
    List<TekiyoNozeiShuki> findPreviousRecords(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo,
            @Param("idx") Integer idx);
}
