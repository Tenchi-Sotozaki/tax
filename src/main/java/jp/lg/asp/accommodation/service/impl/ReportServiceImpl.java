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
        form.setGuestCountTier1(0);
        form.setGuestCountTier2(0);
        form.setGuestCountTier3(0);
        form.setTaxAmountTier1(200);
        form.setTaxAmountTier2(500);
        form.setTaxAmountTier3(1000);
        form.setTotalGuestCount(0);
        form.setTotalTaxAmount(0);
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
        // TODO: DB実装後は form.getTargetYearMonth() を和暦に変換する
        params.put("submissionYear",  "令和8");
        params.put("submissionMonth", "4");
        params.put("submissionDay",   "1");

        // ===== 月1（申告対象月）: form の値をそのまま使用 =====
        params.put("targetYear1",    "令和8");
        params.put("targetMonth1",   "4");
        params.put("countTier1_1",   nvlInt(form.getGuestCountTier1()));
        params.put("taxTier1_1",     nvlLong(form.getTaxAmountTier1()));
        params.put("countTier2_1",   nvlInt(form.getGuestCountTier2()));
        params.put("taxTier2_1",     nvlLong(form.getTaxAmountTier2()));
        params.put("countTier3_1",   nvlInt(form.getGuestCountTier3()));
        params.put("taxTier3_1",     nvlLong(form.getTaxAmountTier3()));
        params.put("taxableCount1",  nvlInt(form.getTotalGuestCount()));
        params.put("exemptCount1",   0);
        params.put("totalCount1",    nvlInt(form.getTotalGuestCount()));
        params.put("totalTax1",      nvlLong(form.getTotalTaxAmount()));

        // ===== 月2・月3（TODO: DB実装後は複数月データを取得する） =====
        for (int i = 2; i <= 3; i++) {
            String s = String.valueOf(i);
            params.put("targetYear"  + s, "");
            params.put("targetMonth" + s, "");
            params.put("countTier1_" + s, 0);
            params.put("taxTier1_"   + s, 0L);
            params.put("countTier2_" + s, 0);
            params.put("taxTier2_"   + s, 0L);
            params.put("countTier3_" + s, 0);
            params.put("taxTier3_"   + s, 0L);
            params.put("taxableCount" + s, 0);
            params.put("exemptCount"  + s, 0);
            params.put("totalCount"   + s, 0);
            params.put("totalTax"     + s, 0L);
        }

        return params;
    }

    private int nvlInt(Integer value) {
        return value != null ? value : 0;
    }

    private long nvlLong(Integer value) {
        return value != null ? value.longValue() : 0L;
    }
}
