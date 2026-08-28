package jp.lg.asp.accommodation.service.impl;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.NonyushoDataResponse;
import jp.lg.asp.accommodation.dto.NonyushoDto;
import jp.lg.asp.accommodation.dto.NonyushoReportsDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.ReportsDefId;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.service.NonyushoReportsService;
import jp.lg.asp.accommodation.service.TokugimuService;
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
 * 納入書レポート Service実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NonyushoReportsServiceImpl implements NonyushoReportsService {

	private static final String JRXML_PATH = "reports/nonyusho.jrxml";

	private final TokugimuService tokugimuService;
	private final FukaRepository fukaRepository;
	private final JichitaiRepository jichitaiRepository;
	private final ReportsDefRepository reportsDefRepository;

	private final JichitaiContext jichitaiContext;

	@Override
	public byte[] generateNonyushoPdf(NonyushoDto dto) {
		try {
			InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
			JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

			Map<String, Object> parameters = new HashMap<>();
			JRDataSource dataSource = buildParams(dto);
			
			// 賦課情報が見つからない
			if (dataSource == null) {
				throw new RuntimeException("賦課情報が見つかりません。");
			}
			
			JasperPrint jasperPrint = JasperFillManager.fillReport(
					jasperReport, parameters, dataSource);

			byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);
			return pdf;

		} catch (Exception e) {
			log.error("納入書PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	/**
	 * 納入書動的データ取得（新メソッド）
	 */
	@Override
	public NonyushoDataResponse getNonyushoData(String shiteiNo, String nendo, String shinkokuYm) {
		String jichitaiCode = jichitaiContext.getJichitaiCd();
		log.debug("納入書動的データ取得開始: shiteiNo={}, nendo={}, shinkokuYm={}", shiteiNo, nendo, shinkokuYm);
		log.debug("設定された自治体コード: {}", jichitaiCode);

		NonyushoDataResponse response = new NonyushoDataResponse();

		try {
			// 申告年月から対象年月を算出（YYYY-MM 形式をYYYYMMに変換）
			final String taishoYm;
			if (shinkokuYm != null && !shinkokuYm.isEmpty()) {
				taishoYm = shinkokuYm.replace("-", ""); // "2026-03" -> "202603"
				log.debug("申告年月から対象年月を算出: shinkokuYm={} -> taishoYm={}", shinkokuYm, taishoYm);
			} else {
				taishoYm = null;
			}
			
			// t_fukaテーブルからデータ取得（対象年月で絞り込み）
			log.debug("賆課データ検索開始: jichitaiCode={}, shiteiNo={}, nendo={}, taishoYm={}", jichitaiCode, shiteiNo, nendo, taishoYm);
			
			// 最新の賆課データを取得
			List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(jichitaiCode, shiteiNo, nendo);
			
			// 対象年月が指定されている場合、その条件でフィルタリング
			if (taishoYm != null) {
				fukaList = fukaList.stream()
						.filter(f -> taishoYm.equals(f.getTaishoYm()))
						.collect(java.util.stream.Collectors.toList());
			}
			
			log.debug("取得した賆課データ件数: {}", fukaList.size());
			
			if (!fukaList.isEmpty()) {
				// 最新のレコードを取得（rno最大）
				Fuka fuka = fukaList.stream()
						.max(Comparator.comparing(Fuka::getRno))
						.orElse(fukaList.get(0));
				log.debug("取得した賆課情報: rno={}, totalZeigaku={}, kasanGaku1={}, kasanGaku2={}, kasanGaku3={}", 
						fuka.getRno(), fuka.getTotalZeigaku(), fuka.getKasanGaku1(), fuka.getKasanGaku2(), fuka.getKasanGaku3());
				
				response.setZeigaku(fuka.getTotalZeigaku() != null ? fuka.getTotalZeigaku().toString() : "0");
				Long kasanGaku = (fuka.getKasanGaku1() != null ? fuka.getKasanGaku1() : 0L)
						+ (fuka.getKasanGaku2() != null ? fuka.getKasanGaku2() : 0L)
						+ (fuka.getKasanGaku3() != null ? fuka.getKasanGaku3() : 0L);
				response.setKasan(kasanGaku.toString());
				
				log.debug("設定した税額: zeigaku={}, kasan={}", response.getZeigaku(), response.getKasan());

				// nokigenの設定（null値を除外して処理）
				List<LocalDate> dates = Arrays.asList(
						fuka.getNokigen())
						.stream()
						.filter(date -> date != null) // null値を除外
						.collect(java.util.stream.Collectors.toList());
						
				Optional<LocalDate> minDate = dates.stream().min(Comparator.naturalOrder());
				if (minDate.isPresent()) {
					response.setNokigen(minDate.get().toString());
					log.debug("納期限設定（最早日）: {}", minDate.get());
				} else if (fuka.getShinkokuYmd() != null) {
					// shinkoku_Ymdの翌月末を計算
					LocalDate nextMonthEnd = fuka.getShinkokuYmd().plusMonths(1)
							.withDayOfMonth(fuka.getShinkokuYmd().plusMonths(1).lengthOfMonth());
					response.setNokigen(nextMonthEnd.toString());
					log.debug("納期限設定（申告日基準）: {}", nextMonthEnd);
				} else {
					response.setNokigen("");
					log.error("納期限が設定できませんでした");
				}
			} else {
				response.setZeigaku("0");
				response.setKasan("0");
				response.setNokigen("");
			}

			// 自治体情報取得
			response.setJichitaiCd(jichitaiCode);
			log.debug("レスポンスに設定した自治体コード: {}", jichitaiCode);
			Optional<Jichitai> jichitaiOpt = jichitaiRepository.findById(jichitaiCode);
			if (jichitaiOpt.isPresent()) {
				response.setCityName(jichitaiOpt.get().getName());
				log.debug("取得した自治体名: {}", jichitaiOpt.get().getName());
			} else {
				response.setCityName("");
			}

			// m_reports_defテーブルからデータ取得
			response.setKozaNo(getReportsDefText(ReportsConstants.NONYUSHO_KOZA_NO));
			response.setNonyuBasho(getReportsDefText(ReportsConstants.NONYUSHO_KOZA));
			response.setShiteiKinyuName(getReportsDefText(ReportsConstants.NONYUSHO_SHITEI_KINYU_NAME));
			response.setTorimatome(getReportsDefText(ReportsConstants.NONYUSHO_TORIMATOME));

			log.debug("納入書動的データ取得完了: shiteiNo={}, nendo={}", shiteiNo, nendo);
			return response;

		} catch (Exception e) {
			log.error("納入書動的データ取得エラー: shiteiNo={}, nendo={}", shiteiNo, nendo, e);
			// エラー時はデフォルト値を返す
			response.setZeigaku("0");
			response.setKasan("0");
			response.setNokigen("");
			response.setCityName("");
			response.setJichitaiCd(jichitaiCode);
			response.setKozaNo("");
			response.setNonyuBasho("");
			response.setShiteiKinyuName("");
			response.setTorimatome("");
			return response;
		}
	}

	/**
	 * m_reports_defテーブルから定義IDに対応するdef_textを取得
	 */
	private String getReportsDefText(String defId) {
		String jichitaiCode = jichitaiContext.getJichitaiCd();
		try {
			ReportsDefId id = new ReportsDefId();
			id.setJichitaiCd(jichitaiCode);
			id.setId(defId);

			Optional<ReportsDef> reportsDefOpt = reportsDefRepository.findById(id);
			if (reportsDefOpt.isPresent()) {
				String defText = reportsDefOpt.get().getDefText();
				return defText != null ? defText : "";
			} else {
				log.error("該当するm_reports_defレコードが見つかりません: defId={}", defId);
				return "";
			}
		} catch (Exception e) {
			log.error("m_reports_defデータ取得エラー: defId={}", defId, e);
			return "";
		}
	}

	private JRDataSource buildParams(NonyushoDto dto) {
		NonyushoReportsDto reportsDto = new NonyushoReportsDto();

		// 自治体コードを取得
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		
		// 指定番号を取得
		String shiteiNo = dto.getShiteiNo();
		
		// 年度を取得
		String nendo = dto.getNendo();
		
		// 最新の賦課データを取得
		List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(jichitaiCd, shiteiNo,
				nendo);

		// 賦課情報が存在しない場合は null を返す
		if (fukaList.isEmpty()) {
			return null;
		}
		
		// 基本情報
		reportsDto.setCityName(dto.getCityName() != null ? dto.getCityName() : "");
		reportsDto.setJichitaiCd(jichitaiCd);
		reportsDto.setKozaNo(dto.getKozaNo() != null ? dto.getKozaNo() : "");
		reportsDto.setKozaName(dto.getKozaName() != null ? dto.getKozaName() : "");
		reportsDto.setShiteiNo(shiteiNo != null ? shiteiNo : "");
		reportsDto.setZeigaku(formatToCommaString(dto.getZeigaku()));
		reportsDto.setEntai(formatToCommaString(dto.getEntai()));
		reportsDto.setKasan(formatToCommaString(dto.getKasan()));
		reportsDto.setGokei(formatToCommaString(dto.getGokei()));
		reportsDto.setTokuYubin(dto.getTokuYubinNo() != null ? "〒" + dto.getTokuYubinNo().trim() : "");
		reportsDto.setTokuJusho(dto.getTokuJusho() != null ? dto.getTokuJusho().trim() : "");
		reportsDto.setTokuName(dto.getTokuName() != null ? dto.getTokuName() : "");
		reportsDto.setNonyuBasho(dto.getNonyuBasho() != null ? dto.getNonyuBasho() : "");
		reportsDto.setShiteiKinyuName(dto.getShiteiKinyuName() != null ? dto.getShiteiKinyuName() : "");
		reportsDto.setTorimatome(dto.getTorimatome() != null ? dto.getTorimatome() : "");
		
		// 年度（和暦）を設定
		String nendoStr = "";
		if (dto.getNendo() != null && !dto.getNendo().isEmpty()) {
			try {
				// yyyy の文字列を int に変換
				int year = Integer.parseInt(nendo);

				// 和暦の年を取得する
				LocalDate localDate = LocalDate.of(year, 1, 1);
				JapaneseDate japaneseDate = JapaneseDate.from(localDate);

				// 和暦用のフォーマッタを作成
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年", Locale.JAPANESE);
				String warekiYear = japaneseDate.format(formatter);

				nendoStr = warekiYear;

			} catch (NumberFormatException | java.time.format.DateTimeParseException e) {
				nendoStr = dto.getNendo();
			}
		}

		reportsDto.setNendo(nendoStr);

		// 申告年月
		if (dto.getShinkokuYmd() != null) {
			try {
				// 文字列を YearMonth に変換
				YearMonth yearMonth = YearMonth.parse(dto.getShinkokuYmd(), DateTimeFormatter.ofPattern("yyyyMM"));

		        // 和暦の年月フォーマッタを作成
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年M月", Locale.JAPANESE)
		                .withChronology(JapaneseChronology.INSTANCE);

		        // 和暦に変換
		        String strDate = yearMonth.atDay(1).format(formatter);
		        
		        reportsDto.setShinkokuYm(strDate);
		    } catch (DateTimeParseException e) {
		        reportsDto.setShinkokuYm("");
		    }
		} else {
			reportsDto.setShinkokuYm("");
		}

		// 納期限
		if (dto.getNokigen() != null) {
			try {
		        // 文字列を LocalDate に変換
		        LocalDate localDate = LocalDate.parse(dto.getNokigen(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		        // 和暦に変換
		        JapaneseDate japaneseDate = JapaneseDate.from(localDate);

		        // 和暦用のフォーマッタで文字列化
		        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年M月d日", Locale.JAPANESE);
		        String strDate = japaneseDate.format(formatter);
		        
		        reportsDto.setNokigen(strDate);
		    } catch (DateTimeParseException e) {
		        reportsDto.setNokigen("");
		    }
		} else {
			reportsDto.setNokigen("");
		}
		
		// 最新の賦課情報を取得
		Fuka fuka = fukaList.stream()
				.max(Comparator.comparing(Fuka::getRno))
				.orElse(fukaList.get(0));
		
		// 申告区分を設定
		reportsDto.setShinkokuKubun(fuka.getHenkoKbn());
		
		List<NonyushoReportsDto> dataSourceList = Arrays.asList(reportsDto);
		JRDataSource params = new JRBeanCollectionDataSource(dataSourceList);

		return params;
	}
	
	public boolean dataCheck(NonyushoDto dto) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		String shiteiNo = dto.getShiteiNo();
		String shinkokuYmd = dto.getShinkokuYmd();
		log.debug("dataCheck: jichitaiCd={}, shiteiNo={}, shinkokuYmd={}", jichitaiCd, shiteiNo, shinkokuYmd);
		
		// 最新の賆課データを取得
		List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndTaishoYmOrderByKibetsuAsc(
				jichitaiCd, shiteiNo, shinkokuYmd);
		log.debug("dataCheck result: {} 件", fukaList.size());
		
		// データが存在するかどうかを返す
		return fukaList.isEmpty();
	}
	
	private String formatToCommaString(Object value) {
	    if (value == null) {
	        return "0";
	    }
	    try {
	        BigDecimal val = new BigDecimal(value.toString());
	        DecimalFormat formatter = new DecimalFormat("#,##0");
	        return formatter.format(val);
	    } catch (NumberFormatException e) {
	        return value.toString();
	    }
	}
}