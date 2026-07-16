package jp.lg.asp.accommodation.service.impl;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.TokugimuJuriTsuchiDto;
import jp.lg.asp.accommodation.dto.TokugimuJuriTsuchiReportsDto;
import jp.lg.asp.accommodation.service.TokugimuJuriTsuchiReportsService;
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
 * 特別徴収義務者申請受理通知帳票 Service 実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokugimuJuriTsuchiReportsServiceImpl implements TokugimuJuriTsuchiReportsService {

	private static final String JRXML_PATH = "reports/tokugimuJuriTsuchi.jrxml";

	@Override
	public byte[] generateTsuchiPdf(TokugimuJuriTsuchiDto dto) {
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
			log.error("特別徴収義務者申請受理通知PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	private JRDataSource buildParams(TokugimuJuriTsuchiDto dto) {

		TokugimuJuriTsuchiReportsDto reportsDto = new TokugimuJuriTsuchiReportsDto();

		// 基本情報
		reportsDto.setCityName(dto.getCityName() != null ? dto.getCityName() : "");
		reportsDto.setJorei(dto.getJorei() != null ? dto.getJorei() : "");
		reportsDto.setTokuName(dto.getTokuName() != null ? dto.getTokuName() : "");
		reportsDto.setShiteiNo(dto.getShiteiNo() != null ? dto.getShiteiNo() : "");
		reportsDto.setShisetsuJusho(dto.getShisetsuJusho() != null ? dto.getShisetsuJusho() : "");
		reportsDto.setShisetsuName(dto.getShisetsuName() != null ? dto.getShisetsuName() : "");
		reportsDto.setTokuJusho(dto.getTokuJusho() != null ? dto.getTokuJusho() : "");
		reportsDto.setTokuJushoWithoutYubin(
				dto.getTokuJushoWithoutYubin() != null ? dto.getTokuJushoWithoutYubin() : "");
		reportsDto.setBiko(dto.getBiko() != null ? dto.getBiko() : "");
		reportsDto.setKoin(dto.getKoin() != null && dto.getKoin().length > 0 ? dto.getKoin() : null);

		// 発行日
		if (dto.getHakkoYmd() != null) {
			// フォーマット変換
			String strDate = dto.getHakkoYmd().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
			reportsDto.setHakkoYmd(strDate);
		} else {
			reportsDto.setHakkoYmd("");
		}

		List<TokugimuJuriTsuchiReportsDto> dataSourceList = Arrays.asList(reportsDto);
		JRDataSource params = new JRBeanCollectionDataSource(dataSourceList);

		return params;
	}
}