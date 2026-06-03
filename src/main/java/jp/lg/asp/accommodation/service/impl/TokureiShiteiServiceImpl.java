package jp.lg.asp.accommodation.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.TokureiShiteiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
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

	@Value("${app.jichitai.code}")
	private String jichitaiCode;

	@Value("${app.jichitai.city-name}")
	private String cityName;

	@Value("${app.jichitai.jorei.tokurei-shitei}")
	private String jorei;

	@Override
	public TokureiShiteiDto getTokugimuInfo(String shiteiNo) {
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

		TokureiShiteiDto dto = new TokureiShiteiDto();
		dto.setShiteiNo(tokugimu.getShiteiNo());
		dto.setTokuName(atena.getName());

		String tokuJusho = "";
		if (atena.getYubinNo() != null && !atena.getYubinNo().isEmpty()) {
			tokuJusho = "〒" + atena.getYubinNo() + "\r\n";
		}
		if (atena.getJusho() != null) {
			tokuJusho += atena.getJusho();
		}
		dto.setTokuJusho(tokuJusho);

		dto.setShisetsuName(tokugimu.getShisetsuName());

		String shisetsuJusho = "";
		if (tokugimu.getShisetsuYubinNo() != null && !tokugimu.getShisetsuYubinNo().isEmpty()) {
			shisetsuJusho = "〒" + tokugimu.getShisetsuYubinNo() + "\r\n";
		}
		if (tokugimu.getShisetsuJusho() != null) {
			shisetsuJusho += tokugimu.getShisetsuJusho();
		}
		dto.setShisetsuJusho(shisetsuJusho);

		dto.setCity(cityName);
		dto.setJorei(jorei);

		return dto;
	}
}
