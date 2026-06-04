package jp.lg.asp.accommodation.service.impl;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.NonyushoDto;
import jp.lg.asp.accommodation.dto.NonyushoReportsDto;
import jp.lg.asp.accommodation.service.NonyushoReportsService;
import jp.lg.asp.accommodation.service.TokugimuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * 納入書レポート Service実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NonyushoReportsServiceImpl implements NonyushoReportsService {

    private static final String JRXML_PATH = "reports/nonyusho.jrxml";
    
    private final TokugimuService tokugimuService;

    @Override
    public byte[] generateNonyushoPdf(NonyushoDto dto) {
        try {
            InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            Map<String, Object> parameters = new HashMap<>();
            JRDataSource dataSource = buildParams(dto);
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, parameters, dataSource);

            byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);
            return pdf;

        } catch (Exception e) {
            log.error("納入書PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
            throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
        }
    }

    private JRDataSource buildParams(NonyushoDto dto) {
        NonyushoReportsDto reportsDto = new NonyushoReportsDto();

        // 基本情報
        reportsDto.setCityName(dto.getCityName() != null ? dto.getCityName() : "");
        reportsDto.setJichitaiCd(dto.getJichitaiCd() != null ? dto.getJichitaiCd() : "");
        reportsDto.setKozaNo(dto.getKozaNo() != null ? dto.getKozaNo() : "");
        reportsDto.setKozaName(dto.getKozaName() != null ? dto.getKozaName() : "");
        reportsDto.setNendo(dto.getNendo() != null ? dto.getNendo() : "");
        reportsDto.setShiteiNo(dto.getShiteiNo() != null ? dto.getShiteiNo() : "");
        reportsDto.setZeigaku(dto.getZeigaku() != null ? dto.getZeigaku() : "");
        reportsDto.setEntai(dto.getEntai() != null ? dto.getEntai() : "");
        reportsDto.setKasan(dto.getKasan() != null ? dto.getKasan() : "");
        reportsDto.setGokei(dto.getGokei() != null ? dto.getGokei() : "");
        
        // 住所に郵便番号を連結
        String tokuJusho = dto.getTokuJusho() != null ? dto.getTokuJusho().trim() : "";
        String tokuYubinNo = dto.getTokuYubinNo() != null ? dto.getTokuYubinNo().trim() : "";
        
        // 郵便番号がある場合は住所の先頭に付加
        if (!tokuYubinNo.isEmpty()) {
            // 郵便番号のフォーマットを確認し、必要に応じてハイフンを追加
            if (tokuYubinNo.matches("\\d{7}")) {
                tokuYubinNo = tokuYubinNo.substring(0, 3) + "-" + tokuYubinNo.substring(3);
            }
            // 郵便番号を先頭に追加（住所が空でも郵便番号は表示）
            tokuJusho = "〒" + tokuYubinNo + (tokuJusho.isEmpty() ? "" : " " + tokuJusho);
        }
        reportsDto.setTokuJusho(tokuJusho);
        
        reportsDto.setTokuName(dto.getTokuName() != null ? dto.getTokuName() : "");
        reportsDto.setNonyuBasho(dto.getNonyuBasho() != null ? dto.getNonyuBasho() : "");
        reportsDto.setShiteiKinyuName(dto.getShiteiKinyuName() != null ? dto.getShiteiKinyuName() : "");
        reportsDto.setTorimatome(dto.getTorimatome() != null ? dto.getTorimatome() : "");
        reportsDto.setJichitaiKoin(dto.getJichitaiKoin());

        // 申告年月
        if (dto.getShinkokuYmd() != null) {
            String strDate = dto.getShinkokuYmd().format(DateTimeFormatter.ofPattern("yyyy年M月"));
            reportsDto.setShinkokuYm(strDate);
        } else {
            reportsDto.setShinkokuYm("");
        }

        // 納期限
        if (dto.getNokigen() != null) {
            String strDate = dto.getNokigen().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
            reportsDto.setNokigen(strDate);
        } else {
            reportsDto.setNokigen("");
        }

        List<NonyushoReportsDto> dataSourceList = Arrays.asList(reportsDto);
        JRDataSource params = new JRBeanCollectionDataSource(dataSourceList);

        return params;
    }
}