package jp.lg.asp.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 納入書動的データレスポンス
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NonyushoDataResponse {
    
    /** 税額 */
    private String zeigaku;
    
    /** 加算額 */
    private String kasan;
    
    /** 納期限 */
    private String nokigen;
    
    /** 自治体名 */
    private String cityName;
    
    /** 自治体コード */
    private String jichitaiCd;
    
    /** 口座番号 */
    private String kozaNo;
    
    /** 納入場所 */
    private String nonyuBasho;
    
    /** 指定金融機関名 */
    private String shiteiKinyuName;
    
    /** 取りまとめ店 */
    private String torimatome;
}