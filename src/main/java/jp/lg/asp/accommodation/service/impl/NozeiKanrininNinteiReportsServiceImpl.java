package jp.lg.asp.accommodation.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.chrono.JapaneseChronology;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiDto;
import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiReportsDto;
import jp.lg.asp.accommodation.service.NozeiKanrininNinteiReportsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * 納税管理人選任免除認定（不認定）通知書PDF生成 Service実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NozeiKanrininNinteiReportsServiceImpl implements NozeiKanrininNinteiReportsService {

	private static final String JRXML_PATH = "reports/nozeiKanrininNintei.jrxml";

	@Override
	public byte[] generatePdf(NozeiKanrininNinteiDto dto) {
		if (dto == null) {
	        throw new IllegalArgumentException("帳票データ（DTO）がnullです。");
	    }

	    try {
	        JasperReport jasperReport;
	        try (InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream()) {
	            jasperReport = JasperCompileManager.compileReport(jrxmlStream);
	        }

	        Map<String, Object> parameters = new HashMap<>();
	        parameters.put("net.sf.jasperreports.default.font.name", "IPAex明朝");
	        parameters.put("net.sf.jasperreports.default.pdf.font.name", "IPAex明朝");
	        parameters.put("net.sf.jasperreports.default.pdf.encoding", "Identity-H");
	        parameters.put("net.sf.jasperreports.default.pdf.embedded", "true");

			// 発行日
			if (dto.getHakkoYmd() != null) {
				// 和暦用のフォーマッタを作成
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年M月dd日", Locale.JAPANESE)
						.withChronology(JapaneseChronology.INSTANCE);

				String strDate = dto.getHakkoYmd().format(formatter);
				parameters.put("hakkoYmd", strDate);
			} else {
				parameters.put("hakkoYmd", "");
			}
			
	        parameters.put("jorei", dto.getJorei() != null ? dto.getJorei() : "");
	        parameters.put("city", dto.getCityName() != null ? dto.getCityName() : "");
	        parameters.put("biko", dto.getBiko() != null ? dto.getBiko() : "");
	        parameters.put("nintei", dto.getNintei() != null ? dto.getNintei() : "認定");

	        // field設定
	        NozeiKanrininNinteiReportsDto reportDto = new NozeiKanrininNinteiReportsDto();
	        reportDto.setYubin(dto.getTokuYubin() != null ? dto.getTokuYubin() : "");
	        reportDto.setJusho(dto.getTokuJusho() != null ? dto.getTokuJusho() : "");
	        reportDto.setName(dto.getTokuName() != null ? dto.getTokuName() : "");
	        reportDto.setShisetsuYubin(dto.getShisetsuYubin() != null ? dto.getShisetsuYubin() : "");
	        reportDto.setShisetsuJusho(dto.getShisetsuJusho() != null ? dto.getShisetsuJusho() : "");
	        reportDto.setShisetsuName(dto.getShisetsuName() != null ? dto.getShisetsuName() : "");
	        reportDto.setKoin(dto.getKoin() != null && dto.getKoin().length > 0 ? dto.getKoin() : null);
	       
	        List<NozeiKanrininNinteiReportsDto> dataSourceList = Arrays.asList(reportDto);
	        JRDataSource dataSource = new JRBeanCollectionDataSource(dataSourceList);

	        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
	        return JasperExportManager.exportReportToPdf(jasperPrint);

	    } catch (IOException | JRException e) {
	        log.error("納税管理人選任免除認定通知書PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
	        throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
	    }
	}
}
