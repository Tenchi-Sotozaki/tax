package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.TokureiTekiyo;
import jp.lg.asp.accommodation.entity.TokureiTekiyoId;

@Repository
public interface TokureiTekiyoRepository extends JpaRepository<TokureiTekiyo, TokureiTekiyoId> {
    @Query("SELECT t FROM TokureiTekiyo t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.delFlg = '0' ORDER BY t.tekiyoStYmd ASC")
    List<TokureiTekiyo> findActiveByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);

    @Query("SELECT COALESCE(MAX(t.rno), 0) FROM TokureiTekiyo t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo")
    int findMaxRnoByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);
}
