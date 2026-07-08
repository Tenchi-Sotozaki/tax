package jp.lg.asp.accommodation.service.impl;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.KofuRitsuConfigDto;
import jp.lg.asp.accommodation.entity.KofuRitsu;
import jp.lg.asp.accommodation.repository.KofuRitsuRepository;
import jp.lg.asp.accommodation.service.KofuRitsuConfigService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KofuRitsuConfigServiceImpl implements KofuRitsuConfigService {

	private final KofuRitsuRepository kofuRitsuRepository;

	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public KofuRitsu findCurrent() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return kofuRitsuRepository.findCurrentByJichitaiCd(jichitaiCd).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public List<KofuRitsu> findAll() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return kofuRitsuRepository.findAllByJichitaiCd(jichitaiCd);
	}

	@Override
	@Transactional
	public void register(KofuRitsuConfigDto dto) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		// 既存の最新レコードのnew_flgを0に更新
		kofuRitsuRepository.findCurrentByJichitaiCd(jichitaiCd).ifPresent(current -> {
			current.setNewFlg(0);
			kofuRitsuRepository.save(current);
		});

		// 新規レコードをINSERT
		BigDecimal nextRno = kofuRitsuRepository.findNextRno(jichitaiCd);
		KofuRitsu entity = new KofuRitsu();
		entity.setJichitaiCd(jichitaiCd);
		entity.setRno(nextRno);
		entity.setKofuRitsu(dto.getKofuRitsu());
		entity.setTekiyoStYmd(dto.getTekiyoStYmd());
		entity.setTekiyoEdYmd(dto.getTekiyoEdYmd());
		entity.setNewFlg(1);
		kofuRitsuRepository.save(entity);
	}
}
