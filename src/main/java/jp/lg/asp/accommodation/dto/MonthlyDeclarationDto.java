package jp.lg.asp.accommodation.dto;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * 1ヶ月分の宿泊税納入情報を保持するDTO
 */
@Data
public class MonthlyDeclarationDto {

    /** 納入年月 (yyyy/MM) */
    @DateTimeFormat(pattern = "yyyy/MM")
    private String paymentYearMonth;

    /** 課税対象宿泊数（税区分①） */
    private Integer stayCount1;
    /** 税額（税区分①） */
    private Long taxAmount1;

    /** 課税対象宿泊数（税区分②） */
    private Integer stayCount2;
    /** 税額（税区分②） */
    private Long taxAmount2;

    /** 課税対象宿泊数（税区分③） */
    private Integer stayCount3;
    /** 税額（税区分③） */
    private Long taxAmount3;

    /** 課税対象外宿泊数 */
    private Integer exemptStayCount;

    /** 総宿泊数 */
    private Integer totalStayCount;

    /** 納入金額 */
    private Long totalPaymentAmount;

}