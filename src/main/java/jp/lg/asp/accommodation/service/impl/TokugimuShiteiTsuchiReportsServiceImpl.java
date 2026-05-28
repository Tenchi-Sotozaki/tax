package jp.lg.asp.accommodation.service.impl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.TokugimuShiteiTsuchiDto;
import jp.lg.asp.accommodation.service.TokugimuShiteiTsuchiReportsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

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
		log.info("特別徴収義務者指定通知PDF生成開始: shiteiNo={}", dto.getShiteiNo());

		try {
			InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
			JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

			Map<String, Object> params = buildParams(dto);

			JasperPrint jasperPrint = JasperFillManager.fillReport(
					jasperReport, params, new JREmptyDataSource());

			byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);
			log.info("特別徴収義務者指定通知PDF生成完了: size={}bytes", pdf.length);
			return pdf;

		} catch (Exception e) {
			log.error("特別徴収義務者指定通知PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	private Map<String, Object> buildParams(TokugimuShiteiTsuchiDto dto) {
		Map<String, Object> params = new HashMap<>();

		// 発行日
		if (dto.getHakkoYmd() != null) {
			// 和暦変換（令和元年 = 2019年）
			int waYear = dto.getHakkoYmd().getYear() - 2018;
			String warekiDate = String.format("令和%d年%d月%d日",
					waYear,
					dto.getHakkoYmd().getMonthValue(),
					dto.getHakkoYmd().getDayOfMonth());
			params.put("hakkoYmd", warekiDate);
		} else {
			params.put("hakkoYmd", "");
		}

		// 基本情報
		params.put("cityName", dto.getCityName() != null ? dto.getCityName() : "");
		params.put("jorei", dto.getJorei() != null ? dto.getJorei() : "");
		params.put("tokuName", dto.getTokuName() != null ? dto.getTokuName() : "");
		params.put("shiteiNo", dto.getShiteiNo() != null ? dto.getShiteiNo() : "");
		params.put("shisetsuJusho", dto.getShisetsuJusho() != null ? dto.getShisetsuJusho() : "");
		params.put("shisetsuName", dto.getShisetsuName() != null ? dto.getShisetsuName() : "");
		params.put("tokuJusho", dto.getTokuJusho() != null ? dto.getTokuJusho() : "");
		params.put("riyu", dto.getRiyu() != null ? dto.getRiyu() : "");
		params.put("city", dto.getCity() != null ? dto.getCity() : "");

		return params;
	}
}