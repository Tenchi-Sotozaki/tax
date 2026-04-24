package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.CollectorForm;
import jp.lg.asp.accommodation.dto.CollectorListItem;
import jp.lg.asp.accommodation.dto.CollectorSearchForm;
import jp.lg.asp.accommodation.dto.FacilityDto;
import jp.lg.asp.accommodation.dto.TaxManagerForm;

import java.util.List;

/**
 * 特別徴収義務者管理 Service インターフェース。
 * DB実装後はこのインターフェースを変えずに実装クラスのみ差し替える。
 */
public interface CollectorService {

    /** 検索条件に合致する一覧を返す */
    List<CollectorListItem> search(CollectorSearchForm form);

    /** IDで1件取得してフォームに変換する */
    CollectorForm getCollectorById(Long id);

    /** 義務者名を取得する */
    String getObligorName(String obligorId);

    /** 施設一覧を取得する */
    List<FacilityDto> getFacilities(String obligorId);

    /** 納税管理人フォームの初期値を生成する */
    TaxManagerForm buildTaxManagerForm(Long collectorId);

    /** 特別徴収義務者を登録する */
    void register(CollectorForm form);

    /** 特別徴収義務者を更新する */
    void update(Long id, CollectorForm form);

    /** 特別徴収義務者を削除する */
    void delete(Long id);

    /** 納税管理人を登録・更新する */
    void saveTaxManager(Long collectorId, TaxManagerForm form);

    /** IDから指定番号を取得する */
    String getShiteiNoById(Long id);
}
