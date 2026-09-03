package jp.lg.asp.accommodation.service.impl;

import java.util.Optional;

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
    public Optional<Jichitai> findById(String jichitaiCd) {
        return jichitaiRepository.findById(jichitaiCd);
    }

    @Override
    @Transactional
    public void save(String currentJichitaiCd, JichitaiConfigDto configForm) {
        Jichitai jichitai = jichitaiRepository.findById(currentJichitaiCd).orElseThrow();
        String newJichitaiCd = configForm.getJichitaiCd();
        boolean cdChanged = !currentJichitaiCd.equals(newJichitaiCd);

        if (cdChanged) {
            Jichitai newJichitai = new Jichitai();
            newJichitai.setJichitaiCd(newJichitaiCd);
            newJichitai.setName(configForm.getName());
            newJichitai.setKbnName(configForm.getKbnName());
            newJichitai.setNendoStMonth(configForm.getNendoStMonth());
            newJichitai.setNozeiShuki(configForm.getNozeiShuki());
            newJichitai.setShiteiStChar(configForm.getShiteiStChar());
            newJichitai.setGassanStChar(configForm.getGassanStChar());
            newJichitai.setAtenaStNo(configForm.getAtenaStNo());
            jichitaiRepository.save(newJichitai);
            jichitaiRepository.delete(jichitai);
        } else {
            jichitai.setName(configForm.getName());
            jichitai.setKbnName(configForm.getKbnName());
            jichitai.setNendoStMonth(configForm.getNendoStMonth());
            jichitai.setNozeiShuki(configForm.getNozeiShuki());
            jichitai.setShiteiStChar(configForm.getShiteiStChar());
            jichitai.setGassanStChar(configForm.getGassanStChar());
            jichitai.setAtenaStNo(configForm.getAtenaStNo());
            jichitaiRepository.save(jichitai);
        }
    }

	@Override
	@Transactional(readOnly = true)
	public Jichitai getCurrentJichitai() {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		return jichitaiRepository.findById(jichitaiCd).orElseThrow();
	}

	@Override
	@Transactional(readOnly = true)
	public JichitaiConfigDto getJichitaiConfigDtoById(String jichitaiCd) {
	    return jichitaiRepository.findById(jichitaiCd)
	            .map(j -> {
	                JichitaiConfigDto form = new JichitaiConfigDto();
	                form.setJichitaiCd(j.getJichitaiCd());
	                form.setName(j.getName());
	                form.setKbnName(j.getKbnName());
	                form.setParam(j.getParam());
	                form.setUserId(j.getUserName());
	                form.setNendoStMonth(j.getNendoStMonth() == null ? "3" : j.getNendoStMonth().trim());
	                form.setNozeiShuki(j.getNozeiShuki());
	                form.setShiteiStChar(j.getShiteiStChar());
	                form.setGassanStChar(j.getGassanStChar());
	                form.setAtenaStNo(j.getAtenaStNo());
	                return form;
	            }).orElseGet(this::getJichitaiConfigDto);
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
	                form.setUserId(jichitai.getUserName());

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
	    jichitai.setUserName(configForm.getUserId());
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
