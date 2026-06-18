package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.GassanDaichoItem;
import jp.lg.asp.accommodation.dto.GassanDaichoSearchForm;

public interface GassanDaichoService {
    
    /**
     * 合算申告情報管理台帳の検索
     * 
     * @param searchForm 検索条件
     * @return 検索結果リスト
     */
    List<GassanDaichoItem> search(GassanDaichoSearchForm searchForm);
    
    /**
     * 合算指定番号による詳細情報取得
     * 
     * @param gassanShiteiNo 合算指定番号
     * @return 合算申告情報詳細
     */
    GassanDaichoItem getByGassanShiteiNo(String gassanShiteiNo);
}