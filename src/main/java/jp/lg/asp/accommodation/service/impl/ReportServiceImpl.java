package jp.lg.asp.accommodation.service.impl;

import jp.lg.asp.accommodation.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// TODO: JasperReports 導入後に以下のimportを有効化する
// import net.sf.jasperreports.engine.JasperCompileManager;
// import net.sf.jasperreports.engine.JasperExportManager;
// import net.sf.jasperreports.engine.JasperFillManager;
// import net.sf.jasperreports.engine.JasperPrint;
// import net.sf.jasperreports.engine.JasperReport;
// import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
// import org.springframework.core.io.ClassPathResource;
// import java.io.InputStream;
// import java.util.HashMap;
// import java.util.Map;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    /**
     * 宿泊税申告書をPDF形式で生成する。
     *
     * <p>TODO: JasperReports 導入後に以下の手順で実装する：
     * <ol>
     *   <li>build.gradle に {@code implementation 'net.sf.jasperreports:jasperreports:6.21.0'} を追加</li>
     *   <li>src/main/resources/reports/ に宿泊税申告書.jrxml を配置</li>
     *   <li>下記コメントアウトされた実装コードを有効化する</li>
     * </ol>
     */
    @Override
    public byte[] generateDeclarationPdf(String id) {
        log.info("PDF生成開始: id={}", id);

        // =====================================================================
        // TODO: JasperReports 実装（JRXML配置後に以下を有効化）
        // =====================================================================
        //
        // try {
        //     // 1. JRXMLをコンパイル
        //     InputStream jrxmlStream =
        //         new ClassPathResource("reports/tax-declaration.jrxml").getInputStream();
        //     JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);
        //
        //     // 2. パラメータとデータソースを準備
        //     Map<String, Object> params = new HashMap<>();
        //     params.put("declarationId", id);
        //     params.put("reportTitle", "宿泊税申告書");
        //     // TODO: DB実装後は declarationRepository.findById(id) でデータ取得
        //     List<Object> dataList = List.of(); // ダミー
        //     JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(dataList);
        //
        //     // 3. レポートを充填
        //     JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);
        //
        //     // 4. PDFにエクスポート
        //     return JasperExportManager.exportReportToPdf(jasperPrint);
        //
        // } catch (Exception e) {
        //     log.error("PDF生成エラー: id={}", id, e);
        //     throw new RuntimeException("PDF生成に失敗しました", e);
        // }
        // =====================================================================

        // ダミー実装: 最小限の有効なPDFバイト列を返す
        return buildDummyPdf(id);
    }

    /**
     * ダミーPDFを生成する（JasperReports導入前の暫定実装）。
     * 最小限の PDF 1.4 構造を持つバイト列を返す。
     */
    private byte[] buildDummyPdf(String id) {
        String content = "宿泊税申告書 (ID: " + id + ") - ダミー出力";
        String pdf = "%PDF-1.4\n"
            + "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
            + "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"
            + "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842]\n"
            + "   /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n"
            + "4 0 obj\n<< /Length " + (content.length() + 30) + " >>\nstream\n"
            + "BT /F1 14 Tf 50 750 Td (" + content + ") Tj ET\n"
            + "endstream\nendobj\n"
            + "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"
            + "xref\n0 6\n0000000000 65535 f\n"
            + "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n0\n%%EOF";
        return pdf.getBytes();
    }
}
