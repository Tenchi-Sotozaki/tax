package jp.lg.asp.accommodation.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiDto;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiReportsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * 宿泊税特別徴収事務交付金交付決定通知書 ReportsService実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KofuKetteiTsuchiReportsServiceImpl implements KofuKetteiTsuchiReportsService {

    @Override
    public byte[] generateKofuKetteiTsuchiPdf(KofuKetteiTsuchiDto dto) {
        try {
            log.debug("PDF生成開始 - 指定番号: {}", dto.getShiteiNo());
            log.debug("交付決定額: {}", dto.getKofugaku());
            
            // JasperReportsのIPAex明朝フォント設定
            System.setProperty("net.sf.jasperreports.default.font.name", "IPAex明朝");
            System.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");
            
            // JRXMLファイルをコンパイル
            ClassPathResource resource = new ClassPathResource("reports/kofuKetteiTsuchijrxml.jrxml");
            if (!resource.exists()) {
                throw new RuntimeException("JRXMLファイルが見つかりません: reports/kofuKetteiTsuchijrxml.jrxml");
            }
            
            JasperReport jasperReport = JasperCompileManager.compileReport(resource.getInputStream());
            log.debug("JRXMLファイルのコンパイル完了");

            // パラメータ設定
            Map<String, Object> parameters = new HashMap<>();
            
            // データソース作成
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(java.util.Arrays.asList(dto));
            log.debug("データソース作成完了 - データ件数: 1");

            // レポート生成
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            log.debug("レポート生成完了");

            // PDF出力（IPAex明朝フォント使用）
            byte[] pdfData = JasperExportManager.exportReportToPdf(jasperPrint);
            log.debug("PDF出力完了 - サイズ: {} bytes", pdfData.length);
            
            return pdfData;

        } catch (Exception e) {
            log.error("PDF生成に失敗しました - 指定番号: {}", dto != null ? dto.getShiteiNo() : "null", e);
            throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
        }
    }
}