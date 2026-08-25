package jp.lg.asp.accommodation.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.ShiteiGassanConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.ShiteiGassanConfigService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShiteiGassanConfigServiceImpl implements ShiteiGassanConfigService {

    private final JichitaiRepository jichitaiRepository;

    @Override
    @Transactional(readOnly = true)
    public Jichitai findById(String jichitaiCd) {
        return jichitaiRepository.findById(jichitaiCd).orElse(null);
    }

    @Override
    @Transactional
    public void save(String jichitaiCd, ShiteiGassanConfigDto dto) {
        Jichitai jichitai = jichitaiRepository.findById(jichitaiCd)
                .orElseThrow(() -> new IllegalStateException("自治体情報が見つかりません"));
        jichitai.setShiteiStChar(dto.getShiteiStChar());
        jichitai.setGassanStChar(dto.getGassanStChar());
        jichitaiRepository.save(jichitai);
    }
}
