package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * 納税管理人選任免除認定（不認定）通知書DTO
 */
@Data
public class NozeiKanrininNinteiDto {

    /** 発行日 */
    private LocalDate hakkoYmd;

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

    /** 認定区分（認定/不認定） */
    private String nintei;

    /** 備考（不認定の理由） */
    private String biko;

    /** 指定番号（内部処理用） */
    private String shiteiNo;
    
    /** 公印 */
    private byte[] koin;
}
