package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.TaxDeclarationForm;

/**
 * 帳票出力 Service インターフェース。
 */
public interface ReportService {

    /**
     * 宿泊税申告書をPDF形式で生成する（フォームデータ版）。
     *
     * @param form 申告フォーム（3段階税率データを含む）
     * @return PDF バイト配列
     */
    byte[] generateDeclarationPdf(TaxDeclarationForm form);

    /**
     * 宿泊税申告書をPDF形式で生成する（ID版・後方互換）。
     *
     * @param id 申告ID
     * @return PDF バイト配列
     */
    byte[] generateDeclarationPdf(String id);
}
