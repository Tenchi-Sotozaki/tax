package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.exception.ResourceNotFoundException;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.AtenaConfigService;
import jp.lg.asp.accommodation.util.HashUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtenaConfigServiceImpl implements AtenaConfigService {

    private final AtenaRepository atenaRepository;
    private final JichitaiRepository jichitaiRepository;
    private final HashUtil hashUtil;

    @Override
    public Atena findByAtenaNo(String jichitaiCd, BigDecimal atenaNo) {
        return atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atenaNo)
                .orElseThrow(() -> new ResourceNotFoundException("宛名が見つかりません。"));
    }

    @Override
    @Transactional
    public Atena register(Atena atena, String jichitaiCd) {
        Jichitai jichitai = jichitaiRepository.findById(jichitaiCd)
                .orElseThrow(() -> new ResourceNotFoundException("自治体情報が見つかりません。"));

        BigDecimal nextNo = jichitai.getAtenaStNo() != null
                ? jichitai.getAtenaStNo().add(BigDecimal.ONE)
                : BigDecimal.ONE;

        jichitai.setAtenaStNo(nextNo);
        jichitaiRepository.save(jichitai);

        atena.setJichitaiCd(jichitaiCd);
        atena.setAtenaNo(nextNo);
        if (atena.getKojinNo() != null && !atena.getKojinNo().isBlank()) {
            atena.setKojinNo(hashUtil.sha256(atena.getKojinNo()));
            atena.setKbn("1");
        } else {
            atena.setKbn("2");
        }

        return atenaRepository.save(atena);
    }

    @Override
    @Transactional
    public Atena update(Atena atena, String jichitaiCd) {
        AtenaId id = new AtenaId();
        id.setJichitaiCd(jichitaiCd);
        id.setAtenaNo(atena.getAtenaNo());
        Atena existing = atenaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("宛名が見つかりません。"));

        atena.setJichitaiCd(jichitaiCd);
        if (atena.getKojinNo() != null && !atena.getKojinNo().isBlank()) {
            atena.setKojinNo(hashUtil.sha256(atena.getKojinNo()));
            atena.setKbn("1");
        } else {
            atena.setKojinNo(existing.getKojinNo());
            atena.setKbn(existing.getKbn());
        }

        return atenaRepository.save(atena);
    }
}
