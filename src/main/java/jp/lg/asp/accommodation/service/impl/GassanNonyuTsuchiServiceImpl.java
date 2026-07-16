package jp.lg.asp.accommodation.service.impl;

import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.GassanNonyuTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.GassanNonyuTsuchiService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 合算申告納入承認通知書 Service 実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GassanNonyuTsuchiServiceImpl implements GassanNonyuTsuchiService {

	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final GassanRepository gassanRepository;
	private final ReportsCommonService reportsCommonService;

	@Value("${app.jichitai.code}")
	private String jichitaiCode;

	private String cityName;
	private String jorei;

	@PostConstruct
	public void init() {
		Jichitai jichitaiInfo = reportsCommonService.getJichitaiInfo();
		if (jichitaiInfo != null) {
			cityName = jichitaiInfo.getName();
		}
		jorei = reportsCommonService.getReportsDefText(ReportsConstants.TOKUREI_SHITEI_JOREI);
	}

	@Override
	public GassanNonyuTsuchiDto getGassanNonyuTsuchiInfo(String shiteiNo) {
		Optional<Tokugimu> tokugimuOpt = tokugimuRepository
				.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
						jichitaiCode, shiteiNo, "1", "0");

		if (tokugimuOpt.isEmpty()) {
			log.warn("特別徴収義務者が見つかりません: shiteiNo={}", shiteiNo);
			return null;
		}

		Tokugimu tokugimu = tokugimuOpt.get();

		Optional<Atena> atenaOpt = atenaRepository
				.findByJichitaiCdAndAtenaNo(jichitaiCode, tokugimu.getAtenaNo());

		if (atenaOpt.isEmpty()) {
			log.warn("宛名情報が見つかりません: atenaNo={}", tokugimu.getAtenaNo());
			return null;
		}

		Atena atena = atenaOpt.get();

		// 合算情報を取得
		List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndShiteiNo(jichitaiCode, shiteiNo);

		GassanNonyuTsuchiDto dto = new GassanNonyuTsuchiDto();
		dto.setShiteiNo(shiteiNo);
		dto.setTokuName(atena.getName());

		String tokuJusho = "";
		if (atena.getYubinNo() != null && !atena.getYubinNo().isEmpty()) {
			tokuJusho = "〒" + atena.getYubinNo() + "\r\n";
		}
		if (atena.getJusho() != null) {
			tokuJusho += atena.getJusho();
		}
		dto.setTokuJusho(tokuJusho);

		if (!gassanList.isEmpty()) {
			Gassan gassan = gassanList.get(0);
			dto.setGassanShiteiNo(gassan.getGassanShiteiNo());
			dto.setTekiyoStYmd(gassan.getTekiyoStYmd());
		}

		dto.setCity(cityName);
		dto.setJorei(jorei);
		dto.setKoin(reportsCommonService.getReportsDefData(ReportsConstants.KOIN));;

		return dto;
	}
}
