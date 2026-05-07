package jp.lg.asp.accommodation.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;
import jp.lg.asp.accommodation.dto.FukaTaxDetailDto;
import lombok.RequiredArgsConstructor;

/**
 * 宿泊税申告のバリデーションを行うサービス
 */
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

    private void checkStayCount(FukaMonthlyDeclarationDto detail) {
        boolean hasInput = false;
        
        // 1. リスト化された税区分の宿泊数をチェック
        if (detail.getTaxDetails() != null) {
            for (FukaTaxDetailDto taxDetail : detail.getTaxDetails()) {
                if (taxDetail.getStayCount() != null) {
                    hasInput = true;
                    break; // 1つでも入力があればOK
                }
            }
        }
        
        // 2. 課税対象外の宿泊数もチェック
        if (detail.getExemptStayCount() != null) {
            hasInput = true;
        }
        
        if (!hasInput) {
            throw new IllegalArgumentException("納入年月入力行の宿泊数は必須です。");
        }
    }
}