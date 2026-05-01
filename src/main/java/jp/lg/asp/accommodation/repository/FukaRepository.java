package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaId;

@Repository
public interface FukaRepository extends JpaRepository<Fuka, FukaId> {
    
    // 基本の検索（全件表示用）
    List<Fuka> findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
            String jichitaiCd, String shiteiNo, String nendo);

    // 申告済み絞り込み：shinkokuYmd が NULL ではない
    List<Fuka> findByJichitaiCdAndShiteiNoAndNendoAndShinkokuYmdIsNotNullOrderByKibetsuAsc(
            String jichitaiCd, String shiteiNo, String nendo);

    // 未申告絞り込み：shinkokuYmd が NULL である
    List<Fuka> findByJichitaiCdAndShiteiNoAndNendoAndShinkokuYmdIsNullOrderByKibetsuAsc(
            String jichitaiCd, String shiteiNo, String nendo);
    
    Optional<Fuka> findByJichitaiCdAndShiteiNoAndKibetsu(String jichitaiCd, String shiteiNo, Integer kibetsu);
}