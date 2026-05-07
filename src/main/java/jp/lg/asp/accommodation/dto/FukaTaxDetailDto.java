package jp.lg.asp.accommodation.dto;
import lombok.Data;

@Data
public class FukaTaxDetailDto {
	// 画面表示用のラベル（例：10,000円 ～ 19,999円）
    private String label;

    // 自動計算に使用する単価（100円、200円など）
    private Long taxRate;

    // 画面で入力された宿泊数（人数）
    private Integer stayCount;

    // 画面で入力、または自動計算された税額
    private Long taxAmount;
    
 // 税率管理番号 (m_zeiritsu_teigaku の seq)
    private Integer zeiritsuSeq;

    // 定額管理番号 (m_zeiritsu_teigaku の teigaku_seq)
    private Integer teigakuSeq;

}