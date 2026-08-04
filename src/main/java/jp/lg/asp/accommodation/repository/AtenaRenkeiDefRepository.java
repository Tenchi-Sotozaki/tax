package jp.lg.asp.accommodation.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.AtenaRenkeiDef;
import jp.lg.asp.accommodation.entity.AtenaRenkeiDefId;

@Repository
public interface AtenaRenkeiDefRepository extends JpaRepository<AtenaRenkeiDef, AtenaRenkeiDefId> {

    List<AtenaRenkeiDef> findByJichitaiCdAndSeqOrderByAtenaNoAsc(String jichitaiCd, BigDecimal seq);
}
