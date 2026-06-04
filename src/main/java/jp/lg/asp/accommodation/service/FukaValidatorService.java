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
	 * 納入年月と宿泊数、および税額の相関チェックを実行し、
	 * 不整合（ズレ）があれば true、なければ false を返す。（ソフトバリデーション）
	 */
	public boolean hasDiscrepancy(FukaDeclarationForm form) {
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
		if (detail == null) {
			return false;
		}

		FukaKbn kbn = FukaKbn.fromCode(form.getFukaKbn());
		boolean stayCountDiscrepancy = false;

		// 1. 宿泊数の不整合チェック
		if (StringUtils.hasText(detail.getPaymentYearMonth())) {
			if (kbn.isTeiritsu()) {
				stayCountDiscrepancy = hasStayCountDiscrepancyForTeiritsu(form);
			} else {
				stayCountDiscrepancy = hasStayCountDiscrepancyForTeigaku(detail);
			}
		}

		// 2. 税額合計の不整合チェック
		boolean taxTotalDiscrepancy = hasTaxTotalDiscrepancy(form);

		// 3. 月計表と親画面の突合チェック（①宿泊数 ②金額）
		boolean tallyDiscrepancy = false; // TODO: 一時無効化 hasTallyVsParentDiscrepancy(form);

		return stayCountDiscrepancy || taxTotalDiscrepancy || tallyDiscrepancy;
	}

	/**
	 * 明細リストから税額の正解値を再計算し、画面の合計値と比較する。
	 * 不一致の場合は true を返す。
	 * ※ 明細が未入力（stayCountが全てnullまたは0）の場合はチェックをスキップする。
	 */
	private boolean hasTaxTotalDiscrepancy(FukaDeclarationForm form) {
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
		if (detail == null || detail.getTaxDetails() == null || detail.getTaxDetails().isEmpty()) {
			return false;
		}

		// 明細に1件でも入力があるか確認（全未入力ならチェック不要）
		boolean hasInput = detail.getTaxDetails().stream()
				.anyMatch(d -> d.getStayCount() != null && d.getStayCount() > 0);
		// 定率制の場合はkazeiRyokinが入力されているかも確認
		if (!hasInput && (form.getKazeiRyokin() == null || form.getKazeiRyokin() == 0)) {
			return false;
		}

		long expectedTotal = calculateExpectedTotal(form);
		long inputTotal = getInputTotal(form);

		if (expectedTotal != inputTotal) {
			log.warn("税額整合性エラー(警告): 明細からの算出値={}, 画面入力値={}", expectedTotal, inputTotal);
			return true;
		}
		return false;
	}

	/**
	 * 明細リスト（taxDetails）から税額の正解値を算出する。
	 */
	public long calculateExpectedTotal(FukaDeclarationForm form) {
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
		if (detail == null || detail.getTaxDetails() == null || detail.getTaxDetails().isEmpty()) {
			return 0L;
		}

		FukaKbn kbn = FukaKbn.fromCode(form.getFukaKbn());

		if (kbn.isTeiritsu()) {
			BigDecimal rate = detail.getTaxDetails().get(0).getTaxRate();
			if (rate == null) rate = BigDecimal.ZERO;
			
			long ryokin = (form.getKazeiRyokin() != null) ? form.getKazeiRyokin() : 0L;
			return kbn.calculateTax(rate, ryokin);
		} else {
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
	 * 定額制：宿泊数と合計値の整合性チェック。不一致なら true。
	 */
	private boolean hasStayCountDiscrepancyForTeigaku(FukaMonthlyDeclarationDto detail) {
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
			log.warn("整合性エラー(警告・定額): 画面合計={}, 明細積み上げ={}", detail.getTotalStayCount(), sumOfDetails);
			return true;
		}
		return false;
	}

	/**
	 * 定率制：宿泊数と合計値の整合性チェック。不一致なら true。
	 */
	private boolean hasStayCountDiscrepancyForTeiritsu(FukaDeclarationForm form) {
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();

		long kazeiHakusu = (form.getKazeiHakusu() != null) ? form.getKazeiHakusu() : 0;
		long exemptHakusu = (detail.getExemptStayCount() != null) ? detail.getExemptStayCount() : 0;
		long totalStayCount = (detail.getTotalStayCount() != null) ? detail.getTotalStayCount() : 0;

		if (totalStayCount != (kazeiHakusu + exemptHakusu)) {
			log.warn("整合性エラー(警告・定率): 画面合計={}, 課税={} + 対象外={}", totalStayCount, kazeiHakusu, exemptHakusu);
			return true;
		}
		return false;
	}

	/**
	 * 月計表（monthlyTally）の日別合計と、親画面の明細値の突合チェック。
	 * ① 各区分の宿泊数（taxCategoryCounts）の31日合計 vs 親画面のstayCount
	 * ② 定率制の場合: 各区分の宿泊料金（taxCategoryAmounts）の31日合計 vs 親画面のstayCount
	 *   （定率制では stayCount に宿泊料金が入っているため）
	 * 月計表が未入力（全ゼロ）の場合はチェックをスキップする。
	 * 不一致なら true を返す。
	 */
	private boolean hasTallyVsParentDiscrepancy(FukaDeclarationForm form) {
		var tally = form.getMonthlyTally();
		var detail = form.getMonthlyDetail();
		if (tally == null || detail == null || detail.getTaxDetails() == null) {
			return false;
		}

		var dailyItems = tally.getDailyItems();
		if (dailyItems == null || dailyItems.isEmpty()) {
			return false;
		}

		int categoryCount = detail.getTaxDetails().size();
		if (categoryCount == 0) {
			return false;
		}

		FukaKbn kbn = FukaKbn.fromCode(form.getFukaKbn());

		// 月計表に1件でも入力があるか判定（全ゼロならチェック不要）
		boolean hasCountData = dailyItems.stream()
				.anyMatch(item -> item.getTaxCategoryCounts() != null &&
						item.getTaxCategoryCounts().stream().anyMatch(v -> v != null && v > 0));
		boolean hasAmountData = kbn.isTeiritsu() && dailyItems.stream()
				.anyMatch(item -> item.getTaxCategoryAmounts() != null &&
						item.getTaxCategoryAmounts().stream().anyMatch(v -> v != null && v > 0));

		if (!hasCountData && !hasAmountData) {
			return false;
		}

		// ① 宿泊数のチェック (定額制のみ)
		// 定額制: taxCategoryCounts = 宿泊数、stayCount = 宿泊数
		// 定率制: taxCategoryCounts = 宿泊数、stayCount = 宿泊料金（→比較対象が異なるのでスキップ）
		if (hasCountData && kbn.isTeigaku()) {
			for (int cat = 0; cat < categoryCount; cat++) {
				long tallyCountSum = 0;
				for (var item : dailyItems) {
					var counts = item.getTaxCategoryCounts();
					if (counts != null && counts.size() > cat && counts.get(cat) != null) {
						tallyCountSum += counts.get(cat);
					}
				}

				FukaTaxDetailDto parentDetail = detail.getTaxDetails().get(cat);
				long parentStayCount = (parentDetail.getStayCount() != null) ? parentDetail.getStayCount() : 0L;

				log.info("★★★ [定額制①宿泊数] 区分{}: 月計表合計={}, 親画面stayCount={}", cat + 1, tallyCountSum, parentStayCount);

				if (tallyCountSum != parentStayCount) {
					log.warn("月計表突合エラー(宿泊数): 区分{} 月計表合計={}, 親画面stayCount={}", cat + 1, tallyCountSum, parentStayCount);
					return true;
				}
			}
		}

		// ② 宿泊料金のチェック (定率制のみ)
		// 定率制: taxCategoryAmounts = 宿泊料金、stayCount = 宿泊料金（同じ意味）
		if (hasAmountData && kbn.isTeiritsu()) {
			for (int cat = 0; cat < categoryCount; cat++) {
				long tallyAmountSum = 0;
				for (var item : dailyItems) {
					var amounts = item.getTaxCategoryAmounts();
					if (amounts != null && amounts.size() > cat && amounts.get(cat) != null) {
						tallyAmountSum += amounts.get(cat);
					}
				}

				FukaTaxDetailDto parentDetail = detail.getTaxDetails().get(cat);
				// 定率制では stayCount に「宿泊料金」が入っている
				long parentRyokin = (parentDetail.getStayCount() != null) ? parentDetail.getStayCount() : 0L;

				log.info("★★★ [定率制②料金] 区分{}: 月計表合計={}, 親画面stayCount(料金)={}", cat + 1, tallyAmountSum, parentRyokin);

				if (tallyAmountSum != parentRyokin) {
					log.warn("月計表突合エラー(料金): 区分{} 月計表合計={}, 親画面stayCount(料金)={}", cat + 1, tallyAmountSum, parentRyokin);
					return true;
				}
			}
		}

		return false;
	}
}