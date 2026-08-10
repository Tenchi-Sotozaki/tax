package jp.lg.asp.accommodation.service.impl;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.TokureiShiteiDto;
import jp.lg.asp.accommodation.dto.TokureiShiteiReportsDto;
import jp.lg.asp.accommodation.service.TokureiShiteiReportsService;
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
 * 納入申告書の提出期限等の特例適用者指定通知帳票 Service 実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokureiShiteiReportsServiceImpl implements TokureiShiteiReportsService {

	private static final String JRXML_PATH = "reports/tokureiShitei.jrxml";

	@Override
	public byte[] generateTsuchiPdf(TokureiShiteiDto dto) {
		try {
			InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
			JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

			Map<String, Object> parameters = buildParameters(dto);
			JRDataSource dataSource = buildDataSource(dto);
			JasperPrint jasperPrint = JasperFillManager.fillReport(
					jasperReport, parameters, dataSource);

			byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);
			return pdf;

		} catch (Exception e) {
			log.error("特例適用者指定通知PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	private Map<String, Object> buildParameters(TokureiShiteiDto dto) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("hakkoYmd", dto.getHakkoYmd() != null
				? dto.getHakkoYmd().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) : "");
		parameters.put("tekiyoYmd", dto.getTekiyoYmd() != null
				? dto.getTekiyoYmd().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) : "");
		parameters.put("shonin", dto.getShonin() != null ? dto.getShonin() : "");
		parameters.put("jorei", dto.getJorei() != null ? dto.getJorei() : "");
		parameters.put("city", dto.getCity() != null ? dto.getCity() : "");
		parameters.put("riyu", dto.getRiyu() != null ? dto.getRiyu() : "");
		return parameters;
	}

	private JRDataSource buildDataSource(TokureiShiteiDto dto) {
		TokureiShiteiReportsDto reportsDto = new TokureiShiteiReportsDto();
		reportsDto.setJusho(dto.getTokuJusho() != null ? dto.getTokuJusho() : "");
		reportsDto.setName(dto.getTokuName() != null ? dto.getTokuName() : "");
		reportsDto.setShisetsu_jusho(dto.getShisetsuJusho() != null ? dto.getShisetsuJusho() : "");
		reportsDto.setShisetsu_name(dto.getShisetsuName() != null ? dto.getShisetsuName() : "");
		reportsDto.setShitei_no(dto.getShiteiNo() != null ? dto.getShiteiNo() : "");
		reportsDto.setBiko("");
		reportsDto.setKoin(dto.getKoin() != null && dto.getKoin().length > 0 ? dto.getKoin() : null);
		reportsDto.setShonin(dto.getShonin() != null ? dto.getShonin() : null);

		List<TokureiShiteiReportsDto> dataSourceList = Arrays.asList(reportsDto);
		return new JRBeanCollectionDataSource(dataSourceList, false);
	}
}
