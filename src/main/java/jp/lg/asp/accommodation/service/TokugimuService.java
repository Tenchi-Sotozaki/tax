package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuListItem;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;

/**
 * 特別徴収義務者管理 Service インターフェース。
 * DB実装後はこのインターフェースを変えずに実装クラスのみ差し替える。
 */
public interface TokugimuService {

	/** 検索条件に合致する一覧を返す */
	List<TokugimuListItem> search(TokugimuSearchForm form);

	/** IDで1件取得してフォームに変換する */
	TokugimuForm getTokugimuById(Long id);

	/** 義務者名を取得する */
	String getTokugimuName(String obligorId);

	/** 納税管理人フォームの初期値を生成する */
	TaxManagerForm buildTaxManagerForm(Long collectorId);

	/** 特別徴収義務者を登録する */
	void register(TokugimuForm form);

	/** 特別徴収義務者を更新する */
	void update(Long id, TokugimuForm form);

	/** 特別徴収義務者を削除する */
	void delete(Long id);

	/** 納税管理人を登録・更新する */
	void saveTaxManager(Long collectorId, TaxManagerForm form);

	/** IDから指定番号を取得する */
	String getShiteiNoById(Long id);
}
