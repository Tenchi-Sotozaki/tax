package jp.lg.asp.accommodation.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.constant.FukaConstants;
import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;
import jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto;
import jp.lg.asp.accommodation.dto.FukaMonthlyTallyDto.DailyItem;
import jp.lg.asp.accommodation.dto.FukaTaxDetailDto;
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

		// 1. 合計の不整合チェック
		checkTotalCount(form, messages);

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

		long total = 0L;

		for (FukaTaxDetailDto d : detail.getTaxDetails()) {
			if (FukaConstants.TEIRITSU.getValue().equals(form.getFukaKbn())) {
				long ryokin = (d.getRyokin() != null) ? d.getRyokin() : 0L;
				total += fukaService.calculateTax(form.getFukaKbn(), ryokin, d.getTaxRate(), d.getTaxKenRate());
			} else {
				long count = (d.getHakusu() != null) ? d.getHakusu() : 0L;
				total += fukaService.calculateTax(form.getFukaKbn(), count, d.getTaxRate(), d.getTaxKenRate());
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

		long expectedTotal = calculateExpectedTotal(form);
		long inputTotal = (detail.getTotalPaymentAmount() != null) ? detail.getTotalPaymentAmount() : 0L;

		if (expectedTotal != inputTotal) {
			messages.add(String.format("税額の不一致: 税率から算出した税額合計 = %,d円、入力した税額合計 = %,d円", expectedTotal, inputTotal));
		}
	}

	// ===== 合計チェック =====

	private void checkTotalCount(FukaDeclarationForm form, List<String> messages) {
		long sumHakusu = 0L;
		long sumZeigaku = 0L;
		long sumRyokin = 0L;
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();

		if (detail.getTaxDetails() != null) {
			for (FukaTaxDetailDto taxDetail : detail.getTaxDetails()) {
				if (taxDetail.getHakusu() != null) {
					sumHakusu += taxDetail.getHakusu();
				}
				if (taxDetail.getZeigaku() != null) {
					sumZeigaku += taxDetail.getZeigaku();
				}
				if (FukaConstants.TEIRITSU.getValue().equals(form.getFukaKbn())) {
					if (taxDetail.getRyokin() != null) {
						sumRyokin += taxDetail.getRyokin();
					}
				}
			}
		}
		if (detail.getExemptStayCount() != null) {
			sumHakusu += detail.getExemptStayCount();
		}
		if (FukaConstants.TEIRITSU.getValue().equals(form.getFukaKbn()) && detail.getExemptRyokin() != null) {
			sumRyokin += detail.getExemptRyokin();
		}

		long totalStayCount = (detail.getTotalStayCount() != null) ? detail.getTotalStayCount() : 0;
		if (totalStayCount != sumHakusu) {
			messages.add(String.format("宿泊数の合計不一致: 各区分の合計 = %,d、入力した宿泊数合計 = %,d", sumHakusu, totalStayCount));
		}
		long totalPaymentAmount = (detail.getTotalPaymentAmount() != null) ? detail.getTotalPaymentAmount() : 0;
		if (totalPaymentAmount != sumZeigaku) {
			messages.add(String.format("税額の合計不一致: 各区分の合計 = %,d、入力した税額合計 = %,d", sumZeigaku, totalPaymentAmount));
		}
		if (FukaConstants.TEIRITSU.getValue().equals(form.getFukaKbn())) {
			long kazeiRyokin = (detail.getKazeiRyokin() != null) ? detail.getKazeiRyokin() : 0L;
			if (kazeiRyokin != sumRyokin) {
				messages.add(String.format("料金の合計不一致: 各区分の合計 = %,d、画面の料金 = %,d", sumRyokin, kazeiRyokin));
			}
		}
	}

	// ===== 月計表突合チェック =====

	private void checkTallyVsParentDiscrepancy(FukaDeclarationForm form, List<String> messages) {
		FukaMonthlyTallyDto tally = form.getMonthlyTally();
		FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
		if (tally == null || detail == null || detail.getTaxDetails() == null) {
			return;
		}

		List<DailyItem> dailyItems = tally.getDailyItems();
		if (dailyItems == null || dailyItems.isEmpty()) {
			return;
		}

		int categoryCount = detail.getTaxDetails().size();
		if (categoryCount == 0) {
			return;
		}

		boolean hasCountData = dailyItems.stream()
				.anyMatch(item -> item.getHakusu() != null &&
						item.getHakusu().stream().anyMatch(v -> v != null && v > 0));
		boolean hasAmountData = FukaConstants.TEIRITSU.getValue().equals(form.getFukaKbn()) && dailyItems.stream()
				.anyMatch(item -> item.getRyokin() != null &&
						item.getRyokin().stream().anyMatch(v -> v != null && v > 0));
		boolean hasSogakuData = FukaConstants.TEIRITSU.getValue().equals(form.getFukaKbn()) && dailyItems.stream()
				.anyMatch(item -> item.getSogaku() != null &&
						item.getRyokin().stream().anyMatch(v -> v != null && v > 0));

		if (!hasCountData && !hasAmountData && !hasSogakuData) {
			return;
		}

		// 料金総額
		if (hasSogakuData) {
			for (int cat = 0; cat < categoryCount; cat++) {
				long tallySogakuSum = 0;
				for (DailyItem item : dailyItems) {
					List<Long> sogaku = item.getSogaku();
					if (sogaku != null && sogaku.size() > cat && sogaku.get(cat) != null) {
						tallySogakuSum += sogaku.get(cat);
					}
				}

				FukaTaxDetailDto parentDetail = detail.getTaxDetails().get(cat);
				long parentSogaku = (parentDetail.getRyokinSogaku() != null)
						? parentDetail.getRyokinSogaku().longValue()
						: 0L;
				String label = parentDetail.getLabel();
				if (tallySogakuSum != parentSogaku) {
					messages.add(String.format("月計表と区分「%s」の宿泊料金総額が一致しません。（月計表: %,d円、入力値: %,d円）",
							label, tallySogakuSum, parentSogaku));
				}
			}
		}

		// 宿泊数
		if (hasCountData) {
			for (int cat = 0; cat < categoryCount; cat++) {
				long tallyCountSum = 0L;
				for (DailyItem item : dailyItems) {
					List<Integer> hakusu = item.getHakusu();
					if (hakusu != null && hakusu.size() > cat && hakusu.get(cat) != null) {
						tallyCountSum += hakusu.get(cat);
					}
				}

				FukaTaxDetailDto parentDetail = detail.getTaxDetails().get(cat);
				long parentStayCount = (parentDetail.getHakusu() != null) ? parentDetail.getHakusu() : 0L;
				String label = parentDetail.getLabel();
				if (tallyCountSum != parentStayCount) {
					messages.add(String.format("月計表と区分「%s」の宿泊数が一致しません。（月計表: %,d、入力値: %,d）",
							label, tallyCountSum, parentStayCount));
				}
			}
		}

		// 料金
		if (hasAmountData) {
			for (int cat = 0; cat < categoryCount; cat++) {
				long tallyAmountSum = 0;
				for (DailyItem item : dailyItems) {
					List<Long> amounts = item.getRyokin();
					if (amounts != null && amounts.size() > cat && amounts.get(cat) != null) {
						tallyAmountSum += amounts.get(cat);
					}
				}

				FukaTaxDetailDto parentDetail = detail.getTaxDetails().get(cat);
				long parentRyokin = (parentDetail.getRyokin() != null) ? parentDetail.getRyokin().longValue() : 0L;
				String label = parentDetail.getLabel();
				if (tallyAmountSum != parentRyokin) {
					messages.add(String.format("月計表と区分「%s」の宿泊料金が一致しません。（月計表: %,d円、入力値: %,d円）",
							label, tallyAmountSum, parentRyokin));
				}
			}
		}

		// 免除泊数
		long menjoHakusuSum = dailyItems.stream()
				.mapToInt(item -> item.getMenjoHakusu() != null ? item.getMenjoHakusu() : 0).sum();
		long parentMenjoHakusu = detail.getExemptStayCount() != null ? detail.getExemptStayCount() : 0L;
		if (menjoHakusuSum != parentMenjoHakusu) {
			messages.add(String.format("月計表と区分「課税対象外（免除）」の宿泊数が一致しません。（月計表: %,d、入力値: %,d）",
					menjoHakusuSum, parentMenjoHakusu));
		}

		// 免除料金
		if (FukaConstants.TEIRITSU.getValue().equals(form.getFukaKbn())) {
			long menjoRyokinSum = dailyItems.stream()
					.mapToLong(item -> item.getMenjoRyokin() != null ? item.getMenjoRyokin() : 0L).sum();
			long parentMenjoRyokin = detail.getExemptRyokin() != null ? detail.getExemptRyokin() : 0L;
			if (menjoRyokinSum != parentMenjoRyokin) {
				messages.add(String.format("月計表と区分「課税対象外（免除）」宿泊料金が一致しません。（月計表: %,d、入力値: %,d）",
						menjoRyokinSum, parentMenjoRyokin));
			}
		}

		// 税額
		long zeigakuSum = dailyItems.stream()
				.mapToLong(item -> item.getZeigaku() != null ? item.getZeigaku() : 0L).sum();
		long parentZeigaku = detail.getTotalPaymentAmount() != null ? detail.getTotalPaymentAmount() : 0L;
		if (zeigakuSum != parentZeigaku) {
			messages.add(String.format("月計表と税額が一致しません。（月計表: %,d円、入力値: %,d円）",
					zeigakuSum, parentZeigaku));
		}

	}
}
