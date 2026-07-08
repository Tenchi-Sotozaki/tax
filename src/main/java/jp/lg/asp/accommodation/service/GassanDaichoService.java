package jp.lg.asp.accommodation.service;

import org.springframework.data.domain.Page;

import jp.lg.asp.accommodation.dto.GassanDaichoItem;
import jp.lg.asp.accommodation.dto.GassanDaichoSearchForm;

public interface GassanDaichoService {
    
    /**
     * 合算申告情報管理台帳の検索
     * 
     * @param searchForm 検索条件
     * @return 検索結果ページ
     */
    Page<GassanDaichoItem> search(GassanDaichoSearchForm searchForm);
    
    /**
     * 合算指定番号による詳細情報取得
     * 
     * @param gassanShiteiNo 合算指定番号
     * @return 合算申告情報詳細
     */
    GassanDaichoItem getByGassanShiteiNo(String gassanShiteiNo);
}