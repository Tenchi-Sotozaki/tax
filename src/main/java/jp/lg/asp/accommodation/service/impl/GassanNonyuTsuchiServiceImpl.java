package jp.lg.asp.accommodation.service.impl;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.GassanNonyuTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.entity.NokigenId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.NokigenRepository;
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
	private final NokigenRepository nokigenRepository;
	private final ReportsCommonService reportsCommonService;

	private final JichitaiContext jichitaiContext;

	private String cityName;
	private String jorei;

	private void init() {
		Jichitai jichitaiInfo = reportsCommonService.getJichitaiInfo();
		if (jichitaiInfo != null) {
			cityName = jichitaiInfo.getName();
		}
		jorei = reportsCommonService.getReportsDefText(ReportsConstants.GASSAN_NONYU_JOREI);
	}

	@Override
	public GassanNonyuTsuchiDto getGassanNonyuTsuchiInfo(String shiteiNo) {
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
			LocalDate tekiyoStYmd = gassan.getTekiyoStYmd();
			dto.setTekiyoStYmd(tekiyoStYmd);

			// 納入期限を取得
			Jichitai jichitaiInfo = reportsCommonService.getJichitaiInfo();
			if (jichitaiInfo != null && tekiyoStYmd != null) {

				// nendo_st_month は char(2) のためパディングが残る。他の参照箇所と同じくトリムしてから変換する
				int nendoStMonth = Integer.parseInt(jichitaiInfo.getNendoStMonth().trim());
				int targetMonth = tekiyoStYmd.getMonthValue();
				int ki = ((targetMonth - nendoStMonth + 12) % 12) + 1;
				int nendo = tekiyoStYmd.getYear();
				if (targetMonth < nendoStMonth) {
					nendo--;
				}
				Optional<Nokigen> nokigenOpt = nokigenRepository.findById(new NokigenId(jichitaiCode, String.valueOf(nendo)));
				if (nokigenOpt.isPresent()) {
					Nokigen nokigen = nokigenOpt.get();
					String nokigenYmd = getNokigenByKi(nokigen, ki);
					if (nokigenYmd != null && nokigenYmd.length() == 8) {
						int month = Integer.parseInt(nokigenYmd.substring(4, 6));
						int day = Integer.parseInt(nokigenYmd.substring(6, 8));
						dto.setNonyuKigen(month + "月" + day + "日");
					}
				}
			}
		}

		dto.setCity(cityName);
		dto.setJorei(jorei);
		dto.setKoin(reportsCommonService.getReportsDefData(ReportsConstants.KOIN));;

		return dto;
	}

	private String getNokigenByKi(Nokigen nokigen, int ki) {
		switch (ki) {
			case 1: return nokigen.getNokigen1st();
			case 2: return nokigen.getNokigen2nd();
			case 3: return nokigen.getNokigen3rd();
			case 4: return nokigen.getNokigen4th();
			case 5: return nokigen.getNokigen5th();
			case 6: return nokigen.getNokigen6th();
			case 7: return nokigen.getNokigen7th();
			case 8: return nokigen.getNokigen8th();
			case 9: return nokigen.getNokigen9th();
			case 10: return nokigen.getNokigen10th();
			case 11: return nokigen.getNokigen11th();
			case 12: return nokigen.getNokigen12th();
			default: return null;
		}
	}
}
