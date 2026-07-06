package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuShinseiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.KofuShinseiService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宿泊税特別徴収事務交付金交付申請書 Service実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KofuShinseiServiceImpl implements KofuShinseiService {

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
	public KofuShinseiDto getReportData(String shiteiNo) {
		// デフォルト年度（現在年度）で取得
		LocalDate now = LocalDate.now();
		String currentNendo = String.valueOf(
				now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1);
		return getReportData(shiteiNo, currentNendo);
	}

	@Override
	public KofuShinseiDto getReportData(String shiteiNo, String nendo) {
		try {
			log.info("交付申請書データ取得開始 - 指定番号: {}, 年度: {}", shiteiNo, nendo);

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

			// 奨励金情報取得
			Optional<Shoreikin> shoreikinOpt = shoreikinRepository
					.findByJichitaiCdAndShiteiNoAndNendo(jichitaiCode, shiteiNo, nendo);

			// 帳票定義から発行様式と交付条件を取得
			String hakkoYoshiki = getReportsDefText("KOFU_SHINSEI_HAKKO_YOSHIKI");
			String kofuJoken = getReportsDefText("KOFU_SHINSEI_KOFU_JOKEN");

			// DTOに設定
			KofuShinseiDto dto = new KofuShinseiDto();
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

				log.info("奨励金情報設定: 納入額={}, 交付額={}", dto.getNonyugaku(), dto.getKofugaku());
			} else {
				// 奨励金情報が存在しない場合はデフォルト値
				dto.setNonyugaku("0");
				dto.setKofugaku("0");
				log.warn("奨励金情報が見つかりません: shiteiNo={}, nendo={}", shiteiNo, nendo);
			}

			// 固定値設定
			dto.setCityName(jichitaiName);
			dto.setJorei(jorei);
			dto.setHakkoYoshiki(hakkoYoshiki);
			dto.setKofuJoken(kofuJoken);

			log.info("交付申請書データ取得完了: {}, 年度: {}", dto.getShiteiNo(), dto.getNendo());
			return dto;

		} catch (Exception e) {
			log.error("交付申請書データ取得中にエラーが発生しました - 指定番号: {}, 年度: {}", shiteiNo, nendo, e);
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
