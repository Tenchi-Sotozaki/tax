package jp.lg.asp.accommodation.service.impl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宿泊税特別徴収事務交付金決定通知書・交付申請書 Service実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KofuKetteiTsuchiShinseiServiceImpl implements KofuKetteiTsuchiShinseiService {

	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final ShoreikinRepository shoreikinRepository;
	private final ReportsDefRepository reportsDefRepository;
	private final ReportsCommonService reportsCommonService;

	private final JichitaiContext jichitaiContext;

	private String jichitaiName;
	private String jorei;
	private byte[] koin;

	private void init() {
		Jichitai jichitaiInfo = reportsCommonService.getJichitaiInfo();
		if (jichitaiInfo != null) {
			jichitaiName = jichitaiInfo.getName();
		}
		jorei = reportsCommonService.getReportsDefText(ReportsConstants.SHOREIKIN_KOFU_JOREI);
		koin = reportsCommonService.getReportsDefData(ReportsConstants.KOIN);
	}

	@Override
	public KofuKetteiTsuchiShinseiDto getReportData(String shiteiNo) {
		init();
		// デフォルト年度（現在年度）で取得
		LocalDate now = LocalDate.now();
		String currentNendo = String.valueOf(
				now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1);
		return getReportData(shiteiNo, currentNendo);
	}

	@Override
	public KofuKetteiTsuchiShinseiDto getReportData(String shiteiNo, String nendo) {
		init();
		String jichitaiCode = jichitaiContext.getJichitaiCd();
		try {
			log.debug("交付申請書データ取得開始 - 指定番号: {}, 年度: {}", shiteiNo, nendo);

			// 特別徴収義務者情報取得（最新・未削除）
			Optional<Tokugimu> tokugimuOpt = tokugimuRepository
					.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
							jichitaiCode, shiteiNo, "1", "0");

			if (tokugimuOpt.isEmpty()) {
				log.error("特別徴収義務者が見つかりません: shiteiNo={}", shiteiNo);
				return null;
			}

			Tokugimu tokugimu = tokugimuOpt.get();
			log.debug("特別徴収義務者情報取得: {}", tokugimu.getShiteiNo());

			// 宛名情報取得
			Optional<Atena> atenaOpt = atenaRepository
					.findByJichitaiCdAndAtenaNo(jichitaiCode, tokugimu.getAtenaNo());

			if (atenaOpt.isEmpty()) {
				log.error("宛名情報が見つかりません: atenaNo={}", tokugimu.getAtenaNo());
				return null;
			}

			Atena atena = atenaOpt.get();
			log.debug("宛名情報取得: {}", atena.getName());

			// 奨励金情報取得
			Optional<Shoreikin> shoreikinOpt = shoreikinRepository
					.findByJichitaiCdAndShiteiNoAndNendo(jichitaiCode, shiteiNo, nendo);

			// 帳票定義から発行様式と交付条件を取得
			String hakkoYoshiki = getReportsDefText(ReportsConstants.KOFU_HAKKO_YOSHIKI);
			String kofuJoken = getReportsDefText(ReportsConstants.KOFU_JOKEN);
			
			// DTOに設定
			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(tokugimu.getShiteiNo());
			dto.setNendo(nendo);
			dto.setTokuName(atena.getName());
			dto.setShisetsuName(tokugimu.getShisetsuName());
			
			// 施設住所を郵便番号と住所で連結
			StringBuilder shisetsuJusho = new StringBuilder();
			if (tokugimu.getShisetsuYubinNo() != null && !tokugimu.getShisetsuYubinNo().isEmpty()) {
				shisetsuJusho.append("〒").append(tokugimu.getShisetsuYubinNo()).append("\n");
			}
			if (tokugimu.getShisetsuJusho() != null) {
				shisetsuJusho.append(tokugimu.getShisetsuJusho());
			}
			dto.setShisetsuJusho(shisetsuJusho.toString());

			// 奨励金情報設定（数値のみ）
			if (shoreikinOpt.isPresent()) {
				Shoreikin shoreikin = shoreikinOpt.get();
				// 数値を文字列に変換し、nullチェックを実施
				Long kofuZeigaku = shoreikin.getKofuZeigaku();
				Long kofuGaku = shoreikin.getKofuGaku();
			
				// 数値のみを設定（JRXMLで単位を付与）
				dto.setNonyugaku(kofuZeigaku != null ? String.valueOf(kofuZeigaku) : "0");
				dto.setKofugaku(kofuGaku != null ? String.valueOf(kofuGaku) : "0");
				
				if (shoreikin.getKofuYmd() != null) {
					
					dto.setKofuYmd(shoreikin.getKofuYmd().toString());
				}

				log.debug("奨励金情報設定: 納入額={}, 交付額={}", dto.getNonyugaku(), dto.getKofugaku());
			} else {
				// 奨励金情報が存在しない場合はデフォルト値
				dto.setNonyugaku("0");
				dto.setKofugaku("0");
				log.error("奨励金情報が見つかりません: shiteiNo={}, nendo={}", shiteiNo, nendo);
				return null;
			}

			// 固定値設定
			dto.setCityName(jichitaiName);
			dto.setJorei(jorei);
			dto.setHakkoJorei(jorei);
			dto.setHakkoYoshiki(hakkoYoshiki);
			dto.setKofuJoken(kofuJoken);
			dto.setKoin(koin != null && koin.length > 0 ? koin : null);

			log.debug("交付申請書データ取得完了: {}, 年度: {}", dto.getShiteiNo(), dto.getNendo());
			return dto;

		} catch (Exception e) {
			log.error("交付申請書データ取得中にエラーが発生しました - 指定番号: {}, 年度: {}", shiteiNo, nendo, e);
			return null;
		}
	}

	@Override
	public List<KofuKetteiTsuchiShinseiDto> getAllReportData(String nendo) {
		if (nendo == null || nendo.isBlank()) {
			return List.of();
		}
		init();
		String jichitaiCode = jichitaiContext.getJichitaiCd();
		String hakkoYoshiki = getReportsDefText(ReportsConstants.KOFU_HAKKO_YOSHIKI);
		String kofuJoken = getReportsDefText(ReportsConstants.KOFU_JOKEN);

		List<Tokugimu> tokugimuList = tokugimuRepository.findAllByJichitaiCd(jichitaiCode);
		if (tokugimuList.isEmpty()) {
			return List.of();
		}

		List<BigDecimal> atenaNos = tokugimuList.stream().map(Tokugimu::getAtenaNo).distinct().collect(Collectors.toList());
		Map<BigDecimal, Atena> atenaMap = atenaRepository.findByJichitaiCdAndAtenaNoIn(jichitaiCode, atenaNos)
				.stream().collect(Collectors.toMap(Atena::getAtenaNo, a -> a));

		List<String> shiteiNoList = tokugimuList.stream().map(Tokugimu::getShiteiNo).collect(Collectors.toList());
		Map<String, Shoreikin> shoreikinMap = shoreikinRepository
				.findByJichitaiCdAndShiteiNoInAndNendo(jichitaiCode, shiteiNoList, nendo)
				.stream().collect(Collectors.toMap(Shoreikin::getShiteiNo, s -> s));

		List<KofuKetteiTsuchiShinseiDto> result = new ArrayList<>();
		for (Tokugimu tokugimu : tokugimuList) {
			Atena atena = atenaMap.get(tokugimu.getAtenaNo());
			Shoreikin shoreikin = shoreikinMap.get(tokugimu.getShiteiNo());
			if (atena == null || shoreikin == null) {
				log.warn("帳票データスキップ: shiteiNo={}", tokugimu.getShiteiNo());
				continue;
			}

			KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
			dto.setShiteiNo(tokugimu.getShiteiNo());
			dto.setNendo(nendo);
			dto.setTokuName(atena.getName());
			dto.setShisetsuName(tokugimu.getShisetsuName());

			StringBuilder shisetsuJusho = new StringBuilder();
			if (tokugimu.getShisetsuYubinNo() != null && !tokugimu.getShisetsuYubinNo().isEmpty()) {
				shisetsuJusho.append("〒").append(tokugimu.getShisetsuYubinNo()).append("\n");
			}
			if (tokugimu.getShisetsuJusho() != null) {
				shisetsuJusho.append(tokugimu.getShisetsuJusho());
			}
			dto.setShisetsuJusho(shisetsuJusho.toString());

			Long kofuZeigaku = shoreikin.getKofuZeigaku();
			Long kofuGaku = shoreikin.getKofuGaku();
			dto.setNonyugaku(kofuZeigaku != null ? String.valueOf(kofuZeigaku) : "0");
			dto.setKofugaku(kofuGaku != null ? String.valueOf(kofuGaku) : "0");
			if (shoreikin.getKofuYmd() != null) {
				dto.setKofuYmd(shoreikin.getKofuYmd().toString());
			}

			dto.setCityName(jichitaiName);
			dto.setJorei(jorei);
			dto.setHakkoJorei(jorei);
			dto.setHakkoYoshiki(hakkoYoshiki);
			dto.setKofuJoken(kofuJoken);
			dto.setKoin(koin != null && koin.length > 0 ? koin : null);

			result.add(dto);
		}
		return result;
	}

	/**
	 * 帳票定義からテキストを取得する
	 */
	private String getReportsDefText(String id) {
		String jichitaiCode = jichitaiContext.getJichitaiCd();
		try {
			Optional<ReportsDef> reportsDefOpt = reportsDefRepository
					.findByIdAndJichitaiCd(id, jichitaiCode);

			if (reportsDefOpt.isPresent()) {
				String defText = reportsDefOpt.get().getDefText();
				return defText != null ? defText : "";
			} else {
				log.error("帳票定義が見つかりません: id={}", id);
				return "";
			}
		} catch (Exception e) {
			log.error("帳票定義取得中にエラーが発生しました: id={}", id, e);
			return "";
		}
	}
}
