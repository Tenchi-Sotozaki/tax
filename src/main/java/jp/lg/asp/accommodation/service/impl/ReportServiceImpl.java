package jp.lg.asp.accommodation.service.impl;

import jp.lg.asp.accommodation.dto.TaxDeclarationForm;
import jp.lg.asp.accommodation.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private static final String JRXML_PATH = "reports/tax-declaration.jrxml";

    // -------------------------------------------------------------------------
    // フォームデータ版（メイン実装）
    // -------------------------------------------------------------------------

    /**
     * TaxDeclarationForm の値を Jasper パラメータにマッピングして PDF を生成する。
     */
    @Override
    public byte[] generateDeclarationPdf(TaxDeclarationForm form) {
        log.info("PDF生成開始: obligorId={}, yearMonth={}",
                form.getObligorId(), form.getTargetYearMonth());
        try {
            InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            Map<String, Object> params = buildParams(form);

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, params, new JREmptyDataSource());

            byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);
            log.info("PDF生成完了: size={}bytes", pdf.length);
            return pdf;

        } catch (Exception e) {
            log.error("PDF生成エラー: obligorId={}", form.getObligorId(), e);
            throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // ID版（後方互換）
    // -------------------------------------------------------------------------

    /**
     * ID のみを受け取る後方互換メソッド。
     * ダミーフォームを生成して委譲する。
     */
    @Override
    public byte[] generateDeclarationPdf(String id) {
        TaxDeclarationForm form = new TaxDeclarationForm();
        form.setObligorId(id);
        return generateDeclarationPdf(form);
    }

    // -------------------------------------------------------------------------
    // パラメータ構築（DTO → Jasper パラメータ マッピング）
    // -------------------------------------------------------------------------

    private Map<String, Object> buildParams(TaxDeclarationForm form) {
        Map<String, Object> params = new HashMap<>();

        // ===== 基本情報（TODO: DB実装後はDTOまたはServiceから取得する） =====
        params.put("obligorName",    "株式会社 テストホテル");
        params.put("obligorAddress", "北海道札幌市中央区北1条西1丁目");
        params.put("obligorNumber",  "1234567890123");
        params.put("obligorPhone",   "011-000-0000");
        params.put("facilityName",   "テストホテル 札幌");
        params.put("facilityAddress","北海道札幌市中央区北1条西1丁目");
        params.put("registrationNo", "12345");
        params.put("submissionYear",  "令和8");
        params.put("submissionMonth", "4");
        params.put("submissionDay",   "1");

        // ===== 3ヶ月分マッピング =====
        for (int i = 0; i < 3; i++) {
            String s = String.valueOf(i + 1);
            TaxDeclarationForm.MonthlyData m = form.getMonth(i);

            // 対象年月（TODO: 和暦変換実装後は変換メソッドを呼び出す）
            if (m.getTargetYearMonth() != null) {
                params.put("targetYear"  + s, "令和" + (m.getTargetYearMonth().getYear() - 2018));
                params.put("targetMonth" + s, String.valueOf(m.getTargetYearMonth().getMonthValue()));
            } else {
                params.put("targetYear"  + s, "");
                params.put("targetMonth" + s, "");
            }

            params.put("countTier1_" + s, nvlInt(m.getGuestCountTier1()));
            params.put("taxTier1_"   + s, nvlLong(m.getTaxAmountTier1()));
            params.put("countTier2_" + s, nvlInt(m.getGuestCountTier2()));
            params.put("taxTier2_"   + s, nvlLong(m.getTaxAmountTier2()));
            params.put("countTier3_" + s, nvlInt(m.getGuestCountTier3()));
            params.put("taxTier3_"   + s, nvlLong(m.getTaxAmountTier3()));
            params.put("taxableCount" + s, nvlInt(m.getTotalGuestCount()));
            params.put("exemptCount"  + s, nvlInt(m.getExemptGuestCount()));
            params.put("totalCount"   + s, nvlInt(m.getTotalGuestCount()));
            params.put("totalTax"     + s, nvlLong(m.getTotalTaxAmount()));
        }

        return params;
    }

    private int nvlInt(Integer value) {
        return value != null ? value : 0;
    }

    private long nvlLong(Long value) {
        return value != null ? value : 0L;
    }
}
