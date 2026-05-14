package jp.lg.asp.accommodation.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 納入金額管理台帳画面用DTO
 */
@Getter
@Setter
public class FukaDaichoForm {

    // 検索対象年度 (例: 2026)
    private String nendo;

    // 抽出ステータス ("999":すべて, "1":申告済み, "2":未申告)
    private String status;

    // 指定番号
    private String shiteiNo;

    // 特別徴収義務者名称
    private String obligorName;

    // 周期区分名称
    private String shukiKbnName;

    // 合計金額
    private Long totalAmount;

    // 納入金額管理台帳明細リスト
    private List<FukaDaichoListItem> items = new ArrayList<>();
}