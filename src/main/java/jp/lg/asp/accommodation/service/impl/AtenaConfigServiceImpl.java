package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.exception.BusinessException;
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

    public Atena register(Atena atena, String jichitaiCd) {
        // 自治体存在チェック
        Jichitai jichitai = jichitaiRepository.findById(jichitaiCd)
                .orElseThrow(() -> new ResourceNotFoundException("自治体情報が見つかりません。"));

        // 個人番号の重複チェック & ハッシュ化
        if (atena.getKojinNo() != null && !atena.getKojinNo().isEmpty()) {
            String hashedKojinNo = hashUtil.sha256(atena.getKojinNo());
            if (atenaRepository.existsByKojinNo(jichitaiCd, hashedKojinNo, null)) {
                throw new BusinessException("DUPLICATE_KOJIN_NO", "この個人番号はすでに登録されています。");
            }
            atena.setKojinNo(hashedKojinNo);
            atena.setKbn("1");
        } 
        // 法人の重複チェックを追加
        else if (atena.getHojinNo() != null && !atena.getHojinNo().isEmpty()) {
            if (atenaRepository.existsByHojinNo(jichitaiCd, atena.getHojinNo(), null)) {
                throw new BusinessException("DUPLICATE_HOJIN_NO", "この法人番号はすでに登録されています。");
            }
            atena.setKbn("2");
        }

        // 採番処理などの後続ロジック...
        BigDecimal atenaNo = atenaRepository.findMaxAtenaNoByJichitaiCd(jichitaiCd)
                .map(max -> max.add(BigDecimal.ONE))
                .orElseGet(() -> {
                    if (jichitai.getAtenaStNo() == null) {
                        throw new BusinessException("DUPLICATE_ATENAST_NO", "宛名の開始番号が設定されていません。管理者にお問い合わせください。");
                    }
                    return jichitai.getAtenaStNo();
                });

        atena.setJichitaiCd(jichitaiCd);
        atena.setAtenaNo(atenaNo);

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

        String kojinNo = atena.getKojinNo();
        boolean hasKojinNo = (kojinNo != null) && (!kojinNo.isBlank());

        if (hasKojinNo) {
            String hashed = hashUtil.sha256(kojinNo);
            if (atenaRepository.existsByKojinNo(jichitaiCd, hashed, atena.getAtenaNo())) {
                throw new BusinessException("DUPLICATE_KOJIN_NO", "この個人番号はすでに登録されています。");
            }
            atena.setKojinNo(hashed);
            atena.setKbn("1");
        } else {
            String hojinNo = atena.getHojinNo();
            boolean hasHojinNo = (hojinNo != null) && (!hojinNo.isBlank());
            if (hasHojinNo && atenaRepository.existsByKojinNo(jichitaiCd, hojinNo, atena.getAtenaNo())) {
                throw new BusinessException("DUPLICATE_HOJIN_NO", "この法人番号はすでに登録されています。");
            }
            atena.setKojinNo(existing.getKojinNo());
            atena.setKbn(existing.getKbn());
        }

        return atenaRepository.save(atena);
    }
}