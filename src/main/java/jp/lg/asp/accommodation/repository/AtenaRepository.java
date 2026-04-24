package jp.lg.asp.accommodation.repository;

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AtenaRepository extends JpaRepository<Atena, AtenaId> {

    @Query("SELECT a FROM Atena a WHERE a.jichitaiCd = :jichitaiCd AND a.atenaNo IN :atenaNos")
    List<Atena> findByJichitaiCdAndAtenaNoIn(
        @Param("jichitaiCd") String jichitaiCd,
        @Param("atenaNos") List<BigDecimal> atenaNos
    );

    @Query("SELECT a FROM Atena a WHERE a.jichitaiCd = :jichitaiCd AND a.atenaNo = :atenaNo")
    Optional<Atena> findByJichitaiCdAndAtenaNo(
        @Param("jichitaiCd") String jichitaiCd,
        @Param("atenaNo") BigDecimal atenaNo
    );

    @Modifying
    @Query("DELETE FROM Atena a WHERE a.jichitaiCd = :jichitaiCd AND a.atenaNo = :atenaNo")
    void deleteByJichitaiCdAndAtenaNo(
        @Param("jichitaiCd") String jichitaiCd,
        @Param("atenaNo") BigDecimal atenaNo
    );
}
