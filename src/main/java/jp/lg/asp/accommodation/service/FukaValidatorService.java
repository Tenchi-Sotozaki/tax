package jp.lg.asp.accommodation.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;
import jp.lg.asp.accommodation.dto.FukaTaxDetailDto;
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

		if (StringUtils.hasText(detail.getPaymentYearMonth())) {
			// 💡 賦課区分によってバリデーションロジックを切り替える
			if ("2".equals(form.getFukaKbn())) {
				checkStayCountForTeiritsu(form);
			} else {
				checkStayCountForTeigaku(detail);
			}
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

		// 💡 定率制の合計チェックロジック
		// 課税対象宿泊数 + 課税対象外宿泊数 = 総宿泊数
		long kazeiHakusu = (form.getKazeiHakusu() != null) ? form.getKazeiHakusu() : 0;
		long exemptHakusu = (detail.getExemptStayCount() != null) ? detail.getExemptStayCount() : 0;
		long totalStayCount = (detail.getTotalStayCount() != null) ? detail.getTotalStayCount() : 0;

		if (totalStayCount != (kazeiHakusu + exemptHakusu)) {
			log.error("整合性エラー(定率): 画面合計={}, 課税={} + 対象外={}", totalStayCount, kazeiHakusu, exemptHakusu);
			throw new IllegalArgumentException("総宿泊数が「課税対象」と「対象外」の合計と一致しません。");
		}
	}
}