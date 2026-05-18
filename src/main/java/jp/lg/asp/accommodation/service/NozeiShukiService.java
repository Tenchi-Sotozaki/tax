package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;
import java.util.List;

import jp.lg.asp.accommodation.dto.NozeiShukiDto;
import jp.lg.asp.accommodation.entity.NozeiShuki;

public interface NozeiShukiService {

    /** 自治体コードに紐づく有効な納税周期マスタを取得する */
    List<NozeiShukiDto> findAll();
    
    /** 周期で検索 */
    List<NozeiShukiDto> findByShuki(Integer shuki);
    
    /** SEQで詳細取得 */
    NozeiShuki findBySeq(BigDecimal seq);
    
    /** 周期の重複チェック（新規登録時） */
    boolean existsByShuki(BigDecimal shuki);

    /** 周期の重複チェック（更新時） */
    boolean existsByShukiExcludeSeq(BigDecimal shuki, BigDecimal seq);

    /** 納税周期保存 */
    NozeiShuki save(NozeiShuki nozeiShuki);
    
    /** 納税周期削除 */
    void delete(BigDecimal seq);
}
