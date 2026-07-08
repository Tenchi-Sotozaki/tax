package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.KofukinFurikomi;
import jp.lg.asp.accommodation.entity.KofukinFurikomiId;

@Repository
public interface KofukinFurikomiRepository extends JpaRepository<KofukinFurikomi, KofukinFurikomiId> {

    @Query("SELECT k FROM KofukinFurikomi k WHERE k.jichitaiCd = :jichitaiCd AND k.shiteiNo = :shiteiNo ORDER BY k.furikomiYmd DESC, k.rno DESC")
    List<KofukinFurikomi> findByJichitaiCdAndShiteiNo(@Param("jichitaiCd") String jichitaiCd, @Param("shiteiNo") String shiteiNo);

    Optional<KofukinFurikomi> findByJichitaiCdAndShiteiNoAndTaishoYmAndRno(
            String jichitaiCd, String shiteiNo, String taishoYm, Integer rno);

    @Query("SELECT k FROM KofukinFurikomi k WHERE k.jichitaiCd = :jichitaiCd AND k.newFlg = '1' AND k.delFlg = '0' ORDER BY k.furikomiYmd DESC, k.shiteiNo ASC")
    List<KofukinFurikomi> findActiveByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);
}