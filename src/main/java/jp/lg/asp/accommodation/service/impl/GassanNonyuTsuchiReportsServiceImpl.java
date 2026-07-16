package jp.lg.asp.accommodation.service.impl;

import java.io.InputStream;
import java.sql.Date;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.GassanNonyuTsuchiDto;
import jp.lg.asp.accommodation.dto.GassanNonyuTsuchiReportsDto;
import jp.lg.asp.accommodation.service.GassanNonyuTsuchiReportsService;
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
 * 合算申告納入承認通知書帳票 Service 実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GassanNonyuTsuchiReportsServiceImpl implements GassanNonyuTsuchiReportsService {

	private static final String JRXML_PATH = "reports/gassanNonyuTsuchi.jrxml";

	@Override
	public byte[] generateTsuchiPdf(GassanNonyuTsuchiDto dto) {
		try {
			InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
			JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

			Map<String, Object> parameters = buildParameters(dto);
			JRDataSource dataSource = buildDataSource(dto);
			JasperPrint jasperPrint = JasperFillManager.fillReport(
					jasperReport, parameters, dataSource);

			return JasperExportManager.exportReportToPdf(jasperPrint);

		} catch (Exception e) {
			log.error("合算申告納入承認通知書PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	private Map<String, Object> buildParameters(GassanNonyuTsuchiDto dto) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("hakkoYmd", dto.getHakkoYmd() != null
				? dto.getHakkoYmd().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) : "");
		parameters.put("jorei", dto.getJorei() != null ? dto.getJorei() : "");
		parameters.put("city", dto.getCity() != null ? dto.getCity() : "");
		parameters.put("biko", dto.getBiko() != null ? dto.getBiko() : "");
		return parameters;
	}

	private JRDataSource buildDataSource(GassanNonyuTsuchiDto dto) {
		GassanNonyuTsuchiReportsDto reportsDto = new GassanNonyuTsuchiReportsDto();
		reportsDto.setJusho(dto.getTokuJusho() != null ? dto.getTokuJusho() : "");
		reportsDto.setName(dto.getTokuName() != null ? dto.getTokuName() : "");
		reportsDto.setGassan_shitei_no(dto.getGassanShiteiNo() != null ? dto.getGassanShiteiNo() : "");
		reportsDto.setTekiyo_st_ymd(dto.getTekiyoStYmd() != null
				? Date.valueOf(dto.getTekiyoStYmd()) : null);
		reportsDto.setKoin(dto.getKoin());

		List<GassanNonyuTsuchiReportsDto> dataSourceList = Arrays.asList(reportsDto);
		return new JRBeanCollectionDataSource(dataSourceList, false);
	}
}
