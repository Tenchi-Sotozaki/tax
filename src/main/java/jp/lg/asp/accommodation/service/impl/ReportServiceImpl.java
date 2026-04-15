package jp.lg.asp.accommodation.service.impl;

import jp.lg.asp.accommodation.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JREmptyDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private static final String JRXML_PATH = "reports/tax-declaration.jrxml";

    /**
     * 宿泊税申告書をPDF形式で生成する。
     *
     * @param id 申告ID（または特別徴収義務者ID）
     * @return PDF バイト配列
     */
    @Override
    public byte[] generateDeclarationPdf(String id) {
        log.info("PDF生成開始: id={}", id);
        try {
            // 1. JRXMLをコンパイル
            InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            // 2. パラメータを準備
            //    TODO: DB実装後は id でデータを取得してパラメータに詰め替える
            Map<String, Object> params = new HashMap<>();
            params.put("declarationId",      id);
            params.put("obligorName",         "グランドホテル東京（ID:" + id + "）");
            params.put("targetYearMonth",     "2024年8月");
            params.put("taxableGuestCount",   120);
            params.put("taxRate",             200);
            params.put("totalTaxAmount",      24000);

            // 3. レポートを充填（データソースは空）
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, params, new JREmptyDataSource());

            // 4. PDFにエクスポート
            byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);
            log.info("PDF生成完了: id={}, size={}bytes", id, pdf.length);
            return pdf;

        } catch (Exception e) {
            log.error("PDF生成エラー: id={}", id, e);
            throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
        }
    }
}
