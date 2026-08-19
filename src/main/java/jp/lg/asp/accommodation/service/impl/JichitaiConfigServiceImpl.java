package jp.lg.asp.accommodation.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.JichitaiConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.JichitaiConfigService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JichitaiConfigServiceImpl implements JichitaiConfigService {

    private final JichitaiRepository jichitaiRepository;

    @Override
    @Transactional(readOnly = true)
    public Jichitai findById(String jichitaiCd) {
        return jichitaiRepository.findById(jichitaiCd).orElseThrow();
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
}
