package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.AtenaConfigForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.service.AtenaConfigService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtenaConfigServiceImpl implements AtenaConfigService {

	private final AtenaRepository atenaRepository;
	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public AtenaConfigForm findByAtenaNo(BigDecimal atenaNo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atenaNo)
				.orElseThrow(() -> new IllegalArgumentException("宛名情報が見つかりません。"));
		AtenaConfigForm form = new AtenaConfigForm();
		form.setAtenaNo(atena.getAtenaNo());
		form.setKojinNo(atena.getKojinNo());
		form.setHojinNo(atena.getHojinNo());
		form.setName(atena.getName());
		form.setNameKana(atena.getNameKana());
		form.setYubinNo(atena.getYubinNo());
		form.setJusho(atena.getJusho());
		form.setTel1(atena.getTel1());
		form.setTel2(atena.getTel2());
		form.setKbn(atena.getKbn());
		return form;
	}

	@Override
	@Transactional
	public void register(AtenaConfigForm form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		BigDecimal nextNo = atenaRepository.findAll().stream()
				.filter(a -> a.getJichitaiCd().equals(jichitaiCd))
				.map(Atena::getAtenaNo)
				.max(BigDecimal::compareTo)
				.map(max -> max.add(BigDecimal.ONE))
				.orElse(BigDecimal.ONE);

		Atena atena = new Atena();
		atena.setJichitaiCd(jichitaiCd);
		atena.setAtenaNo(nextNo);
		atena.setKbn(form.getKbn() != null ? form.getKbn() : "1");
		atena.setKojinNo(form.getKojinNo());
		atena.setHojinNo(form.getHojinNo());
		atena.setName(form.getName());
		atena.setNameKana(form.getNameKana());
		atena.setYubinNo(form.getYubinNo());
		atena.setJusho(form.getJusho());
		atena.setTel1(form.getTel1());
		atena.setTel2(form.getTel2());
		atenaRepository.save(atena);
	}

	@Override
	@Transactional
	public void update(BigDecimal atenaNo, AtenaConfigForm form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atenaNo)
				.orElseThrow(() -> new IllegalArgumentException("宛名情報が見つかりません。"));
		atena.setKojinNo(form.getKojinNo());
		atena.setHojinNo(form.getHojinNo());
		atena.setName(form.getName());
		atena.setNameKana(form.getNameKana());
		atena.setYubinNo(form.getYubinNo());
		atena.setJusho(form.getJusho());
		atena.setTel1(form.getTel1());
		atena.setTel2(form.getTel2());
		atenaRepository.save(atena);
	}
}
