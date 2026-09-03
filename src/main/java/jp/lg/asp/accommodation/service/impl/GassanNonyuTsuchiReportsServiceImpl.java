package jp.lg.asp.accommodation.service.impl;

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
		// 必須項目の null チェック
		if (dto == null || dto.getHakkoYmd() == null || dto.getJorei() == null
				|| dto.getCity() == null || dto.getBiko() == null
				|| dto.getNonyuKigen() == null || dto.getTokuJusho() == null
				|| dto.getTokuName() == null || dto.getGassanShiteiNo() == null
				|| dto.getTekiyoStYmd() == null) {
			throw new RuntimeException("帳票出力項目が設定されていません。管理者にお問い合わせください。");
		}

		// 公印の未設定チェック（null または 長さ0）
		if (dto.getKoin() == null || dto.getKoin().length == 0) {
			throw new RuntimeException("公印が設定されていません。管理者にお問い合わせください。");
		}

		try {
			InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
			JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

			Map<String, Object> parameters = buildParameters(dto);
			JRDataSource dataSource = buildDataSource(dto);
			JasperPrint jasperPrint = JasperFillManager.fillReport(
					jasperReport, parameters, dataSource);

			return JasperExportManager.exportReportToPdf(jasperPrint);

		} catch (Exception e) {
			// バリデーション例外はそのままスロー、それ以外はラップする
			if (e instanceof RuntimeException && 
					(e.getMessage().contains("必須項目") || e.getMessage().contains("公印"))) {
				throw (RuntimeException) e;
			}
			log.error("合算申告納入承認通知書PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	private Map<String, Object> buildParameters(GassanNonyuTsuchiDto dto) {
		Map<String, Object> parameters = new HashMap<>();
		
		if (dto.getHakkoYmd() != null) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年M月d日", Locale.JAPANESE)
					.withChronology(JapaneseChronology.INSTANCE);
			parameters.put("hakkoYmd", dto.getHakkoYmd().format(formatter));
		} else {
			parameters.put("hakkoYmd", "");
		}
		
		parameters.put("jorei", dto.getJorei() != null ? dto.getJorei() : "");
		parameters.put("city", dto.getCity() != null ? dto.getCity() : "");
		parameters.put("biko", dto.getBiko() != null ? dto.getBiko() : "");
		parameters.put("nonyuKigen", dto.getNonyuKigen() != null ? dto.getNonyuKigen() : "");
		return parameters;
	}

	private JRDataSource buildDataSource(GassanNonyuTsuchiDto dto) {
		GassanNonyuTsuchiReportsDto reportsDto = new GassanNonyuTsuchiReportsDto();
		reportsDto.setJusho(dto.getTokuJusho() != null ? dto.getTokuJusho() : "");
		reportsDto.setName(dto.getTokuName() != null ? dto.getTokuName() : "");
		reportsDto.setGassan_shitei_no(dto.getGassanShiteiNo() != null ? dto.getGassanShiteiNo() : "");
		reportsDto.setKoin(dto.getKoin() != null && dto.getKoin().length > 0 ? dto.getKoin() : null);
		
		if (dto.getTekiyoStYmd() != null) {
			// 和暦のフォーマッタを作成
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年M月", Locale.JAPANESE)
					.withChronology(JapaneseChronology.INSTANCE);
					
			// 和暦の文字列に変換
			String warekiYm = dto.getTekiyoStYmd().format(formatter);
			reportsDto.setTekiyo_st_ymd(warekiYm);
		} else {
			reportsDto.setTekiyo_st_ymd("");
		}

		List<GassanNonyuTsuchiReportsDto> dataSourceList = Arrays.asList(reportsDto);
		return new JRBeanCollectionDataSource(dataSourceList, false);
	}
}