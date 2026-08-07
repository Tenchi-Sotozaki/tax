package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.TopPageConfigForm;
import jp.lg.asp.accommodation.entity.TopPageContent;

public interface TopPageService {

    /** トップページ表示用コンテンツ取得 */
	List<TopPageContent> findShared();

    /** 編集フォーム初期値取得 */
    TopPageConfigForm loadForm(String kbn, String jichitaiCd);

    void save(TopPageConfigForm form);
}
