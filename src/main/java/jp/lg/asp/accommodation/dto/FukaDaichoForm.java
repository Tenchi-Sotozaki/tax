package jp.lg.asp.accommodation.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

//を整理
@Getter
@Setter
public class FukaDaichoForm {
 
 // --- 検索条件 ---
 /** 検索対象年度 (例: 2026) */
 private String nendo;
 
 /** 抽出ステータス ("999":すべて, "1":申告済み, "2":未申告) */
 private String status;

 private String shiteiNo;

 // --- ヘッダー情報 ---
 private String obligorName;
 private String shukiKbnName;

 private Long totalAmount;
 
 // --- 一覧データ ---
 private List<FukaDaichoListItem> items = new ArrayList<>();
}