package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.ConsolidatedDeclarationForm;
import jp.lg.asp.accommodation.dto.PaymentLedgerSearchForm;
import jp.lg.asp.accommodation.dto.PaymentRecordDto;
import jp.lg.asp.accommodation.dto.TaxCycleInfoDto;
import jp.lg.asp.accommodation.dto.TaxDeclarationForm;

import java.util.List;

/**
 * 申告・納付管理 Service インターフェース。
 * DB実装後はこのインターフェースを変えずに実装クラスのみ差し替える。
 */
public interface DeclarationService {

    /** 特別徴収義務者名を取得する */
    String getObligorName(String obligorId);

    /** 納税周期情報を取得する */
    TaxCycleInfoDto getTaxCycleInfo(String obligorId);

    /** 検索条件に合致する納入記録一覧を取得する */
    List<PaymentRecordDto> searchRecords(String obligorId, PaymentLedgerSearchForm form);

    /** 納入記録の合計金額を計算する */
    int calcTotalAmount(List<PaymentRecordDto> records);

    /** 宿泊税申告フォームの初期値を生成する */
    TaxDeclarationForm buildInitialDeclarationForm(String obligorId);

    /** 宿泊税申告を登録する（納入金額の再計算も含む） */
    void registerDeclaration(TaxDeclarationForm form);

    /** 合算申告フォームの初期値を生成する */
    ConsolidatedDeclarationForm buildInitialConsolidatedForm(String obligorId);

    /** 合算申告を登録する */
    void registerConsolidated(ConsolidatedDeclarationForm form);
}
