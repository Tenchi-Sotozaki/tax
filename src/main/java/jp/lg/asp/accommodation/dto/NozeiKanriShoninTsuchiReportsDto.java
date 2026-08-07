package jp.lg.asp.accommodation.dto;

import lombok.Data;

/**
 * 納税管理人承認(不承認)通知書レポート用DTO
 */
@Data
public class NozeiKanriShoninTsuchiReportsDto {

    /** 発行日 */
    private String hakkoYmd;

    /** 市区町村名 */
    private String cityName;

    /** 条例 */
    private String jorei;

    /** 特別徴収義務者住所 */
    private String tokuJusho;

    /** 特別徴収義務者名 */
    private String tokuName;

    /** 所在地（施設住所） */
    private String shisetsuJusho;

    /** 名称（施設名） */
    private String shisetsuName;

    /** 納税管理人住所 */
    private String nozeiKanriJusho;

    /** 納税管理人名 */
    private String nozeiKanriName;
    
    /** 承認・不承認 */
    private String shonin;

    /** 理由 */
    private String riyu;
    
    /** 公印 */
    private byte[] koin;
}