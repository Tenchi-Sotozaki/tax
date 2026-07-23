package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Holiday;
import jp.lg.asp.accommodation.entity.HolidayId;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, HolidayId> {

    List<Holiday> findByJichitaiCdAndNendoOrderByHolidayDt(String jichitaiCd, String nendo);

    @Modifying
    @Query("DELETE FROM Holiday h WHERE h.jichitaiCd = :jichitaiCd AND h.nendo = :nendo")
    void deleteByJichitaiCdAndNendo(@Param("jichitaiCd") String jichitaiCd, @Param("nendo") String nendo);
}
