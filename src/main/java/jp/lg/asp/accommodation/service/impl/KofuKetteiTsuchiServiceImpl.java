package jp.lg.asp.accommodation.service.impl;

import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宿泊税特別徴収事務交付金交付決定通知書 Service実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KofuKetteiTsuchiServiceImpl implements KofuKetteiTsuchiService {

	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final ShoreikinRepository shoreikinRepository;
	private final ReportsDefRepository reportsDefRepository;
	private final ReportsCommonService reportsCommonService;

	@Value("${app.jichitai.code}")
	private String jichitaiCode;

	private String jichitaiName;
	private String jorei;

	@PostConstruct
	public void init() {
		Jichitai jichitaiInfo = reportsCommonService.getJichitaiInfo();
		if (jichitaiInfo != null) {
			jichitaiName = jichitaiInfo.getName();
		}
		jorei = reportsCommonService.getReportsDefText(ReportsConstants.SHOREIKIN_KOFU_JOREI);
	}

	@Override
	public KofuKetteiTsuchiDto getReportData(String shiteiNo) {
		try {
			log.info("交付決定通知書データ取得開始 - 指定番号: {}", shiteiNo);

			// 特別徴収義務者情報取得（最新・未削除）
			Optional<Tokugimu> tokugimuOpt = tokugimuRepository
					.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
							jichitaiCode, shiteiNo, "1", "0");

			if (tokugimuOpt.isEmpty()) {
				log.warn("特別徴収義務者が見つかりません: shiteiNo={}", shiteiNo);
				return null;
			}

			Tokugimu tokugimu = tokugimuOpt.get();
			log.info("特別徴収義務者情報取得: {}", tokugimu.getShiteiNo());

			// 宛名情報取得
			Optional<Atena> atenaOpt = atenaRepository
					.findByJichitaiCdAndAtenaNo(jichitaiCode, tokugimu.getAtenaNo());

			if (atenaOpt.isEmpty()) {
				log.warn("宛名情報が見つかりません: atenaNo={}", tokugimu.getAtenaNo());
				return null;
			}

			Atena atena = atenaOpt.get();
			log.info("宛名情報取得: {}", atena.getName());

			// 奨励金情報取得（最新年度）
			List<Shoreikin> shoreikinList = shoreikinRepository
					.findActiveByJichitaiCd(jichitaiCode);

			// 指定番号でフィルタリングして最新を取得
			Optional<Shoreikin> shoreikinOpt = shoreikinList.stream()
					.filter(s -> shiteiNo.equals(s.getShiteiNo()))
					.findFirst();

			// 帳票定義から発行様式と条令を取得
			String hakkoYoshiki = getReportsDefText("KOFU_KETTEI_TSUCHI_HAKKO_YOSHIKI");

			// DTOに設定
			KofuKetteiTsuchiDto dto = new KofuKetteiTsuchiDto();
			dto.setHakkoYoshiki(hakkoYoshiki);
			dto.setTokugimuName(atena.getName());
			dto.setCityName(jichitaiName);
			dto.setHakkoJorei(jorei);
			dto.setShisetsuName(tokugimu.getShisetsuName());
			dto.setShiteiNo(tokugimu.getShiteiNo());

			// 施設住所を郵便番号と住所で連結
			StringBuilder shisetsuJusho = new StringBuilder();
			if (tokugimu.getShisetsuYubinNo() != null && !tokugimu.getShisetsuYubinNo().isEmpty()) {
				shisetsuJusho.append("〒").append(tokugimu.getShisetsuYubinNo()).append("\n");
			}
			if (tokugimu.getShisetsuJusho() != null) {
				shisetsuJusho.append(tokugimu.getShisetsuJusho());
			}
			dto.setShisetsuJusho(shisetsuJusho.toString());

			// 奨励金情報設定
			if (shoreikinOpt.isPresent()) {
				Shoreikin shoreikin = shoreikinOpt.get();
				Long kofuGaku = shoreikin.getKofuGaku();
				dto.setKofugaku(kofuGaku != null ? String.valueOf(kofuGaku) : "0");

				if (shoreikin.getKofuYmd() != null) {
					dto.setKofuYmd(shoreikin.getKofuYmd().toString());
				}

				log.info("奨励金情報設定: 交付額={}", dto.getKofugaku());
			} else {
				dto.setKofugaku("0");
				log.warn("奨励金情報が見つかりません: shiteiNo={}", shiteiNo);
			}

			log.info("交付決定通知書データ取得完了: {}", dto.getShiteiNo());
			return dto;

		} catch (Exception e) {
			log.error("交付決定通知書データ取得中にエラーが発生しました - 指定番号: {}", shiteiNo, e);
			return null;
		}
	}

	/**
	 * 帳票定義からテキストを取得する
	 */
	private String getReportsDefText(String id) {
		try {
			Optional<ReportsDef> reportsDefOpt = reportsDefRepository
					.findByIdAndJichitaiCd(id, jichitaiCode);

			if (reportsDefOpt.isPresent()) {
				String defText = reportsDefOpt.get().getDefText();
				return defText != null ? defText : "";
			} else {
				log.warn("帳票定義が見つかりません: id={}", id);
				return "";
			}
		} catch (Exception e) {
			log.error("帳票定義取得中にエラーが発生しました: id={}", id, e);
			return "";
		}
	}
}