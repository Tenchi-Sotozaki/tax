package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import jp.lg.asp.accommodation.constant.FukaConstants;
import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;
import jp.lg.asp.accommodation.dto.FukaTaxDetailDto;
import jp.lg.asp.accommodation.entity.Zeiritsu;
import jp.lg.asp.accommodation.entity.ZeiritsuTeigaku;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsu;
import jp.lg.asp.accommodation.repository.ZeiritsuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.FukaService;
import jp.lg.asp.accommodation.service.FukaValidatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宿泊税申告のバリデーションを行うサービス
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FukaValidatorServiceImpl implements FukaValidatorService {

	private final FukaService fukaService;
	private final ZeiritsuRepository zeiritsuRepository;
	private final ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;
	private final ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	/**
	 * 不整合があれば true を返す。（既存互換）
	 */
	public boolean hasDiscrepancy(FukaDeclarationForm form) {
		return !getDiscrepancyMessages(form).isEmpty();
	}

	/**
	 * 不整合の詳細メッセージリストを返す。空リストなら不整合なし。
	 */
	public List<String> getDiscrepancyMessages(FukaDeclarationForm form) {
		List<String> messages = new ArrayList<>();
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
		if (detail == null) {
			return messages;
		}

		FukaConstants kbn = FukaConstants.getFukaHoshiki(form.getFukaKbn());

		// 1. 宿泊数の不整合チェック
		if (StringUtils.hasText(detail.getPaymentYearMonth())) {
			if (FukaConstants.TEIRITSU.equals(kbn)) {
				checkStayCountForTeiritsu(form, messages);
			} else {
				checkStayCountForTeigaku(detail, messages);
			}
		}

		// 2. 税額合計の不整合チェック
		checkTaxTotalDiscrepancy(form, messages);

		// 3. 月計表と親画面の突合チェック
		checkTallyVsParentDiscrepancy(form, messages);

		return messages;
	}

	/**
	 * 明細リスト（taxDetails）から税額の正解値を算出する。
	 */
	public long calculateExpectedTotal(FukaDeclarationForm form) {
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
		if (detail == null || detail.getTaxDetails() == null || detail.getTaxDetails().isEmpty()) {
			return 0L;
		}

		FukaConstants kbn = FukaConstants.getFukaHoshiki(form.getFukaKbn());
		long total = 0L;

		for (FukaTaxDetailDto d : detail.getTaxDetails()) {
			BigDecimal rate = (d.getTaxRate() != null) ? d.getTaxRate() : BigDecimal.ZERO;

			if (FukaConstants.TEIRITSU.equals(kbn)) {
				long ryokin = (d.getKazeiRyokin() != null) ? d.getKazeiRyokin().longValue() : 0L;
				total += fukaService.calculateTax(form.getFukaKbn(), ryokin, rate);
			} else {
				long count = (d.getStayCount() != null) ? d.getStayCount() : 0L;
				total += fukaService.calculateTax(form.getFukaKbn(), count, rate);
			}
		}
		return total;
	}

	// ===== 税額チェック =====

	private void checkTaxTotalDiscrepancy(FukaDeclarationForm form, List<String> messages) {
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
		if (detail == null || detail.getTaxDetails() == null || detail.getTaxDetails().isEmpty()) {
			return;
		}

		FukaConstants kbn = FukaConstants.getFukaHoshiki(form.getFukaKbn());

		boolean hasInput;
		if (FukaConstants.TEIRITSU.equals(kbn)) {
			hasInput = detail.getTaxDetails().stream()
					.anyMatch(d -> d.getKazeiRyokin() != null && d.getKazeiRyokin() > 0);
		} else {
			hasInput = detail.getTaxDetails().stream()
					.anyMatch(d -> d.getStayCount() != null && d.getStayCount() > 0);
		}
		if (!hasInput) {
			return;
		}

		long expectedTotal = calculateExpectedTotal(form);
		long inputTotal = (detail.getTotalPaymentAmount() != null) ? detail.getTotalPaymentAmount() : 0L;

		if (expectedTotal != inputTotal) {
			messages.add(String.format("税額の不一致: 明細から計算した税額合計 = %,d円、画面入力の総税額 = %,d円", expectedTotal, inputTotal));
		}
	}

	// ===== 宿泊数チェック（定額制） =====

	private void checkStayCountForTeigaku(FukaMonthlyDeclarationDto detail, List<String> messages) {
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

		long totalStayCount = (detail.getTotalStayCount() != null) ? detail.getTotalStayCount() : 0;

		if (totalStayCount != sumOfDetails) {
			messages.add(String.format("宿泊数の不一致: 各区分の合計 = %,d人泊、画面の総宿泊数 = %,d人泊", sumOfDetails, totalStayCount));
		}
	}

	// ===== 宿泊数チェック（定率制） =====

	private void checkStayCountForTeiritsu(FukaDeclarationForm form, List<String> messages) {
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();

		long sumOfDetails = 0;
		if (detail.getTaxDetails() != null) {
			for (FukaTaxDetailDto taxDetail : detail.getTaxDetails()) {
				if (taxDetail.getStayCount() != null) {
					sumOfDetails += taxDetail.getStayCount();
				}
			}
		}

		long exemptHakusu = (detail.getExemptStayCount() != null) ? detail.getExemptStayCount() : 0;
		long totalStayCount = (detail.getTotalStayCount() != null) ? detail.getTotalStayCount() : 0;

		if (totalStayCount != (sumOfDetails + exemptHakusu)) {
			messages.add(String.format("宿泊数の不一致: 区分合計(%,d) + 対象外(%,d) = %,d人泊、画面の総宿泊数 = %,d人泊",
					sumOfDetails, exemptHakusu, sumOfDetails + exemptHakusu, totalStayCount));
		}
	}

	// ===== 月計表突合チェック =====

	private void checkTallyVsParentDiscrepancy(FukaDeclarationForm form, List<String> messages) {
		var tally = form.getMonthlyTally();
		var detail = form.getMonthlyDetail();
		if (tally == null || detail == null || detail.getTaxDetails() == null) {
			return;
		}

		var dailyItems = tally.getDailyItems();
		if (dailyItems == null || dailyItems.isEmpty()) {
			return;
		}

		int categoryCount = detail.getTaxDetails().size();
		if (categoryCount == 0) {
			return;
		}

		FukaConstants kbn = FukaConstants.getFukaHoshiki(form.getFukaKbn());
		List<String> categoryNames = resolveCategoryNames(form.getFukaKbn(), categoryCount);

		boolean hasCountData = dailyItems.stream()
				.anyMatch(item -> item.getTaxCategoryCounts() != null &&
						item.getTaxCategoryCounts().stream().anyMatch(v -> v != null && v > 0));
		boolean hasAmountData = FukaConstants.TEIRITSU.equals(kbn) && dailyItems.stream()
				.anyMatch(item -> item.getTaxCategoryAmounts() != null &&
						item.getTaxCategoryAmounts().stream().anyMatch(v -> v != null && v > 0));

		if (!hasCountData && !hasAmountData) {
			return;
		}

		// ① 宿泊数: 月計表合計 vs 親画面stayCount
		if (hasCountData) {
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
				String label = categoryNames.get(cat);

				if (tallyCountSum != parentStayCount) {
					messages.add(String.format("月計表と親画面で、%s の宿泊数が一致しません。（月計表: %,d人泊、親画面: %,d人泊）",
							label, tallyCountSum, parentStayCount));
				}
			}
		}

		// ② 宿泊料金: 月計表合計 vs 親画面kazeiRyokin（定率制のみ）
		if (hasAmountData && FukaConstants.TEIRITSU.equals(kbn)) {
			for (int cat = 0; cat < categoryCount; cat++) {
				long tallyAmountSum = 0;
				for (var item : dailyItems) {
					var amounts = item.getTaxCategoryAmounts();
					if (amounts != null && amounts.size() > cat && amounts.get(cat) != null) {
						tallyAmountSum += amounts.get(cat);
					}
				}

				FukaTaxDetailDto parentDetail = detail.getTaxDetails().get(cat);
				long parentRyokin = (parentDetail.getKazeiRyokin() != null) ? parentDetail.getKazeiRyokin().longValue() : 0L;
				String label = categoryNames.get(cat);

				if (tallyAmountSum != parentRyokin) {
					messages.add(String.format("月計表と親画面で、%s の宿泊料金が一致しません。（月計表: %,d円、親画面: %,d円）",
							label, tallyAmountSum, parentRyokin));
				}
			}
		}
	}

	/**
	 * 月計表（dailyItems）の合計と親画面（monthlyDetail）の入力値を突合し、
	 * 不一致があればBindingResultにエラーを追加する。
	 */
	public void validateTallyVsParent(FukaDeclarationForm form, BindingResult result) {
		var tally = form.getMonthlyTally();
		var detail = form.getMonthlyDetail();
		if (tally == null || detail == null || detail.getTaxDetails() == null) {
			return;
		}

		var dailyItems = tally.getDailyItems();
		if (dailyItems == null || dailyItems.isEmpty()) {
			return;
		}

		int categoryCount = detail.getTaxDetails().size();
		if (categoryCount == 0) {
			return;
		}

		FukaConstants kbn = FukaConstants.getFukaHoshiki(form.getFukaKbn());
		List<String> categoryNames = resolveCategoryNames(form.getFukaKbn(), categoryCount);
		List<String> errors = new ArrayList<>();

		// 1. 宿泊数の比較（定額制・定率制共通）
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
			String label = categoryNames.get(cat);

			if (tallyCountSum != parentStayCount) {
				errors.add(String.format("月計表と親画面で、%s の宿泊数が一致しません。（月計表: %,d人泊、親画面: %,d人泊）",
						label, tallyCountSum, parentStayCount));
			}
		}

		// 2. 料金の比較（定率制のみ）
		if (FukaConstants.TEIRITSU.equals(kbn)) {
			for (int cat = 0; cat < categoryCount; cat++) {
				long tallyAmountSum = 0;
				for (var item : dailyItems) {
					var amounts = item.getTaxCategoryAmounts();
					if (amounts != null && amounts.size() > cat && amounts.get(cat) != null) {
						tallyAmountSum += amounts.get(cat);
					}
				}

				FukaTaxDetailDto parentDetail = detail.getTaxDetails().get(cat);
				long parentRyokin = (parentDetail.getKazeiRyokin() != null) ? parentDetail.getKazeiRyokin().longValue() : 0L;
				String label = categoryNames.get(cat);

				if (tallyAmountSum != parentRyokin) {
					errors.add(String.format("月計表と親画面で、%s の宿泊料金が一致しません。（月計表: %,d円、親画面: %,d円）",
							label, tallyAmountSum, parentRyokin));
				}
			}
		}

		// 3. 免除宿泊数の比較（共通）
		long tallyExemptSum = 0;
		for (var item : dailyItems) {
			if (item.getExemptCount() != null) {
				tallyExemptSum += item.getExemptCount();
			}
		}
		long parentExempt = (detail.getExemptStayCount() != null) ? detail.getExemptStayCount() : 0L;
		if (tallyExemptSum != parentExempt) {
			errors.add(String.format("月計表と親画面で、免除宿泊数が一致しません。（月計表: %,d人泊、親画面: %,d人泊）",
					tallyExemptSum, parentExempt));
		}

		// エラーがあればBindingResultに追加
		if (!errors.isEmpty()) {
			String message = String.join("\n", errors);
			result.reject("mismatch.tally", message);
		}
	}

	/**
	 * DBの税率マスタから区分名リストを取得する。
	 * 取得できない場合は「対象区分」をフォールバックとして使用する。
	 */
	private List<String> resolveCategoryNames(String fukaKbn, int categoryCount) {
		List<String> names = new ArrayList<>();

		try {
			if ("2".equals(fukaKbn)) {
				// 定率制: m_zeiritsu_teiritsu から区分名を取得
				List<Zeiritsu> parents = zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd);
				Zeiritsu applied = parents.stream()
						.filter(z -> "2".equals(z.getTaishoKbn()) && "2".equals(z.getFukaKbn()))
						.findFirst().orElse(null);

				if (applied != null) {
					List<ZeiritsuTeiritsu> masters = zeiritsuTeiritsuRepository
							.findActiveByTaishoKbnAndTekiyoYm(applied.getTaishoKbn(),
									applied.getTekiyoStYm(), applied.getTekiyoEdYm());
					for (ZeiritsuTeiritsu m : masters) {
						names.add(m.getKbnName() != null ? m.getKbnName() : "対象区分");
					}
				}
			} else {
				// 定額制: m_zeiritsu_teigaku から区分名（料金帯ラベル）を取得
				List<ZeiritsuTeigaku> masters = zeiritsuTeigakuRepository.findByJichitaiCdOrderByRyokinStAsc(jichitaiCd);
				for (ZeiritsuTeigaku m : masters) {
					String label = (m.getRyokinEd() != null)
							? String.format("%,d円〜%,d円未満", m.getRyokinSt(), m.getRyokinEd() + 1)
							: String.format("%,d円以上", m.getRyokinSt());
					names.add(label);
				}
			}
		} catch (Exception e) {
			log.warn("区分名の取得に失敗しました。フォールバック値を使用します: {}", e.getMessage());
		}

		// 足りない分をフォールバックで埋める
		while (names.size() < categoryCount) {
			names.add("対象区分");
		}

		return names;
	}

}
