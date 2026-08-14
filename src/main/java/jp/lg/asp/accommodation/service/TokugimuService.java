package jp.lg.asp.accommodation.service;

import org.springframework.data.domain.Page;

import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuListItem;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;

/**
 * 特別徴収義務者管理 Service インターフェース。
 */
public interface TokugimuService {

	// 検索条件に合致する一覧を返す
	Page<TokugimuListItem> search(TokugimuSearchForm form);

	// 指定番号で1件取得してフォームに変換する
	TokugimuForm getTokugimuByShiteiNo(String shiteiNo);

	// 指定番号・履歴番号で1件取得してフォームに変換する
	TokugimuForm getTokugimuByShiteiNoAndRno(String shiteiNo, int rno);

	// 特別徴収義務者を登録する
	void register(TokugimuForm form);

	// 指定番号をキーに特別徴収義務者を更新する
	void updateByShiteiNo(String shiteiNo, TokugimuForm form);

	// 指定番号をキーに特別徴収義務者を削除する
	/**
	 * 最新の履歴を論理削除する。
	 * 削除後も履歴が残っている場合は、残っている中で最も新しい履歴を最新版に戻す。
	 *
	 * @param shiteiNo 指定番号
	 * @return 削除後も履歴が残っている場合 true、すべて削除された場合 false
	 */
	boolean deleteByShiteiNo(String shiteiNo);

	// IDから指定番号を取得する
	String getShiteiNoById(Long id);

	/**
	 * 指定番号が合算対象かどうかを判定する。
	 * 判定基準は特別徴収義務者管理台帳の「合算対象」と同じ。
	 *
	 * @param shiteiNo 指定番号
	 * @return 合算対象の場合 true
	 */
	boolean isGassanTarget(String shiteiNo);
}