package jp.lg.asp.accommodation.dto;

import lombok.Data;

/**
 * 徴収不能額の還付又は納入義務の免除決定通知書帳票DTO
 */
@Data
public class KanpuMenjoTsuchiReportsDto {

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

    /** 所在地 */
    private String shisetsuJusho;

    /** 名称 */
    private String shisetsuName;

    /** 申請受理日 */
    private String juriYmd;

    /** 申請の年月 */
    private String shinseiYm;

    /** 申請した税額 */
    private String zeigaku;

    /** 還付又は納入義務の免除を決定した額 */
    private String kanpuMenjoGaku;

    /** 一部承認または不承認と決定した理由 */
    private String riyu;

    /** 備考 */
    private String biko;
    
    /** 公印 */
    private byte[] koin;
}