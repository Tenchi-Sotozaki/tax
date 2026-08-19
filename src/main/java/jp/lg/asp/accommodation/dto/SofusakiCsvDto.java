package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 送付先CSV出力 一覧表示用DTO
 */
@Data
public class SofusakiCsvDto {

    /** 宛名番号 */
    private BigDecimal atenaNo;

    /** 指定番号 */
    private String shiteiNo;

    /** 氏名/名称 */
    private String soufusakiName;

    /** ふりがな */
    private String soufusakiNameKana;

    /** 郵便番号 */
    private String soufusakiYubinNo;

    /** 住所 */
    private String soufusakiJusho;

    /** 電話番号 */
    private String soufusakiTel;

    /** 帳票名（表示用） */
    private String rptName;

    /** 印刷日時（表示用） */
    private LocalDateTime opeDt;
}
