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
@RequiredArgsConstructor // 規約 4.3（コンストラクタインジェクション用）
public class FukaValidatorService {

    // 💡 1ヶ月仕様になったため、3ヶ月制限のマージックナンバーは不要になり削除！
    // private static final int DECLARATION_MONTH_LIMIT = 3;

    /**
     * 納入年月と宿泊数の相関チェックを実行する
     */
    public void validateCorrelation(FukaDeclarationForm form) {
        
        // 💡 修正ポイント：リストのループを廃止し、単一オブジェクトを取得
        MonthlyDeclarationDto detail = form.getMonthlyDetail();

        // 万が一、データが空の場合はチェックをスキップ（通常はあり得ないが安全のため）
        if (detail == null) {
            return;
        }

        // 行コメントはコードの上に（規約 6）
        // 納入年月が入力されている場合のみ、宿泊数の入力チェックを行う
        if (StringUtils.hasText(detail.getPaymentYearMonth())) {
            checkStayCount(detail);
        }
    }

    private void checkStayCount(MonthlyDeclarationDto detail) {
        // 演算子の前後には半角スペース（規約 5）
        boolean hasInput = (detail.getStayCount1() != null) 
                        || (detail.getStayCount2() != null) 
                        || (detail.getStayCount3() != null)
                        || (detail.getExemptStayCount() != null); // 💡 課税対象外の入力もあるかもしれないので追加しておくのがオススメだ！
        
        if (!hasInput) {
            // 例外処理（規約 9: エラーハンドリング推奨）
            throw new IllegalArgumentException("納入年月入力行の宿泊数は必須です。");
        }
    }
}