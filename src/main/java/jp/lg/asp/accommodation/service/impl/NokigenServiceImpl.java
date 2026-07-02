package jp.lg.asp.accommodation.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.entity.NokigenId;
import jp.lg.asp.accommodation.repository.NokigenRepository;
import jp.lg.asp.accommodation.service.NokigenService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NokigenServiceImpl implements NokigenService {

    private final NokigenRepository nokigenRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    @Override
    @Transactional(readOnly = true)
    public List<Nokigen> findAll() {
        return nokigenRepository.findAllByJichitaiCd(jichitaiCd);
    }

    @Override
    @Transactional(readOnly = true)
    public Nokigen findByNendo(String nendo) {
        return nokigenRepository.findById(new NokigenId(jichitaiCd, nendo)).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNendo(String nendo) {
        return nokigenRepository.countByJichitaiCdAndNendo(jichitaiCd, nendo) > 0;
    }

    @Override
    @Transactional
    public Nokigen save(Nokigen nokigen) {
        nokigen.setJichitaiCd(jichitaiCd);
        // HTMLのdate入力(yyyy-MM-dd)をDBのchar(8)(yyyyMMdd)に変換
        nokigen.setNokigen1st(toDbDate(nokigen.getNokigen1st()));
        nokigen.setNokigen2nd(toDbDate(nokigen.getNokigen2nd()));
        nokigen.setNokigen3rd(toDbDate(nokigen.getNokigen3rd()));
        nokigen.setNokigen4th(toDbDate(nokigen.getNokigen4th()));
        nokigen.setNokigen5th(toDbDate(nokigen.getNokigen5th()));
        nokigen.setNokigen6th(toDbDate(nokigen.getNokigen6th()));
        nokigen.setNokigen7th(toDbDate(nokigen.getNokigen7th()));
        nokigen.setNokigen8th(toDbDate(nokigen.getNokigen8th()));
        nokigen.setNokigen9th(toDbDate(nokigen.getNokigen9th()));
        nokigen.setNokigen10th(toDbDate(nokigen.getNokigen10th()));
        nokigen.setNokigen11th(toDbDate(nokigen.getNokigen11th()));
        nokigen.setNokigen12th(toDbDate(nokigen.getNokigen12th()));
        return nokigenRepository.save(nokigen);
    }

    /** yyyy-MM-dd → yyyyMMdd 変換。null/空の場合は空文字を返す */
    private String toDbDate(String value) {
        if (value == null || value.isBlank()) return "";
        return value.replace("-", "");
    }
}
