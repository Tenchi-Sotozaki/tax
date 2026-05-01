package jp.lg.asp.accommodation.service;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.MonthlyDeclarationDto;
import lombok.RequiredArgsConstructor;

/**
 * 宿泊税申告のバリデーションを行うサービス
 */
@Service // 規約 4.2
@RequiredArgsConstructor // 規約 4.3（コンストラクタインジェクション用）[cite: 4]
public class FukaValidatorService {

    // マジックナンバー禁止（規約 8）[cite: 4]
    private static final int DECLARATION_MONTH_LIMIT = 3;

    /**
     * 納入年月と宿泊数の相関チェックを実行する[cite: 4]
     */
    public void validateCorrelation(FukaDeclarationForm form) {
        // 行コメントはコードの上に（規約 6）[cite: 4]
        // 3ヶ月分のブロックをループでチェック
        for (int i = 0; i < form.getMonthlyDetails().size(); i++) {
            MonthlyDeclarationDto detail = form.getMonthlyDetails().get(i);
            
            if (StringUtils.hasText(detail.getPaymentYearMonth())) {
                checkStayCount(detail);
            }
        }
    }

    private void checkStayCount(MonthlyDeclarationDto detail) {
        // 演算子の前後には半角スペース（規約 5）[cite: 4]
        boolean hasInput = (detail.getStayCount1() != null) 
                        || (detail.getStayCount2() != null) 
                        || (detail.getStayCount3() != null);
        
        if (!hasInput) {
            // 例外処理（規約 9: エラーハンドリング推奨）[cite: 4]
            throw new IllegalArgumentException("納入年月入力行の宿泊数は必須です。");
        }
    }
}