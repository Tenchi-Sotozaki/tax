package jp.lg.asp.accommodation.service.impl;

import java.util.Optional;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.TokugimuShiteiTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.service.TokugimuShiteiTsuchiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 特別徴収義務者指定通知 Service 実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokugimuShiteiTsuchiServiceImpl implements TokugimuShiteiTsuchiService {

	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final ReportsCommonService reportsCommonService;

	@Value("${app.jichitai.code}")
	private String jichitaiCode;

	private String city;
	private String jichitaiName;
	private String jorei;

	@PostConstruct
	public void init() {
		Jichitai jichitaiInfo = reportsCommonService.getJichitaiInfo();
		jichitaiName = jichitaiInfo.getName();
		city = jichitaiInfo.getKbnName();
		jorei = reportsCommonService.getReportsDefText(ReportsConstants.TOKUGIMU_SHITEI_JOREI);
	}

	@Override
	public TokugimuShiteiTsuchiDto getTokugimuInfo(String shiteiNo) {
		// 特別徴収義務者情報取得（最新・未削除）
		Optional<Tokugimu> tokugimuOpt = tokugimuRepository
				.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
						jichitaiCode, shiteiNo, "1", "0");

		if (tokugimuOpt.isEmpty()) {
			log.warn("特別徴収義務者が見つかりません: shiteiNo={}", shiteiNo);
			return null;
		}

		Tokugimu tokugimu = tokugimuOpt.get();

		// 宛名情報取得
		Optional<Atena> atenaOpt = atenaRepository
				.findByJichitaiCdAndAtenaNo(jichitaiCode, tokugimu.getAtenaNo());

		if (atenaOpt.isEmpty()) {
			log.warn("宛名情報が見つかりません: atenaNo={}", tokugimu.getAtenaNo());
			return null;
		}

		Atena atena = atenaOpt.get();

		// DTOに設定
		TokugimuShiteiTsuchiDto dto = new TokugimuShiteiTsuchiDto();
		dto.setShiteiNo(tokugimu.getShiteiNo());
		dto.setTokuName(atena.getName());

		// 住所を郵便番号と住所で連結
		String tokuJusho = "";
		if (atena.getYubinNo() != null && !atena.getYubinNo().isEmpty()) {
			tokuJusho = "〒" + atena.getYubinNo() + "\r\n";
		}
		if (atena.getJusho() != null) {
			tokuJusho += atena.getJusho();
		}
		dto.setTokuJusho(tokuJusho);

		dto.setShisetsuName(tokugimu.getShisetsuName());

		// 施設所在地を郵便番号と住所で連結
		String shisetsuJusho = "";
		if (tokugimu.getShisetsuYubinNo() != null && !tokugimu.getShisetsuYubinNo().isEmpty()) {
			shisetsuJusho = "〒" + tokugimu.getShisetsuYubinNo() + "\r\n";
		}
		if (tokugimu.getShisetsuJusho() != null) {
			shisetsuJusho += tokugimu.getShisetsuJusho();
		}
		dto.setShisetsuJusho(shisetsuJusho);

		// application.ymlから取得する値
		dto.setCityName(jichitaiName);
		dto.setCity(city);
		dto.setJorei(jorei);

		return dto;
	}
}