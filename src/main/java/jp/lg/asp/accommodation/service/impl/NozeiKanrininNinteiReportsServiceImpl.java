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
		if (dto.getHakkoYmd() == null) {
			throw new IllegalArgumentException("発行日は必須です。");
		}
		if (dto.getJorei() == null) {
			throw new IllegalArgumentException("条例文の設定が未完了です。管理者にお問い合わせください。");
		}
		if (dto.getCityName() == null) {
			throw new IllegalArgumentException("条例文の設定が未完了です。管理者にお問い合わせください。");
		}
		if (dto.getNintei() == null) {
			throw new IllegalArgumentException("認定区分は必須です。");
		}
		if (dto.getTokuYubin() == null || dto.getTokuJusho() == null || dto.getTokuName() == null) {
			throw new IllegalArgumentException("特別徴収義務者情報が取得できませんでした。");
		}
		if (dto.getShisetsuYubin() == null || dto.getShisetsuJusho() == null || dto.getShisetsuName() == null) {
			throw new IllegalArgumentException("施設情報が取得できませんでした。");
		}
		if (dto.getKoin() == null || dto.getKoin().length == 0) {
			throw new IllegalArgumentException("公印が未設定です。管理者にお問い合わせください。");
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

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年M月dd日", Locale.JAPANESE)
					.withChronology(JapaneseChronology.INSTANCE);
			parameters.put("hakkoYmd", dto.getHakkoYmd().format(formatter));
	        parameters.put("jorei", dto.getJorei());
	        parameters.put("city", dto.getCityName());
	        parameters.put("biko", dto.getBiko() != null ? dto.getBiko() : "");
	        parameters.put("nintei", dto.getNintei());

	        // field設定
	        NozeiKanrininNinteiReportsDto reportDto = new NozeiKanrininNinteiReportsDto();
	        reportDto.setYubin(dto.getTokuYubin());
	        reportDto.setJusho(dto.getTokuJusho());
	        reportDto.setName(dto.getTokuName());
	        reportDto.setShisetsuYubin(dto.getShisetsuYubin());
	        reportDto.setShisetsuJusho(dto.getShisetsuJusho());
	        reportDto.setShisetsuName(dto.getShisetsuName());
	        reportDto.setKoin(dto.getKoin());
	       
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
