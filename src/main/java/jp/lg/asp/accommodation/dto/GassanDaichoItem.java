package jp.lg.asp.accommodation.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GassanDaichoItem {
    
    // 合算指定番号
    private String gassanShiteiNo;
    
    // 代表施設（代表指定番号に紐づく施設名称）
    private String daihyoShisetsuName;
    
    // 指定番号
    private String shiteiNo;
    
    // 氏名/名称
    private String name;
    
    // 宛名番号
    private BigDecimal atenaNo;
    
    // 合算対象施設のリスト（詳細表示用）
    private List<GassanFacilityItem> facilityList;
    
    @Getter
    @Setter
    public static class GassanFacilityItem {
        private String shiteiNo;
        private String shisetsuName;
        private String name;
        private BigDecimal atenaNo;
    }
}