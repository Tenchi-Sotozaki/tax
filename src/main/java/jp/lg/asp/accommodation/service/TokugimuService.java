package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuListItem;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;

/**
 * 特別徴収義務者管理 Service インターフェース。
 * 指定番号（shiteiNo）を基準とした処理に変更。
 */
public interface TokugimuService {

	/** 検索条件に合致する一覧を返す */
	List<TokugimuListItem> search(TokugimuSearchForm form);

	/** 指定番号で1件取得してフォームに変換する (Long id から String shiteiNo に変更) */
	TokugimuForm getTokugimuByShiteiNo(String shiteiNo);

	/** 義務者名を取得する */
	String getTokugimuName(String obligorId);

	/** 納税管理人フォームの初期値を生成する (指定番号から宛名IDを特定して処理) */
	TaxManagerForm buildTaxManagerFormByShiteiNo(String shiteiNo);

	/** 特別徴収義務者を登録する */
	void register(TokugimuForm form);

	/** 指定番号をキーに特別徴収義務者を更新する */
	void updateByShiteiNo(String shiteiNo, TokugimuForm form);

	/** 指定番号をキーに特別徴収義務者を削除する */
	void deleteByShiteiNo(String shiteiNo);

	/** 指定番号から紐づく事業者（宛名）に対して納税管理人を登録・更新する */
	void saveTaxManagerByShiteiNo(String shiteiNo, TaxManagerForm form);

	/** IDから指定番号を取得する（他サービスからの呼び出し用） */
	String getShiteiNoById(Long id);
}