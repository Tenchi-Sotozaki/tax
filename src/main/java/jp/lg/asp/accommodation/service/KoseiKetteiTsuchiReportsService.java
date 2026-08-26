package jp.lg.asp.accommodation.service;

import java.util.List;

/**
 * 更正・決定通知書 ReportsService インターフェース
 */
public interface KoseiKetteiTsuchiReportsService {

    List<String> findTaishoYmList(String shiteiNo);

    /**
     * 更正・決定通知書PDFを生成
     * @param shiteiNo 指定番号
     * @param b1Ym 対象月b1（YYYYMM）
     * @param b2Ym 対象月b2（YYYYMM、任意）
     * @param b3Ym 対象月b3（YYYYMM、任意）
     * @return PDFデータ
     */
    byte[] generatePdf(String shiteiNo, String b1Ym, String b2Ym, String b3Ym, String henkoKbn);
}
