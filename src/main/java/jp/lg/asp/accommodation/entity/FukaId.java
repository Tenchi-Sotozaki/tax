package jp.lg.asp.accommodation.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 賦課台帳 (t_fuka) の複合主キー定義クラス
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // 複合主キーには必須
public class FukaId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jichitaiCd;  // 自治体コード
    private String shiteiNo;    // 指定番号
    private Integer rno;        // 履歴番号
    private String nendo;       // 年度
    private Integer kibetsu;    // 期別
}