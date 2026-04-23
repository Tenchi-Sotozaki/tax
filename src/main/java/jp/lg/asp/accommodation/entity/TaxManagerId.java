package jp.lg.asp.accommodation.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 納税管理人テーブル(t_nokan)の複合主キー定義クラス
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxManagerId implements Serializable {

    private String jichitaiCd; // 自治体コード
    private String shiteiNo;   // 指定番号
    private Integer rno;       // 履歴番号

}