package jp.lg.asp.accommodation.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;

@Repository
public interface AtenaRepository extends JpaRepository<Atena, AtenaId> {

    // 1件検索（自治体コード ＋ 宛名番号）
    Optional<Atena> findByJichitaiCdAndAtenaNo(String jichitaiCd, BigDecimal atenaNo);

    // リスト検索（自治体コード ＋ 宛名番号のリスト）
    List<Atena> findByJichitaiCdAndAtenaNoIn(String jichitaiCd, List<BigDecimal> atenaNos);

    // 削除（自治体コード ＋ 宛名番号）
    void deleteByJichitaiCdAndAtenaNo(String jichitaiCd, BigDecimal atenaNo);
}