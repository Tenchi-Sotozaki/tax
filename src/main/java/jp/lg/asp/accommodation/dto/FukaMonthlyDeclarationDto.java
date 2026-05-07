package jp.lg.asp.accommodation.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 月ごとの申告情報を保持するDTO
 */
@Data
public class FukaMonthlyDeclarationDto {

    // 納入年月（yyyy-MM形式）
    private String paymentYearMonth;

    // 税区分の数だけ動的に行が増えるリスト
    private List<FukaTaxDetailDto> taxDetails = new ArrayList<>();

    private Integer exemptStayCount; 

    private Integer totalStayCount;

    // 合計納入金額（税額合計）
    private Long totalPaymentAmount;
}