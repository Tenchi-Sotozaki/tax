package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.NozeiShuki;
import jp.lg.asp.accommodation.entity.NozeiShukiId;

@Repository
public interface NozeiShukiRepository extends JpaRepository<NozeiShuki, NozeiShukiId> {

    @Query("SELECT n FROM NozeiShuki n WHERE n.jichitaiCd = :jichitaiCd AND n.delFlg = '0' ORDER BY n.seq")
    List<NozeiShuki> findActiveByJichitaiCd(@Param("jichitaiCd") String jichitaiCd);
}
