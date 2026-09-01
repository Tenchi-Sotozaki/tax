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
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.entity.NokigenId;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.ReportsDefId;
import jp.lg.asp.accommodation.entity.TokureiTekiyo;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.NokigenRepository;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.repository.TokureiTekiyoRepository;
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
	private final NokigenRepository nokigenRepository;
	private final ReportsDefRepository reportsDefRepository;
	private final TokureiTekiyoRepository tokureiTekiyoRepository;

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
	 * 納入書動的データ取得
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
			log.debug("賦課データ検索開始: jichitaiCode={}, shiteiNo={}, nendo={}, taishoYm={}", jichitaiCode, shiteiNo, nendo, taishoYm);

			// 最新の賦課データを取得
			List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(jichitaiCode, shiteiNo, nendo);

			// 対象年月が指定されている場合、その条件でフィルタリング
			if (taishoYm != null) {
				fukaList = fukaList.stream()
						.filter(f -> taishoYm.equals(f.getTaishoYm()))
						.collect(java.util.stream.Collectors.toList());
			}

			log.debug("取得した賦課データ件数: {}", fukaList.size());

			if (!fukaList.isEmpty()) {
				// 最新のレコードを取得（rno最大）
				Fuka fuka = fukaList.stream()
						.max(Comparator.comparing(Fuka::getRno))
						.orElse(fukaList.get(0));
				log.debug("取得した賦課情報: rno={}, totalZeigaku={}, kasanGaku1={}, kasanGaku2={}, kasanGaku3={}",
						fuka.getRno(), fuka.getTotalZeigaku(), fuka.getKasanGaku1(), fuka.getKasanGaku2(), fuka.getKasanGaku3());

				response.setZeigaku(fuka.getTotalZeigaku() != null ? fuka.getTotalZeigaku().toString() : "0");
				Long kasanGaku = (fuka.getKasanGaku1() != null ? fuka.getKasanGaku1() : 0L)
						+ (fuka.getKasanGaku2() != null ? fuka.getKasanGaku2() : 0L)
						+ (fuka.getKasanGaku3() != null ? fuka.getKasanGaku3() : 0L);
				response.setKasan(kasanGaku.toString());

				log.debug("設定した税額: zeigaku={}, kasan={}", response.getZeigaku(), response.getKasan());

				// nokigenの設定
				LocalDate nokigenDate = fuka.getNokigen();
				if (nokigenDate != null) {
					// ① fuka.nokigen あり → そのまま返す
					response.setNokigen(nokigenDate.toString());
					log.debug("納期限設定: {}", nokigenDate);
				} else {
					// ② fuka.nokigen なし → 特例適用判定
					String nokigenValue = resolveNokigenFromMaster(
							shiteiNo, shinkokuYm, nendo, fuka.getShinkokuYmd());
					response.setNokigen(nokigenValue);
					if (nokigenValue.isEmpty()) {
						log.error("納期限が設定できませんでした");
					}
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
	 * fuka.nokigenがnullの場合の納期限解決
	 * 特例適用中 → 四半期末のm_nokigenを返す
	 * 特例適用外 → fukaShinkokuYmdの翌月末を返す（nullなら空文字）
	 */
	private String resolveNokigenFromMaster(String shiteiNo, String shinkokuYm,
			String nendo, LocalDate fukaShinkokuYmd) {
		try {
			String jichitaiCd = jichitaiContext.getJichitaiCd();

			// 自治体設定から年度開始月・defaultShukiを取得
			Optional<Jichitai> jichitaiOpt = jichitaiRepository.findById(jichitaiCd);
			int nendoStMonth = jichitaiOpt
					.map(j -> Integer.parseInt(j.getNendoStMonth().trim()))
					.orElse(3);
			int defaultShuki = jichitaiOpt
					.map(j -> Integer.parseInt(j.getNozeiShuki().trim()))
					.orElse(1);

			// 特例適用判定（defaultShuki==1 かつ taishoDateが期間内レコードあり）
			boolean isTokurei = false;
			if (defaultShuki == 1 && shinkokuYm != null && !shinkokuYm.isEmpty()) {
				LocalDate taishoDate = LocalDate.parse(shinkokuYm + "-01");
				List<TokureiTekiyo> tekiyoList =
						tokureiTekiyoRepository.findActiveByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
				isTokurei = tekiyoList.stream().anyMatch(t ->
						t.getTekiyoStYmd() != null && t.getTekiyoEdYmd() != null &&
						!taishoDate.isBefore(t.getTekiyoStYmd()) &&
						!taishoDate.isAfter(t.getTekiyoEdYmd()));
			}

			if (isTokurei) {
				// 特例適用中：四半期末月のm_nokigenインデックスを算出
				int month = Integer.parseInt(shinkokuYm.replace("-", "").substring(4, 6));
				// 年度開始月を基準にインデックスを算出（例：nendoStMonth=3 → 3月=1st）
				int index = (month - nendoStMonth + 12) % 12 + 1;
				// 四半期末に丸める（1,2,3→3 / 4,5,6→6 / 7,8,9→9 / 10,11,12→12）
				int quarterEndIndex = ((index - 1) / 3 + 1) * 3;
				log.debug("特例適用中: month={}, index={}, quarterEndIndex={}", month, index, quarterEndIndex);

				Optional<Nokigen> nokigenOpt =
						nokigenRepository.findById(new NokigenId(jichitaiCd, nendo));
				if (nokigenOpt.isEmpty()) return "";
				String dbValue = getNokigenByIndex(nokigenOpt.get(), quarterEndIndex);
				if (dbValue == null || dbValue.isBlank()) return "";
				return LocalDate.parse(dbValue, DateTimeFormatter.ofPattern("yyyyMMdd"))
						.format(DateTimeFormatter.ISO_LOCAL_DATE);
			} else {
				// 特例適用外：fukaShinkokuYmdの翌月末
				if (fukaShinkokuYmd == null) return "";
				LocalDate nextMonthEnd = fukaShinkokuYmd.plusMonths(1)
						.withDayOfMonth(fukaShinkokuYmd.plusMonths(1).lengthOfMonth());
				log.debug("特例適用外: fukaShinkokuYmd={}, nextMonthEnd={}", fukaShinkokuYmd, nextMonthEnd);
				return nextMonthEnd.toString();
			}
		} catch (Exception e) {
			log.error("納期限解決エラー", e);
			return "";
		}
	}

	private String getNokigenByIndex(Nokigen nokigen, int index) {
		return switch (index) {
			case 1  -> nokigen.getNokigen1st();
			case 2  -> nokigen.getNokigen2nd();
			case 3  -> nokigen.getNokigen3rd();
			case 4  -> nokigen.getNokigen4th();
			case 5  -> nokigen.getNokigen5th();
			case 6  -> nokigen.getNokigen6th();
			case 7  -> nokigen.getNokigen7th();
			case 8  -> nokigen.getNokigen8th();
			case 9  -> nokigen.getNokigen9th();
			case 10 -> nokigen.getNokigen10th();
			case 11 -> nokigen.getNokigen11th();
			case 12 -> nokigen.getNokigen12th();
			default -> "";
		};
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

		String jichitaiCd = jichitaiContext.getJichitaiCd();
		String shiteiNo = dto.getShiteiNo();
		String nendo = dto.getNendo();

		List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(jichitaiCd, shiteiNo, nendo);

		if (fukaList.isEmpty()) {
			return null;
		}

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
				int year = Integer.parseInt(nendo);
				LocalDate localDate = LocalDate.of(year, 1, 1);
				JapaneseDate japaneseDate = JapaneseDate.from(localDate);
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年", Locale.JAPANESE);
				nendoStr = japaneseDate.format(formatter);
			} catch (NumberFormatException | java.time.format.DateTimeParseException e) {
				nendoStr = dto.getNendo();
			}
		}
		reportsDto.setNendo(nendoStr);

		// 申告年月
		if (dto.getShinkokuYmd() != null) {
			try {
				YearMonth yearMonth = YearMonth.parse(dto.getShinkokuYmd(), DateTimeFormatter.ofPattern("yyyyMM"));
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年M月", Locale.JAPANESE)
						.withChronology(JapaneseChronology.INSTANCE);
				reportsDto.setShinkokuYm(yearMonth.atDay(1).format(formatter));
			} catch (DateTimeParseException e) {
				reportsDto.setShinkokuYm("");
			}
		} else {
			reportsDto.setShinkokuYm("");
		}

		// 納期限
		if (dto.getNokigen() != null) {
			try {
				LocalDate localDate = LocalDate.parse(dto.getNokigen(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
				JapaneseDate japaneseDate = JapaneseDate.from(localDate);
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年M月d日", Locale.JAPANESE);
				reportsDto.setNokigen(japaneseDate.format(formatter));
			} catch (DateTimeParseException e) {
				reportsDto.setNokigen("");
			}
		} else {
			reportsDto.setNokigen("");
		}

		Fuka fuka = fukaList.stream()
				.max(Comparator.comparing(Fuka::getRno))
				.orElse(fukaList.get(0));
		reportsDto.setShinkokuKubun(fuka.getHenkoKbn());

		List<NonyushoReportsDto> dataSourceList = Arrays.asList(reportsDto);
		return new JRBeanCollectionDataSource(dataSourceList);
	}

	public boolean dataCheck(NonyushoDto dto) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		String shiteiNo = dto.getShiteiNo();
		String shinkokuYmd = dto.getShinkokuYmd();
		log.debug("dataCheck: jichitaiCd={}, shiteiNo={}, shinkokuYmd={}", jichitaiCd, shiteiNo, shinkokuYmd);

		List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndTaishoYmOrderByKibetsuAsc(
				jichitaiCd, shiteiNo, shinkokuYmd);
		log.debug("dataCheck result: {} 件", fukaList.size());

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
