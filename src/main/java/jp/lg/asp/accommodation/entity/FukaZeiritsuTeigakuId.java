package jp.lg.asp.accommodation.entity;

import java.io.Serializable;

import lombok.Data;

// Entityクラスで併せ持つ機能のうち３つ以上（Getter, Setter, EqualsAndHashCode等）を付与するため@Dataを使用
@Data
public class FukaZeiritsuTeigakuId implements Serializable {

    // シリアルバージョンUID（Serializable実装時の定石）
    private static final long serialVersionUID = 1L;

    // 自治体コード
    private String jichitaiCd;
    
    // 税率管理番号
    private Integer seq;

    // 定額管理番号
    private Integer teigakuSeq;

}