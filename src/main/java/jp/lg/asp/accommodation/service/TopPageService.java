package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.TopPageConfigForm;
import jp.lg.asp.accommodation.entity.TopPageContent;

public interface TopPageService {

    /** トップページ表示用コンテンツ取得（共有＋自治体カスタマイズ） */
    TopPageContent findShared();

    TopPageContent findCustom(String jichitaiCd);

    /** 編集フォーム初期値取得 */
    TopPageConfigForm loadForm(String kbn, String jichitaiCd);

    void save(TopPageConfigForm form);
}
