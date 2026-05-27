package jp.lg.asp.accommodation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.lg.asp.accommodation.entity.FukaZeiritsu;
import jp.lg.asp.accommodation.entity.FukaZeiritsuId;

public interface FukaZeiritsuRepository extends JpaRepository<FukaZeiritsu, FukaZeiritsuId> {

    List<FukaZeiritsu> findByJichitaiCdAndDelFlgOrderBySeqAsc(String jichitaiCd, String delFlg);

 // FukaZeiritsuRepository.java に追加
    Optional<FukaZeiritsu> findFirstByJichitaiCdAndFukaKbnAndDelFlgOrderBySeqAsc(String jichitaiCd, String fukaKbn, String delFlg);
}
