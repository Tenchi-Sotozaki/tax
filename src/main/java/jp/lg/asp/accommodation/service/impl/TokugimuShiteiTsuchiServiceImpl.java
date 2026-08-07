package jp.lg.asp.accommodation.service.impl;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.config.JichitaiContext;
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

	private final JichitaiContext jichitaiContext;

	private String city;
	private String jichitaiName;
	private String jorei;
	private byte[] koin;

	private void init() {
		Jichitai jichitaiInfo = reportsCommonService.getJichitaiInfo();
		if (jichitaiInfo != null) {
			jichitaiName = jichitaiInfo.getName();
			city = jichitaiInfo.getKbnName();
		}
		jorei = reportsCommonService.getReportsDefText(ReportsConstants.TOKUGIMU_SHITEI_JOREI);
		koin = reportsCommonService.getReportsDefData(ReportsConstants.KOIN);
	}

	@Override
	public TokugimuShiteiTsuchiDto getTokugimuInfo(String shiteiNo) {
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
		TokugimuShiteiTsuchiDto dto = new TokugimuShiteiTsuchiDto();
		dto.setShiteiNo(tokugimu.getShiteiNo());
		dto.setTokuName(atena.getName());
		dto.setTokuYubinNo(atena.getYubinNo() != null ? atena.getYubinNo() : "");
		dto.setTokuJusho(atena.getJusho() != null ? atena.getJusho() : "");

		dto.setShisetsuName(tokugimu.getShisetsuName());

		dto.setShisetsuYubinNo(tokugimu.getShisetsuYubinNo() != null ? tokugimu.getShisetsuYubinNo() : "");
		dto.setShisetsuJusho(tokugimu.getShisetsuJusho() != null ? tokugimu.getShisetsuJusho() : "");

		// application.ymlから取得する値
		dto.setCityName(jichitaiName);
		dto.setCity(city);
		dto.setJorei(jorei);
		dto.setKoin(koin);
		
		return dto;
	}
}