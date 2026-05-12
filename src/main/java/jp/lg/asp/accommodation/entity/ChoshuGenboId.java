package jp.lg.asp.accommodation.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 徴収原簿テーブルの複合主キー
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChoshuGenboId implements Serializable {
    private String jichitaiCd;
    private String shiteiNo;
    private Integer rno;
    private String nendo;
    private Integer kibetsu;
}