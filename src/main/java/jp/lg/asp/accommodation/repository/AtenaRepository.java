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
import org.springframework.lang.Nullable;

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

    @Query("SELECT a FROM Atena a WHERE a.jichitaiCd = :jichitaiCd AND ("
        + "(:addressNumber IS NOT NULL AND a.atenaNo = :addressNumber) OR "
        + "(:name        IS NOT NULL AND a.name     = :name)           OR "
        + "(:address     IS NOT NULL AND a.jusho    = :address)        OR "
        + "(:phone       IS NOT NULL AND a.tel1     = :phone)          OR "
        + "(:kojinNo     IS NOT NULL AND a.kojinNo  = :kojinNo)        OR "
        + "(:hojinNo     IS NOT NULL AND a.hojinNo  = :hojinNo))")
    List<Atena> searchByAnyField(
        @Param("jichitaiCd")   String jichitaiCd,
        @Param("addressNumber") @Nullable BigDecimal addressNumber,
        @Param("name")          @Nullable String name,
        @Param("address")       @Nullable String address,
        @Param("phone")         @Nullable String phone,
        @Param("kojinNo")       @Nullable String kojinNo,
        @Param("hojinNo")       @Nullable String hojinNo
    );

    @Modifying
    @Query("DELETE FROM Atena a WHERE a.jichitaiCd = :jichitaiCd AND a.atenaNo = :atenaNo")
    void deleteByJichitaiCdAndAtenaNo(
        @Param("jichitaiCd") String jichitaiCd,
        @Param("atenaNo") BigDecimal atenaNo
    );
}
