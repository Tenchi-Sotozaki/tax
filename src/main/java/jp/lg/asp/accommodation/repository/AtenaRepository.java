package jp.lg.asp.accommodation.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;

@Repository
public interface AtenaRepository extends JpaRepository<Atena, AtenaId> {

    @Query("SELECT COUNT(a) > 0 FROM Atena a WHERE a.jichitaiCd = :jichitaiCd AND a.kojinNo = :kojinNo AND (:excludeAtenaNo IS NULL OR a.atenaNo <> :excludeAtenaNo)")
    boolean existsByKojinNo(
        @Param("jichitaiCd") String jichitaiCd,
        @Param("kojinNo") String kojinNo,
        @Param("excludeAtenaNo") @Nullable BigDecimal excludeAtenaNo
    );

    @Query("SELECT COUNT(a) > 0 FROM Atena a WHERE a.jichitaiCd = :jichitaiCd AND a.hojinNo = :hojinNo AND (:excludeAtenaNo IS NULL OR a.atenaNo <> :excludeAtenaNo)")
    boolean existsByHojinNo(
        @Param("jichitaiCd") String jichitaiCd,
        @Param("hojinNo") String hojinNo,
        @Param("excludeAtenaNo") @Nullable BigDecimal excludeAtenaNo
    );

    @Query("SELECT MAX(a.atenaNo) FROM Atena a WHERE a.jichitaiCd = :jichitaiCd")
    Optional<BigDecimal> findMaxAtenaNoByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);

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

    @Query("SELECT a FROM Atena a WHERE a.jichitaiCd = :jichitaiCd"
        + " AND (:atenaNo      = '%' OR CAST(a.atenaNo AS string) = :atenaNo)"
        + " AND a.name                           LIKE :namePattern"
        + " AND COALESCE(a.nameKana, '')          LIKE :nameKanaPattern"
        + " AND (:yubinNo      = '%' OR COALESCE(a.yubinNo, '')  = :yubinNo)"
        + " AND COALESCE(a.jusho, '')            LIKE :jushoPattern"
        + " AND (:tel          = '%' OR a.tel1 = :tel OR COALESCE(a.tel2, '') = :tel)"
        + " AND (:kojinNo      = '%' OR COALESCE(a.kojinNo, '')  = :kojinNo)"
        + " AND (:hojinNo      = '%' OR COALESCE(a.hojinNo, '')  = :hojinNo)")
    List<Atena> search(
        @Param("jichitaiCd")       String jichitaiCd,
        @Param("atenaNo")          String atenaNo,
        @Param("namePattern")      String namePattern,
        @Param("nameKanaPattern")  String nameKanaPattern,
        @Param("yubinNo")          String yubinNo,
        @Param("jushoPattern")     String jushoPattern,
        @Param("tel")              String tel,
        @Param("kojinNo")          String kojinNo,
        @Param("hojinNo")          String hojinNo
    );

    @Query("""
    		SELECT a FROM Atena a WHERE a.jichitaiCd = :jichitaiCd
              AND (CAST(a.atenaNo AS string) = :atenaNo
                OR a.name LIKE :namePattern
                OR (a.jusho IS NOT NULL AND a.jusho LIKE :jushoPattern)
                OR (a.tel1 IS NOT NULL AND a.tel1 = :tel)
                OR (a.tel2 IS NOT NULL AND a.tel2 = :tel)
                OR (a.kojinNo IS NOT NULL AND a.kojinNo  = :kojinNo)
                OR (a.hojinNo IS NOT NULL AND a.hojinNo = :hojinNo))
    		""")
        List<Atena> searchOr(
            @Param("jichitaiCd")       String jichitaiCd,
            @Param("atenaNo")          String atenaNo,
            @Param("namePattern")      String namePattern,
            @Param("jushoPattern")     String jushoPattern,
            @Param("tel")              String tel,
            @Param("kojinNo")          String kojinNo,
            @Param("hojinNo")          String hojinNo
        );
}
