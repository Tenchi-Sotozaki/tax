package jp.lg.asp.accommodation.service.impl;

import java.io.InputStream;
import java.time.YearMonth;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.TokureiShiteiCancelDto;
import jp.lg.asp.accommodation.dto.TokureiShiteiReportsDto;
import jp.lg.asp.accommodation.service.TokureiShiteiCancelReportsService;
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
 * 納入申告書の提出期限等の特例適用者指定取消通知帳票 Service 実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokureiShiteiCancelReportsServiceImpl implements TokureiShiteiCancelReportsService {

	private static final String JRXML_PATH = "reports/tokureiShiteiCancel.jrxml";

	@Override
	public byte[] generateTsuchiPdf(TokureiShiteiCancelDto dto) {
		try {
			// 必須チェック等
			if (dto == null) {
				throw new IllegalArgumentException("DTOがnullです。");
			}
			if (dto.getHakkoYmd() == null) {
				throw new IllegalArgumentException("発行年月日は必須です。");
			}
			if (dto.getTekiyoYmd() == null || dto.getTekiyoYmd().isEmpty()) {
				throw new IllegalArgumentException("適用年月日は必須です。");
			}
			if (dto.getJorei() == null || dto.getCity() == null || dto.getRiyu() == null) {
				throw new IllegalArgumentException("必須のテキスト項目がnullです。");
			}
			if (dto.getTokuYubin() == null || dto.getTokuJusho() == null || dto.getTokuName() == null ||
				dto.getShisetsuYubin() == null || dto.getShisetsuJusho() == null || dto.getShisetsuName() == null ||
				dto.getShiteiNo() == null || dto.getBiko() == null) {
				throw new IllegalArgumentException("データソース用の必須項目がnullです。");
			}
			if (dto.getKoin() == null || dto.getKoin().length == 0) {
				throw new IllegalArgumentException("記章（koin）は必須です。");
			}

			InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
			JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

			Map<String, Object> parameters = buildParameters(dto);
			JRDataSource dataSource = buildDataSource(dto);
			JasperPrint jasperPrint = JasperFillManager.fillReport(
					jasperReport, parameters, dataSource);

			return JasperExportManager.exportReportToPdf(jasperPrint);

		} catch (IllegalArgumentException e) {
			log.error("特例適用者指定取消通知PDF生成バリデーションエラー: shiteiNo={}", dto != null ? dto.getShiteiNo() : null, e);
			throw e;
		} catch (Exception e) {
			log.error("特例適用者指定取消通知PDF生成エラー: shiteiNo={}", dto != null ? dto.getShiteiNo() : null, e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	private Map<String, Object> buildParameters(TokureiShiteiCancelDto dto) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("hakkoYmd", dto.getHakkoYmd() != null
			    ? JapaneseDate.from(dto.getHakkoYmd()).format(DateTimeFormatter.ofPattern("GGGGy年M月d日", java.util.Locale.JAPANESE))
			    : "");
		
		// YYYY年M月形式に変換
		String formattedYmd = "";
		if (dto.getTekiyoYmd() != null && !dto.getTekiyoYmd().isEmpty()) {

			YearMonth yearMonth = YearMonth.parse(dto.getTekiyoYmd(), DateTimeFormatter.ofPattern("yyyy-MM"));

			// 和暦変換のために仮の日を代入
			JapaneseDate japaneseDate = JapaneseDate.from(yearMonth.atDay(1));
			
			// 和暦用のフォーマッターを設定
			DateTimeFormatter jpFormatter = DateTimeFormatter.ofPattern("Gy年M月")
					.withChronology(JapaneseChronology.INSTANCE)
					.withLocale(Locale.JAPAN);

			// 和暦にフォーマット
			formattedYmd = japaneseDate.format(jpFormatter);
		}

		parameters.put("tekiyoYmd", formattedYmd);
		parameters.put("jorei", dto.getJorei() != null ? dto.getJorei() : "");
		parameters.put("city", dto.getCity() != null ? dto.getCity() : "");
		parameters.put("riyu", dto.getRiyu() != null ? dto.getRiyu() : "");
		return parameters;
	}

	private JRDataSource buildDataSource(TokureiShiteiCancelDto dto) {
		TokureiShiteiReportsDto reportsDto = new TokureiShiteiReportsDto();
		reportsDto.setYubin(dto.getTokuYubin() != null ? dto.getTokuYubin() : "");
		reportsDto.setJusho(dto.getTokuJusho() != null ? dto.getTokuJusho() : "");
		reportsDto.setName(dto.getTokuName() != null ? dto.getTokuName() : "");
		reportsDto.setShisetsu_yubin(dto.getShisetsuYubin() != null ? dto.getShisetsuYubin() : "");
		reportsDto.setShisetsu_jusho(dto.getShisetsuJusho() != null ? dto.getShisetsuJusho() : "");
		reportsDto.setShisetsu_name(dto.getShisetsuName() != null ? dto.getShisetsuName() : "");
		reportsDto.setShitei_no(dto.getShiteiNo() != null ? dto.getShiteiNo() : "");
		reportsDto.setBiko(dto.getBiko() != null ? dto.getBiko() : ""); 
		reportsDto.setKoin(dto.getKoin() != null && dto.getKoin().length > 0 ? dto.getKoin() : null);
		
		List<TokureiShiteiReportsDto> dataSourceList = Arrays.asList(reportsDto);
		return new JRBeanCollectionDataSource(dataSourceList, false);
	}
}
