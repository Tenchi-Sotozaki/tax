package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;
import jp.lg.asp.accommodation.dto.FukaTaxDetailDto;
import jp.lg.asp.accommodation.enums.FukaKbn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宿泊税申告のバリデーションを行うサービス
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FukaValidatorService {

	/**
	 * 納入年月と宿泊数の相関チェックを実行する
	 */
	public void validateCorrelation(FukaDeclarationForm form) {
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
		if (detail == null)
			return;

		FukaKbn kbn = FukaKbn.fromCode(form.getFukaKbn());

		if (StringUtils.hasText(detail.getPaymentYearMonth())) {
			if (kbn.isTeiritsu()) {
				checkStayCountForTeiritsu(form);
			} else {
				checkStayCountForTeigaku(detail);
			}
		}

		// 明細から算出した税額合計と、画面の合計値の整合性チェック
		validateTaxTotal(form);
	}

	/**
	 * 明細リストから税額の正解値を再計算し、画面の合計値と比較する。
	 * 不一致の場合は IllegalArgumentException をスローする。
	 */
	private void validateTaxTotal(FukaDeclarationForm form) {
		long expectedTotal = calculateExpectedTotal(form);
		long inputTotal = getInputTotal(form);

		if (expectedTotal != inputTotal) {
			log.error("税額整合性エラー: 明細からの算出値={}, 画面入力値={}", expectedTotal, inputTotal);
			throw new IllegalArgumentException(
					String.format("税額の合計が明細と一致しません。明細からの算出値: %,d円 / 画面入力値: %,d円", expectedTotal, inputTotal));
		}
	}

	/**
	 * 明細リスト（taxDetails）から税額の正解値を算出する。
	 * Enumの calculateTax メソッドに計算を委譲することで、定額/定率の分岐を排除する。
	 */
	public long calculateExpectedTotal(FukaDeclarationForm form) {
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
		if (detail == null || detail.getTaxDetails() == null || detail.getTaxDetails().isEmpty()) {
			return 0L;
		}

		FukaKbn kbn = FukaKbn.fromCode(form.getFukaKbn());

		if (kbn.isTeiritsu()) {
			//定率制の計算ロジック：フォームの「課税対象宿泊料金」 × マスタの「税率」
			BigDecimal rate = detail.getTaxDetails().get(0).getTaxRate();
			if (rate == null) rate = BigDecimal.ZERO;
			
			long ryokin = (form.getKazeiRyokin() != null) ? form.getKazeiRyokin() : 0L;
			return kbn.calculateTax(rate, ryokin);
		} else {
			//定額制の計算ロジック：各明細の「単価」 × 「宿泊数」の合計
			long total = 0L;
			for (FukaTaxDetailDto d : detail.getTaxDetails()) {
				BigDecimal rate = (d.getTaxRate() != null) ? d.getTaxRate() : BigDecimal.ZERO;
				long count = (d.getStayCount() != null) ? d.getStayCount() : 0L;
				total += kbn.calculateTax(rate, count);
			}
			return total;
		}
	}

	/**
	 * 画面から送信された合計税額を取得する。
	 * 定率制の場合は taxDetails[0].taxAmount、定額制の場合は totalPaymentAmount を参照する。
	 */
	private long getInputTotal(FukaDeclarationForm form) {
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
		if (detail == null) return 0L;

		FukaKbn kbn = FukaKbn.fromCode(form.getFukaKbn());

		if (kbn.isTeiritsu()) {
			if (detail.getTaxDetails() != null && !detail.getTaxDetails().isEmpty()) {
				Long amt = detail.getTaxDetails().get(0).getTaxAmount();
				return (amt != null) ? amt : 0L;
			}
			return 0L;
		} else {
			return (detail.getTotalPaymentAmount() != null) ? detail.getTotalPaymentAmount() : 0L;
		}
	}

	/**
	 * 定額制：宿泊数と合計値の整合性チェック
	 */
	private void checkStayCountForTeigaku(FukaMonthlyDeclarationDto detail) {
		long sumOfDetails = 0;

		if (detail.getTaxDetails() != null) {
			for (FukaTaxDetailDto taxDetail : detail.getTaxDetails()) {
				if (taxDetail.getStayCount() != null) {
					sumOfDetails += taxDetail.getStayCount();
				}
			}
		}

		if (detail.getExemptStayCount() != null) {
			sumOfDetails += detail.getExemptStayCount();
		}

		if (detail.getTotalStayCount() == null || detail.getTotalStayCount() != sumOfDetails) {
			log.error("整合性エラー(定額): 画面合計={}, 明細積み上げ={}", detail.getTotalStayCount(), sumOfDetails);
			throw new IllegalArgumentException("宿泊数の合計値が明細と一致しません。再計算してください。");
		}
	}

	/**
	 * 定率制：宿泊数と合計値の整合性チェック
	 */
	private void checkStayCountForTeiritsu(FukaDeclarationForm form) {
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();

		long kazeiHakusu = (form.getKazeiHakusu() != null) ? form.getKazeiHakusu() : 0;
		long exemptHakusu = (detail.getExemptStayCount() != null) ? detail.getExemptStayCount() : 0;
		long totalStayCount = (detail.getTotalStayCount() != null) ? detail.getTotalStayCount() : 0;

		if (totalStayCount != (kazeiHakusu + exemptHakusu)) {
			log.error("整合性エラー(定率): 画面合計={}, 課税={} + 対象外={}", totalStayCount, kazeiHakusu, exemptHakusu);
			throw new IllegalArgumentException("総宿泊数が「課税対象」と「対象外」の合計と一致しません。");
		}
	}
}
