package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Data;

// Entityクラスで併せ持つ機能のうち３つ以上（Getter, Setter, ToString等）を付与するため@Dataを使用
@Entity
@Table(name = "m_zeiritsu_teigaku")
@IdClass(FukaZeiritsuTeigakuId.class)
@Data
public class FukaZeiritsuTeigaku {

    // 自治体コード
    @Id
    private String jichitaiCd;
    
    // 税率管理番号
    @Id
    private Integer seq;

    // 定額管理番号
    @Id
    private Integer teigakuSeq;

    // 宿泊料金範囲開始
    private Long ryokinSt;
    
    // 宿泊料金範囲終了（上限なしの場合はnullを許容）
    private Long ryokinEd;
    
    // 宿泊税額
    private Long zeigaku;
    
    // 削除フラグ
    private String delFlg;
}