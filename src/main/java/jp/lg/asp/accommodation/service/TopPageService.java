package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.TopPageConfigForm;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.TopPageContent;

public interface TopPageService {

    /** トップページ表示用コンテンツ取得 */
	List<TopPageContent> findShared();
	
	/** 一覧取得 */
	List<TopPageContent> findAll();

    /** 編集フォーム初期値取得 */
    TopPageConfigForm loadForm();
    
    /** 編集対象取得 */
    TopPageContent findBySeq(Integer seq);
	
    /** 保存 */
    void save(TopPageConfigForm form);
    
    /** 削除 */
    void delete(Integer seq);

    /** 自治体情報取得 */
    Jichitai findJichitai(String jichitaiCd);
}
