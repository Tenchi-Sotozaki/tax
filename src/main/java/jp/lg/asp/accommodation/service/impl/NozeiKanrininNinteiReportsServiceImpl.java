package jp.lg.asp.accommodation.service.impl;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiDto;
import jp.lg.asp.accommodation.service.NozeiKanrininNinteiReportsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

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
		try {
			InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
			JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("net.sf.jasperreports.default.font.name", "IPAex明朝");
			parameters.put("net.sf.jasperreports.default.pdf.font.name", "IPAex明朝");
			parameters.put("net.sf.jasperreports.default.pdf.encoding", "Identity-H");
			parameters.put("net.sf.jasperreports.default.pdf.embedded", "true");

			if (dto.getHakkoYmd() != null) {
				parameters.put("hakkoYmd", dto.getHakkoYmd().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
			} else {
				parameters.put("hakkoYmd", "");
			}
			parameters.put("jorei", dto.getJorei() != null ? dto.getJorei() : "");
			parameters.put("city", dto.getCityName() != null ? dto.getCityName() : "");
			parameters.put("biko", dto.getBiko() != null ? dto.getBiko() : "");
			parameters.put("nintei", dto.getNintei() != null ? dto.getNintei() : "認定");

			// jrxmlのフィールド名に合わせたMap
			Map<String, Object> row = new HashMap<>();
			row.put("jusho", dto.getTokuJusho() != null ? dto.getTokuJusho() : "");
			row.put("name", dto.getTokuName() != null ? dto.getTokuName() : "");
			row.put("shisetsu_jusho", dto.getShisetsuJusho() != null ? dto.getShisetsuJusho() : "");
			row.put("shisetsu_name", dto.getShisetsuName() != null ? dto.getShisetsuName() : "");
			row.put("koin", dto.getKoin() != null && dto.getKoin().length > 0 ? dto.getKoin() : null);

			List<Map<String, ?>> dataSourceList = Arrays.asList(row);
			JRDataSource dataSource = new JRMapCollectionDataSource(dataSourceList);

			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
			return JasperExportManager.exportReportToPdf(jasperPrint);

		} catch (Exception e) {
			log.error("納税管理人選任免除認定通知書PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}
}
