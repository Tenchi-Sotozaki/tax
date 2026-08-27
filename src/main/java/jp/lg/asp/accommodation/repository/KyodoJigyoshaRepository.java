package jp.lg.asp.accommodation.repository;

import jp.lg.asp.accommodation.entity.KyodoJigyosha;
import jp.lg.asp.accommodation.entity.KyodoJigyoshaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface KyodoJigyoshaRepository extends JpaRepository<KyodoJigyosha, KyodoJigyoshaId> {

    /** 履歴方式のため、rno を指定して該当世代の行だけを取得する */
    @Query("SELECT k FROM KyodoJigyosha k WHERE k.jichitaiCd = :jichitaiCd AND k.shiteiNo = :shiteiNo AND k.rno = :rno ORDER BY k.idx")
    List<KyodoJigyosha> findByJichitaiCdAndShiteiNoAndRno(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo,
            @Param("rno") BigDecimal rno);

    @Query("SELECT k FROM KyodoJigyosha k WHERE k.jichitaiCd = :jichitaiCd AND k.shiteiNo = :shiteiNo ORDER BY k.rno DESC, k.idx")
    List<KyodoJigyosha> findByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);

    @Modifying
    @Query("DELETE FROM KyodoJigyosha k WHERE k.jichitaiCd = :jichitaiCd AND k.shiteiNo = :shiteiNo")
    void deleteByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);
}
