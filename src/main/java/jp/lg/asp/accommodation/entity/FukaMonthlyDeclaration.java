package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

/**
 * 宿泊税の月別申告情報を保存するエンティティ
 */
@Entity // 規約 4.2[cite: 2]
@Table(name = "monthly_declarations") // 実際のテーブル名に合わせて変更してくれ
@Data // 規約 4.3 (Getter, Setter, ToString等を持つため)[cite: 2]
public class FukaMonthlyDeclaration {

    // 申告データの主キー（自動採番）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 誰の申告か紐付けるための指定番号
    @Column(name = "shitei_no", nullable = false)
    private String shiteiNo;

    // 納入年月 (yyyy/MM)
    @Column(name = "payment_year_month", nullable = false)
    private String paymentYearMonth;

    // 税区分①の宿泊数と税額
    @Column(name = "stay_count_1")
    private Integer stayCount1;

    @Column(name = "tax_amount_1")
    private Long taxAmount1;

    // 税区分②の宿泊数と税額
    @Column(name = "stay_count_2")
    private Integer stayCount2;

    @Column(name = "tax_amount_2")
    private Long taxAmount2;

    // 税区分③の宿泊数と税額
    @Column(name = "stay_count_3")
    private Integer stayCount3;

    @Column(name = "tax_amount_3")
    private Long taxAmount3;

    // 課税対象外宿泊数
    @Column(name = "exempt_stay_count")
    private Integer exemptStayCount;

    // 総宿泊数
    @Column(name = "total_stay_count")
    private Integer totalStayCount;

    // 納入金額合計
    @Column(name = "total_payment_amount")
    private Long totalPaymentAmount;

}