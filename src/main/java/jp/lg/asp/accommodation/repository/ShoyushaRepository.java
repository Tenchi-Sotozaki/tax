package jp.lg.asp.accommodation.repository;

import jp.lg.asp.accommodation.entity.Shoyusha;
import jp.lg.asp.accommodation.entity.ShoyushaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShoyushaRepository extends JpaRepository<Shoyusha, ShoyushaId> {

    @Query("SELECT s FROM Shoyusha s WHERE s.jichitaiCd = :jichitaiCd AND s.shiteiNo = :shiteiNo ORDER BY s.idx")
    List<Shoyusha> findByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);

    @Modifying
    @Query("DELETE FROM Shoyusha s WHERE s.jichitaiCd = :jichitaiCd AND s.shiteiNo = :shiteiNo")
    void deleteByJichitaiCdAndShiteiNo(
            @Param("jichitaiCd") String jichitaiCd,
            @Param("shiteiNo") String shiteiNo);
}
