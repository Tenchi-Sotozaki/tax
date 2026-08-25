package jp.lg.asp.accommodation.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.JichitaiConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.JichitaiConfigService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JichitaiConfigServiceImpl implements JichitaiConfigService {

	private final JichitaiRepository jichitaiRepository;
	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public Jichitai getCurrentJichitai() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return jichitaiRepository.findById(jichitaiCd).orElseThrow();
	}

	@Override
	@Transactional(readOnly = true)
	public JichitaiConfigDto getJichitaiConfigDto() {

	    JichitaiConfigDto form = new JichitaiConfigDto();

	    // デフォルト値
	    form.setNendoStMonth("3");

	    String jichitaiCd = jichitaiContext.getJichitaiCd();

	    if (jichitaiCd == null) {
	        return form;
	    }

	    return jichitaiRepository.findById(jichitaiCd)
	            .map(jichitai -> {

	                form.setJichitaiCd(jichitai.getJichitaiCd());
	                form.setName(jichitai.getName());
	                form.setKbnName(jichitai.getKbnName());
	                form.setParam(jichitai.getParam());

	                form.setNendoStMonth(
	                    jichitai.getNendoStMonth() == null
	                        ? "3"
	                        : jichitai.getNendoStMonth().trim()
	                );

	                form.setNozeiShuki(jichitai.getNozeiShuki());
	                form.setShiteiStChar(jichitai.getShiteiStChar());
	                form.setGassanStChar(jichitai.getGassanStChar());
	                form.setAtenaStNo(jichitai.getAtenaStNo());

	                return form;
	            })
	            .orElse(form);
	}

	@Override
	@Transactional
	public void saveJichitaiConfig(JichitaiConfigDto configForm) {

	    Jichitai jichitai = jichitaiRepository
	            .findById(configForm.getJichitaiCd())
	            .orElseGet(Jichitai::new);

	    jichitai.setJichitaiCd(configForm.getJichitaiCd());

	    copyProperties(configForm, jichitai);

	    jichitaiRepository.save(jichitai);
	}

	private void copyProperties(JichitaiConfigDto src, Jichitai target) {
	    target.setName(src.getName());
	    target.setKbnName(src.getKbnName());
	    target.setNendoStMonth(src.getNendoStMonth());
	    target.setNozeiShuki(src.getNozeiShuki());
	    target.setShiteiStChar(src.getShiteiStChar());
	    target.setGassanStChar(src.getGassanStChar());
	    target.setAtenaStNo(src.getAtenaStNo());

	    target.setParam(src.getParam());
	}
}