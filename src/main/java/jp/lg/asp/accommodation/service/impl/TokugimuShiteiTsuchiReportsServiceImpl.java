package jp.lg.asp.accommodation.service.impl;

import java.io.InputStream;
import java.time.chrono.JapaneseChronology;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.TokugimuShiteiTsuchiDto;
import jp.lg.asp.accommodation.dto.TokugimuShiteiTsuchiReportsDto;
import jp.lg.asp.accommodation.service.TokugimuShiteiTsuchiReportsService;
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
 * 特別徴収義務者指定通知帳票 Service 実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokugimuShiteiTsuchiReportsServiceImpl implements TokugimuShiteiTsuchiReportsService {

	private static final String JRXML_PATH = "reports/tokugimuShiteiTsuchi.jrxml";
	
	@Override
	public byte[] generateTsuchiPdf(TokugimuShiteiTsuchiDto dto) {
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
			log.error("特別徴収義務者指定通知PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	private JRDataSource buildParams(TokugimuShiteiTsuchiDto dto) {

		TokugimuShiteiTsuchiReportsDto reportsDto = new TokugimuShiteiTsuchiReportsDto();

		// 基本情報
		reportsDto.setCityName(dto.getCityName() != null ? dto.getCityName() : "");
		reportsDto.setJorei(dto.getJorei() != null ? dto.getJorei() : "");
		reportsDto.setTokuName(dto.getTokuName() != null ? dto.getTokuName() : "");
		reportsDto.setShiteiNo(dto.getShiteiNo() != null ? dto.getShiteiNo() : "");
		reportsDto.setShisetsuYubinNo(dto.getShisetsuYubinNo() != null ? dto.getShisetsuYubinNo() : "");
		reportsDto.setShisetsuJusho(dto.getShisetsuJusho() != null ? dto.getShisetsuJusho() : "");
		reportsDto.setShisetsuName(dto.getShisetsuName() != null ? dto.getShisetsuName() : "");
		reportsDto.setTokuYubinNo(dto.getTokuYubinNo() != null ? dto.getTokuYubinNo() : "");
		reportsDto.setTokuJusho(dto.getTokuJusho() != null ? dto.getTokuJusho() : "");
		reportsDto.setRiyu(dto.getRiyu() != null ? dto.getRiyu() : "");
		reportsDto.setCity(dto.getCity() != null ? dto.getCity() : "");
		reportsDto.setKoin(dto.getKoin() != null && dto.getKoin().length > 0 ? dto.getKoin() : null);

		// 発行日（和暦）
		if (dto.getHakkoYmd() != null) {
			DateTimeFormatter warekiFormatter = DateTimeFormatter
					.ofPattern("GGGGy年M月d日", Locale.JAPANESE)
					.withChronology(JapaneseChronology.INSTANCE);
			reportsDto.setHakkoYmd(dto.getHakkoYmd().format(warekiFormatter));
		} else {
			reportsDto.setHakkoYmd("");
		}

		List<TokugimuShiteiTsuchiReportsDto> dataSourceList = Arrays.asList(reportsDto);
		JRDataSource params = new JRBeanCollectionDataSource(dataSourceList);

		return params;
	}
}