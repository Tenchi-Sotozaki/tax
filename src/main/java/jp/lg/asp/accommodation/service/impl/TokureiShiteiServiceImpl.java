package jp.lg.asp.accommodation.service.impl;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.TokureiShiteiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.service.TokureiShiteiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 納入申告書の提出期限等の特例適用者指定通知 Service 実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokureiShiteiServiceImpl implements TokureiShiteiService {

	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final ReportsCommonService reportsCommonService;

	private final JichitaiContext jichitaiContext;

	private String cityName;
	private String jorei;
	private byte[] koin;

	private void init() {
		Jichitai jichitaiInfo = reportsCommonService.getJichitaiInfo();
		if (jichitaiInfo != null) {
			cityName = jichitaiInfo.getName();
		}
		jorei = reportsCommonService.getReportsDefText(ReportsConstants.TOKUREI_SHITEI_JOREI);
		koin = reportsCommonService.getReportsDefData(ReportsConstants.KOIN);
	}

	@Override
	public TokureiShiteiDto getTokugimuInfo(String shiteiNo) {
		init();
		String jichitaiCode = jichitaiContext.getJichitaiCd();
		Optional<Tokugimu> tokugimuOpt = tokugimuRepository
				.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
						jichitaiCode, shiteiNo, "1", "0");

		if (tokugimuOpt.isEmpty()) {
			log.error("特別徴収義務者が見つかりません: shiteiNo={}", shiteiNo);
			return null;
		}

		Tokugimu tokugimu = tokugimuOpt.get();

		Optional<Atena> atenaOpt = atenaRepository
				.findByJichitaiCdAndAtenaNo(jichitaiCode, tokugimu.getAtenaNo());

		if (atenaOpt.isEmpty()) {
			log.error("宛名情報が見つかりません: atenaNo={}", tokugimu.getAtenaNo());
			return null;
		}

		Atena atena = atenaOpt.get();

		TokureiShiteiDto dto = new TokureiShiteiDto();
		dto.setShiteiNo(shiteiNo);
		dto.setTokuName(atena.getName());

		// 特別徴収義務者の郵便番号の設定
		if (atena.getYubinNo() != null && !atena.getYubinNo().isEmpty()) {
			dto.setTokuYubin("〒" + atena.getYubinNo());
		}
	
		// 特別徴収義務者の住所の設定
		if (atena.getJusho() != null) {
			dto.setTokuJusho(atena.getJusho());
		}

		dto.setShisetsuName(tokugimu.getShisetsuName());

		// 施設郵便番号の設定
		if (tokugimu.getShisetsuYubinNo() != null && !tokugimu.getShisetsuYubinNo().isEmpty()) {
			dto.setShisetsuYubin("〒" + tokugimu.getShisetsuYubinNo());
		}
		
		// 施設住所の設定
		if (tokugimu.getShisetsuJusho() != null) {
			dto.setShisetsuJusho(tokugimu.getShisetsuJusho());
		}

		dto.setCity(cityName);
		dto.setJorei(jorei);
		dto.setKoin(koin);

		return dto;
	}
}
