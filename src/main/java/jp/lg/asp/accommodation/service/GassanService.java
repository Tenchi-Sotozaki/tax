package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jp.lg.asp.accommodation.dto.GassanForm;

public interface GassanService {

    /** フォームに施設一覧を再セットする */
    void reloadFacilityList(GassanForm form);

    /** 合算指定番号で1件取得してフォームに変換する */
    GassanForm getByGassanShiteiNo(String gassanShiteiNo);

    /** 合算指定番号・履歴番号でフォームに変換する */
    GassanForm getByGassanShiteiNoAndRno(String gassanShiteiNo, BigDecimal rno);

    /** 指定番号から登録フォームの初期値を生成する */
    GassanForm buildFormByShiteiNo(String shiteiNo);

    /** 合算申告を新規登録する（gassanShiteiNo が非nullなら再登録）。登録した合算指定番号を返す */
    String register(GassanForm form, String gassanShiteiNo);

    /** 合算指定番号をキーに合算申告を更新する */
    void updateByGassanShiteiNo(String gassanShiteiNo, GassanForm form);

    /** 合算指定番号をキーに論理削除する */
    void deleteByGassanShiteiNo(String gassanShiteiNo);

    /** 指定番号に紐づく最新の合算申告を照会フォームとして返す */
    GassanForm getLatestByShiteiNo(String shiteiNo);

    /** 指定番号に紐づく合算申告を指定の合算指定番号で照会フォームとして返す */
    GassanForm getViewFormByShiteiNo(String shiteiNo, String gassanShiteiNo);

    /** 宛名番号に紐づく施設一覧を取得する */
    List<GassanForm.FacilityItem> getFacilitiesByAtenaNo(BigDecimal atenaNo);

    /** 指定番号がすでに合算指定済みかチェックする（excludeGassanShiteiNo は除外対象） */
    void validateNotAlreadyAssigned(List<String> shiteiNoList, String excludeGassanShiteiNo);
}
