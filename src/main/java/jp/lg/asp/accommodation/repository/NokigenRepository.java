package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.entity.NokigenId;

@Repository
public interface NokigenRepository extends JpaRepository<Nokigen, NokigenId> {

    @Query("SELECT n FROM Nokigen n WHERE n.jichitaiCd = :jichitaiCd ORDER BY n.nendo DESC")
    List<Nokigen> findAllByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);

    @Query("SELECT COUNT(n) FROM Nokigen n WHERE n.jichitaiCd = :jichitaiCd AND n.nendo = :nendo")
    long countByJichitaiCdAndNendo(@Param("jichitaiCd") String jichitaiCd, @Param("nendo") String nendo);
}
