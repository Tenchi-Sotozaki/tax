package jp.lg.asp.accommodation.service.impl;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.TokugimuJuriTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.service.TokugimuJuriTsuchiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 特別徴収義務者申請受理通知 Service 実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokugimuJuriTsuchiServiceImpl implements TokugimuJuriTsuchiService {

	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final ReportsCommonService reportsCommonService;

	private final JichitaiContext jichitaiContext;

	private String jichitaiName;
	private String jorei;

	private void init() {
		Jichitai jichitaiInfo = reportsCommonService.getJichitaiInfo();
		if (jichitaiInfo != null) {
			jichitaiName = jichitaiInfo.getName();
		}
		jorei = reportsCommonService.getReportsDefText(ReportsConstants.TOKUGIMU_JURI_JOREI);
	}

	@Override
	public TokugimuJuriTsuchiDto getTokugimuInfo(String shiteiNo) {
		init();
		String jichitaiCode = jichitaiContext.getJichitaiCd();
		// 特別徴収義務者情報取得（最新・未削除）
		Optional<Tokugimu> tokugimuOpt = tokugimuRepository
				.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
						jichitaiCode, shiteiNo, "1", "0");

		if (tokugimuOpt.isEmpty()) {
			log.error("特別徴収義務者が見つかりません: shiteiNo={}", shiteiNo);
			return null;
		}

		Tokugimu tokugimu = tokugimuOpt.get();

		// 宛名情報取得
		Optional<Atena> atenaOpt = atenaRepository
				.findByJichitaiCdAndAtenaNo(jichitaiCode, tokugimu.getAtenaNo());

		if (atenaOpt.isEmpty()) {
			log.error("宛名情報が見つかりません: atenaNo={}", tokugimu.getAtenaNo());
			return null;
		}

		Atena atena = atenaOpt.get();

		// DTOに設定
		TokugimuJuriTsuchiDto dto = new TokugimuJuriTsuchiDto();
		dto.setShiteiNo(tokugimu.getShiteiNo());
		dto.setTokuName(atena.getName());
		dto.setBiko(tokugimu.getBiko().isEmpty() ? "" : tokugimu.getBiko());

		// 住所を郵便番号と住所で連結
		String tokuJusho = "";
		String tokuJushoWithoutYubin = "";
		if (atena.getYubinNo() != null && !atena.getYubinNo().isEmpty()) {
			tokuJusho = "〒" + atena.getYubinNo() + "\r\n";
		}
		if (atena.getJusho() != null) {
			tokuJusho += atena.getJusho();
			tokuJushoWithoutYubin = atena.getJusho();
		}
		dto.setTokuJusho(tokuJusho);
		dto.setTokuJushoWithoutYubin(tokuJushoWithoutYubin);

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
		dto.setJorei(jorei);

		return dto;
	}
}