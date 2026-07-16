package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.TaxManager;
import jp.lg.asp.accommodation.entity.TaxManagerId;

@Repository
public interface TaxManagerRepository extends JpaRepository<TaxManager, TaxManagerId> {

    /**
     * 指定番号で最新の納税管理人情報を取得（newFlg = '1'）
     */
    @Query("SELECT t FROM TaxManager t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.newFlg = '1'")
    Optional<TaxManager> findLatestByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);

    /**
     * 指定番号で最大履歴番号を取得
     */
    @Query("SELECT COALESCE(MAX(t.rno), 0) FROM TaxManager t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo")
    Integer findMaxRnoByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);

    /**
     * 指定番号の現在の最新フラグを0に更新
     */
    @Modifying
    @Query("UPDATE TaxManager t SET t.newFlg = '0' WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.newFlg = '1'")
    void updateNewFlgToZero(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);

    /**
     * 特定の履歴番号の削除フラグを1に更新
     */
    @Modifying
    @Query("UPDATE TaxManager t SET t.delFlg = '1' WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.rno = :rno")
    void updateDelFlgToOne(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo, @Param("rno") Integer rno);

    /**
     * 特定の履歴番号の最新フラグを1に更新
     */
    @Modifying
    @Query("UPDATE TaxManager t SET t.newFlg = '1' WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.rno = :rno")
    void updateNewFlgToOneByRno(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo, @Param("rno") Integer rno);

    /**
     * 指定番号の全履歴を取得（履歴番号降順）
     */
    @Query("SELECT t FROM TaxManager t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo ORDER BY t.rno DESC")
    List<TaxManager> findAllByJichitaiCdAndShiteiNoOrderByRnoDesc(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);

    /**
     * 指定番号・履歴番号でデータを取得
     */
    @Query("SELECT t FROM TaxManager t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo AND t.rno = :rno")
    Optional<TaxManager> findByJichitaiCdAndShiteiNoAndRno(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo, @Param("rno") Integer rno);

    /**
     * 指定番号の最小履歴番号を取得
     */
    @Query("SELECT COALESCE(MIN(t.rno), 0) FROM TaxManager t WHERE t.jichitaiCd = :jichitaiCd AND t.shiteiNo = :shiteiNo")
    Integer findMinRnoByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);
}