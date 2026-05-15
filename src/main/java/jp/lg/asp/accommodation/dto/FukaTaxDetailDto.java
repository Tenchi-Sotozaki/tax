package jp.lg.asp.accommodation.dto;

import lombok.Data;

@Data
public class FukaTaxDetailDto {
    private String label;
    private Long taxRate;
    
    // 💡 修正：個別の行は 0 や未入力（null）を許容するため制約を削除
    // 未入力時は Service 層で 0 に補完するぜ
    private Integer stayCount;
    private Long taxAmount;
    
    private Integer zeiritsuSeq;
    private Integer teigakuSeq;
    
}