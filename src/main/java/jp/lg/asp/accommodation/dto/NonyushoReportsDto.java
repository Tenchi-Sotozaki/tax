package jp.lg.asp.accommodation.dto;

import lombok.Data;

/**
 * 納入書レポートDTO
 */
@Data
public class NonyushoReportsDto {
    
    /** 自治体名 */
    private String cityName;
    
    /** 自治体コード */
    private String jichitaiCd;
    
    /** 口座番号 */
    private String kozaNo;
    
    /** 加入者名 */
    private String kozaName;
    
    /** 年度 */
    private String nendo;
    
    /** 申告年月 */
    private String shinkokuYm;
    
    /** 指定番号 */
    private String shiteiNo;
    
    /** 税額 */
    private String zeigaku;
    
    /** 延滞金 */
    private String entai;
    
    /** 加算額 */
    private String kasan;
    
    /** 合計額 */
    private String gokei;
    
    /** 納期限 */
    private String nokigen;
    
    /** 特別徴収義務者住所(所在地) */
    private String tokuJusho;
    
    /** 特別徴収義務者氏名(名称) */
    private String tokuName;
    
    /** 納入場所 */
    private String nonyuBasho;
    
    /** 指定金融機関名 */
    private String shiteiKinyuName;
    
    /** 取りまとめ店 */
    private String torimatome;
    
    /** 公印 */
    private byte[] koin;
}