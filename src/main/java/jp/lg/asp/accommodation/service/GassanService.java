package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.GassanForm;

public interface GassanService {

    /** フォームに施設一覧を再セットする */
    void reloadFacilityList(GassanForm form);

    /** 合算指定番号で1件取得してフォームに変換する */
    GassanForm getByGassanShiteiNo(String gassanShiteiNo);

    /** 指定番号から登録フォームの初期値を生成する */
    GassanForm buildFormByShiteiNo(String shiteiNo);

    /** 合算申告を新規登録する */
    void register(GassanForm form);

    /** 合算指定番号をキーに合算申告を更新する */
    void updateByGassanShiteiNo(String gassanShiteiNo, GassanForm form);

    /** 合算指定番号をキーに論理削除する */
    void deleteByGassanShiteiNo(String gassanShiteiNo);

    /** 指定番号に紐づく最新の合算申告を照会フォームとして返す */
    GassanForm getLatestByShiteiNo(String shiteiNo);

    /** 指定番号に紐づく合算申告を指定の合算指定番号で照会フォームとして返す */
    GassanForm getViewFormByShiteiNo(String shiteiNo, String gassanShiteiNo);
}
