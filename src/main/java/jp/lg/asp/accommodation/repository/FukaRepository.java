package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaId;

@Repository
public interface FukaRepository extends JpaRepository<Fuka, FukaId> {
    
    List<Fuka> findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
            String jichitaiCd, String shiteiNo, String nendo);
    
}