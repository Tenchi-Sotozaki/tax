package jp.lg.asp.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.lg.asp.accommodation.entity.FukaZeiritsuTeigaku;
import jp.lg.asp.accommodation.entity.FukaZeiritsuTeigakuId;

public interface FukaZeiritsuTeigakuRepository extends JpaRepository<FukaZeiritsuTeigaku, FukaZeiritsuTeigakuId> {
    
    // 自治体コードで検索し、料金開始額の昇順（安い順）で取得する
    List<FukaZeiritsuTeigaku> findByJichitaiCdOrderByRyokinStAsc(String jichitaiCd);
}