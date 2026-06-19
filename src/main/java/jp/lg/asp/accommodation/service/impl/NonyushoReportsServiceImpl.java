package jp.lg.asp.accommodation.service.impl;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.NonyushoDataResponse;
import jp.lg.asp.accommodation.dto.NonyushoDto;
import jp.lg.asp.accommodation.dto.NonyushoReportsDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.ReportsDef;
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

	@Value("${app.jichitai.code}")
	private String jichitaiCode;

	@Override
	public byte[] generateNonyushoPdf(NonyushoDto dto) {
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
			log.error("納入書PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
			throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
		}
	}

	@Override
	public NonyushoDataResponse getNonyushoData(String shiteiNo, String nendo) {
		log.info("納入書動的データ取得開始: shiteiNo={}, nendo={}", shiteiNo, nendo);
		log.info("設定された自治体コード: {}", jichitaiCode);

		NonyushoDataResponse response = new NonyushoDataResponse();

		try {
			// t_fukaテーブルからデータ取得（自治体コード、有効フラグを考慮）
			log.info("賆課データ検索開始: jichitaiCode={}, shiteiNo={}, nendo={}", jichitaiCode, shiteiNo, nendo);
			Optional<Fuka> fukaOpt = fukaRepository.findTopByJichitaiCdAndShiteiNoAndNendoAndNewFlgAndDelFlgOrderByRnoDesc(
					jichitaiCode, shiteiNo, nendo, "1", "0");

			if (fukaOpt.isPresent()) {
				Fuka fuka = fukaOpt.get();
				log.info("取得した賆課情報: rno={}, totalZeigaku={}, kasanGaku1={}, kasanGaku2={}, kasanGaku3={}", 
						fuka.getRno(), fuka.getTotalZeigaku(), fuka.getKasanGaku1(), fuka.getKasanGaku2(), fuka.getKasanGaku3());
				
				response.setZeigaku(fuka.getTotalZeigaku() != null ? fuka.getTotalZeigaku().toString() : "0");
				Long kasanGaku = (fuka.getKasanGaku1() != null ? fuka.getKasanGaku1() : 0L)
						+ (fuka.getKasanGaku2() != null ? fuka.getKasanGaku2() : 0L)
						+ (fuka.getKasanGaku3() != null ? fuka.getKasanGaku3() : 0L);
				response.setKasan(kasanGaku.toString());
				
				log.info("設定した税額: zeigaku={}, kasan={}", response.getZeigaku(), response.getKasan());

				// nokigenの設定（null値を除外して処理）
				List<LocalDate> dates = Arrays.asList(
						fuka.getNokigen1(),
						fuka.getNokigen2(),
						fuka.getNokigen3())
						.stream()
						.filter(date -> date != null) // null値を除外
						.collect(java.util.stream.Collectors.toList());
						
				Optional<LocalDate> minDate = dates.stream().min(Comparator.naturalOrder());
				if (minDate.isPresent()) {
					response.setNokigen(minDate.get().toString());
					log.info("納期限設定（最早日）: {}", minDate.get());
				} else if (fuka.getShinkokuYmd() != null) {
					// shinkoku_Ymdの翌月末を計算
					LocalDate nextMonthEnd = fuka.getShinkokuYmd().plusMonths(1)
							.withDayOfMonth(fuka.getShinkokuYmd().plusMonths(1).lengthOfMonth());
					response.setNokigen(nextMonthEnd.toString());
					log.info("納期限設定（申告日基準）: {}", nextMonthEnd);
				} else {
					response.setNokigen("");
					log.warn("納期限が設定できませんでした");
				}
			} else {
				log.warn("該当するt_fukaレコードが見つかりません: shiteiNo={}, nendo={}", shiteiNo, nendo);
				response.setZeigaku("0");
				response.setKasan("0");
				response.setNokigen("");
			}

			// 自治体情報取得
			response.setJichitaiCd(jichitaiCode);
			log.info("レスポンスに設定した自治体コード: {}", jichitaiCode);
			Optional<Jichitai> jichitaiOpt = jichitaiRepository.findById(jichitaiCode);
			if (jichitaiOpt.isPresent()) {
				response.setCityName(jichitaiOpt.get().getName());
				log.info("取得した自治体名: {}", jichitaiOpt.get().getName());
			} else {
				log.warn("自治体情報が見つかりません: jichitaiCd={}", jichitaiCode);
				response.setCityName("");
			}

			// m_reports_defテーブルからデータ取得
			response.setKozaNo(getReportsDefData("口座番号"));
			response.setNonyuBasho(getReportsDefData("納入場所"));
			response.setShiteiKinyuName(getReportsDefData("指定金融機関名"));
			response.setTorimatome(getReportsDefData("取りまとめ店"));

			log.info("納入書動的データ取得完了: shiteiNo={}, nendo={}", shiteiNo, nendo);
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
	 * m_reports_defテーブルから指定したテキストのdef_dataを取得
	 */
	private String getReportsDefData(String defText) {
		try {
			Optional<ReportsDef> reportsDefOpt = reportsDefRepository.findByJichitaiCdAndDefText(jichitaiCode, defText);
			if (reportsDefOpt.isPresent()) {
				byte[] defData = reportsDefOpt.get().getDefData();
				return defData != null ? new String(defData, "UTF-8") : "";
			} else {
				log.warn("該当するm_reports_defレコードが見つかりません: defText={}", defText);
				return "";
			}
		} catch (Exception e) {
			log.error("m_reports_defデータ取得エラー: defText={}", defText, e);
			return "";
		}
	}

	private JRDataSource buildParams(NonyushoDto dto) {
		NonyushoReportsDto reportsDto = new NonyushoReportsDto();

		// 基本情報
		reportsDto.setCityName(dto.getCityName() != null ? dto.getCityName() : "");
		reportsDto.setJichitaiCd(dto.getJichitaiCd() != null ? dto.getJichitaiCd() : "");
		reportsDto.setKozaNo(dto.getKozaNo() != null ? dto.getKozaNo() : "");
		reportsDto.setKozaName(dto.getKozaName() != null ? dto.getKozaName() : "");
		reportsDto.setNendo(dto.getNendo() != null ? dto.getNendo() : "");
		reportsDto.setShiteiNo(dto.getShiteiNo() != null ? dto.getShiteiNo() : "");
		reportsDto.setZeigaku(dto.getZeigaku() != null ? dto.getZeigaku() : "");
		reportsDto.setEntai(dto.getEntai() != null ? dto.getEntai() : "");
		reportsDto.setKasan(dto.getKasan() != null ? dto.getKasan() : "");
		reportsDto.setGokei(dto.getGokei() != null ? dto.getGokei() : "");

		// 住所に郵便番号を連結
		String tokuJusho = dto.getTokuJusho() != null ? dto.getTokuJusho().trim() : "";
		String tokuYubinNo = dto.getTokuYubinNo() != null ? dto.getTokuYubinNo().trim() : "";

		log.info("郵便番号連結前: 郵便番号=[{}], 住所=[{}]", tokuYubinNo, tokuJusho);

		// 郵便番号がある場合は住所の先頭に付加
		if (!tokuYubinNo.isEmpty()) {
			// 郵便番号のフォーマットを確認し、必要に応じてハイフンを追加
			if (tokuYubinNo.matches("\\d{7}")) {
				tokuYubinNo = tokuYubinNo.substring(0, 3) + "-" + tokuYubinNo.substring(3);
			}
			// 郵便番号を先頭に追加（住所が空でも郵便番号は表示）
			tokuJusho = "〒" + tokuYubinNo + (tokuJusho.isEmpty() ? "" : " " + tokuJusho);
			log.info("郵便番号連結後: [{}]", tokuJusho);
		} else {
			log.info("郵便番号が空のため連結処理をスキップしました");
		}
		reportsDto.setTokuJusho(tokuJusho);

		reportsDto.setTokuName(dto.getTokuName() != null ? dto.getTokuName() : "");
		reportsDto.setNonyuBasho(dto.getNonyuBasho() != null ? dto.getNonyuBasho() : "");
		reportsDto.setShiteiKinyuName(dto.getShiteiKinyuName() != null ? dto.getShiteiKinyuName() : "");
		reportsDto.setTorimatome(dto.getTorimatome() != null ? dto.getTorimatome() : "");
		reportsDto.setJichitaiKoin(dto.getJichitaiKoin());

		// 申告年月
		if (dto.getShinkokuYmd() != null) {
			String strDate = dto.getShinkokuYmd().format(DateTimeFormatter.ofPattern("yyyy年M月"));
			reportsDto.setShinkokuYm(strDate);
		} else {
			reportsDto.setShinkokuYm("");
		}

		// 納期限
		if (dto.getNokigen() != null) {
			String strDate = dto.getNokigen().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
			reportsDto.setNokigen(strDate);
		} else {
			reportsDto.setNokigen("");
		}

		List<NonyushoReportsDto> dataSourceList = Arrays.asList(reportsDto);
		JRDataSource params = new JRBeanCollectionDataSource(dataSourceList);

		return params;
	}
}