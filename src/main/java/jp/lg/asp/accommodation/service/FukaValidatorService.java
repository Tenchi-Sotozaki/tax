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
@Service // 規約 4.2
@RequiredArgsConstructor // 規約 4.3（コンストラクタインジェクション用）

public class FukaValidatorService {

    // 💡 1ヶ月仕様になったため、3ヶ月制限のマージックナンバーは不要になり削除！
    // private static final int DECLARATION_MONTH_LIMIT = 3;

    /**
     * 納入年月と宿泊数の相関チェックを実行する
     */
	public void validateCorrelation(FukaDeclarationForm form) {
        FukaMonthlyDeclarationDto detail = form.getMonthlyDetail();
        if (detail == null) return;

        if (StringUtils.hasText(detail.getPaymentYearMonth())) {
            checkStayCount(detail);
        }
    }

	/**
	 * 宿泊数と合計値の整合性チェック
	 */
	private void checkStayCount(FukaMonthlyDeclarationDto detail) {
	    long sumOfDetails = 0;
	    
	    // 1. 各税区分の宿泊数を加算
	    if (detail.getTaxDetails() != null) {
	        for (FukaTaxDetailDto taxDetail : detail.getTaxDetails()) {
	            if (taxDetail.getStayCount() != null) {
	                sumOfDetails += taxDetail.getStayCount();
	            }
	        }
	    }
	    
	    // 2. 課税対象外の宿泊数を加算
	    if (detail.getExemptStayCount() != null) {
	        sumOfDetails += detail.getExemptStayCount();
	    }
	    
	    // 3. 画面から送られてきた「合計値」と比較
	    // 💡 プログラムが正しくても、届いたデータがズレていればここで弾く
	    if (detail.getTotalStayCount() == null || detail.getTotalStayCount() != sumOfDetails) {
	        log.error("整合性エラー: 画面合計={}, 明細積み上げ={}", detail.getTotalStayCount(), sumOfDetails);
	        throw new IllegalArgumentException("宿泊数の合計値が明細と一致しません。再計算してください。");
	    }
	}
    
}