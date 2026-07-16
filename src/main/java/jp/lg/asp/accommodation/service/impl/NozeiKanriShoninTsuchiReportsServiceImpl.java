package jp.lg.asp.accommodation.service.impl;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.NozeiKanriShoninTsuchiDto;
import jp.lg.asp.accommodation.dto.NozeiKanriShoninTsuchiReportsDto;
import jp.lg.asp.accommodation.service.NozeiKanriShoninTsuchiReportsService;
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
 * 納税管理人承認(不承認)通知書PDF生成 Service実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NozeiKanriShoninTsuchiReportsServiceImpl implements NozeiKanriShoninTsuchiReportsService {

	private static final String JRXML_PATH = "reports/nozeiKanrininShoninTsuchi.jrxml";

	@Override
	public byte[] generateTsuchiPdf(NozeiKanriShoninTsuchiDto dto) {
		try {
			InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
			JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

			Map<String, Object> parameters = new HashMap<>();
			// フォント設定を明示的に追加
			parameters.put("net.sf.jasperreports.default.font.name", "IPAex明朝");
			parameters.put("net.sf.jasperreports.default.pdf.font.name", "IPAex明朝");
			parameters.put("net.sf.jasperreports.default.pdf.encoding", "Identity-H");
			parameters.put("net.sf.jasperreports.default.pdf.embedded", "true");
			
			JRDataSource dataSource = buildParams(dto);
			JasperPrint jasperPrint = JasperFillManager.fillReport(
					jasperReport, parameters, dataSource);

			byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);
			return pdf;

		} catch (Exception e) {
			log.error("納税管理人承認通知書PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	private JRDataSource buildParams(NozeiKanriShoninTsuchiDto dto) {
		NozeiKanriShoninTsuchiReportsDto reportsDto = new NozeiKanriShoninTsuchiReportsDto();

		// 基本情報
		reportsDto.setCityName(dto.getCityName() != null ? dto.getCityName() : "");
		reportsDto.setJorei(dto.getJorei() != null ? dto.getJorei() : "");
		reportsDto.setTokuName(dto.getTokuName() != null ? dto.getTokuName() : "");
		reportsDto.setTokuJusho(dto.getTokuJusho() != null ? dto.getTokuJusho() : "");
		reportsDto.setShisetsuJusho(dto.getShisetsuJusho() != null ? dto.getShisetsuJusho() : "");
		reportsDto.setShisetsuName(dto.getShisetsuName() != null ? dto.getShisetsuName() : "");
		reportsDto.setNozeiKanriJusho(dto.getNozeiKanriJusho() != null ? dto.getNozeiKanriJusho() : "");
		reportsDto.setNozeiKanriName(dto.getNozeiKanriName() != null ? dto.getNozeiKanriName() : "");
		reportsDto.setRiyu(dto.getRiyu() != null ? dto.getRiyu() : "");
		reportsDto.setKoin(dto.getKoin());

		// 発行日
		if (dto.getHakkoYmd() != null) {
			String strDate = dto.getHakkoYmd().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
			reportsDto.setHakkoYmd(strDate);
		} else {
			reportsDto.setHakkoYmd("");
		}

		List<NozeiKanriShoninTsuchiReportsDto> dataSourceList = Arrays.asList(reportsDto);
		JRDataSource params = new JRBeanCollectionDataSource(dataSourceList);

		return params;
	}
}