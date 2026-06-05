package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.TokugimuForm;

import jp.lg.asp.accommodation.dto.TokugimuListItem;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;

/**
 * 特別徴収義務者管理 Service インターフェース。
 */
public interface TokugimuService {

	// 検索条件に合致する一覧を返す
	List<TokugimuListItem> search(TokugimuSearchForm form);

	// 指定番号で1件取得してフォームに変換する
	TokugimuForm getTokugimuByShiteiNo(String shiteiNo);

	// 義務者名を取得する
	String getTokugimuName(String obligorId);

	// 特別徴収義務者を登録する
	void register(TokugimuForm form);

	// 指定番号をキーに特別徴収義務者を更新する
	void updateByShiteiNo(String shiteiNo, TokugimuForm form);

	// 指定番号をキーに特別徴収義務者を削除する
	void deleteByShiteiNo(String shiteiNo);

	// IDから指定番号を取得する
	String getShiteiNoById(Long id);
}