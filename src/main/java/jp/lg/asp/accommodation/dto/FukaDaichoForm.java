package jp.lg.asp.accommodation.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FukaDaichoForm {
    
    // --- 検索条件 ---
    private String shiteiNo;
    private String nendo;
    private String status; // "999":すべて, "1":申告済み, "2":未申告

    // --- ヘッダー情報（特別徴収義務者） ---
    private String obligorName; // 氏名/名称

    // --- 納税周期情報エリア ---
    private String shukiKbnName; // 例: "毎月申告"

    
    private Long totalAmount;
    // --- 一覧データ ---
    private List<FukaDaichoListItem> items = new ArrayList<>();
    
   
}