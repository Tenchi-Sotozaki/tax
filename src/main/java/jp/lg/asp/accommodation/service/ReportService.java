package jp.lg.asp.accommodation.service;

/**
 * 帳票出力 Service インターフェース。
 */
public interface ReportService {

    /**
     * 宿泊税申告書をPDF形式で生成する。
     *
     * @param id 申告ID（または特別徴収義務者ID）
     * @return PDF バイト配列
     */
    byte[] generateDeclarationPdf(String id);
}
