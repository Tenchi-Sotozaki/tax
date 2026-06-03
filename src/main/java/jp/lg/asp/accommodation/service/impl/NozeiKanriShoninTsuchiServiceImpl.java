package jp.lg.asp.accommodation.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.NozeiKanriShoninTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Nokan;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.NokanRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.NozeiKanriShoninTsuchiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 納税管理人承認(不承認)通知書 Service実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NozeiKanriShoninTsuchiServiceImpl implements NozeiKanriShoninTsuchiService {

    private final TokugimuRepository tokugimuRepository;
    private final AtenaRepository atenaRepository;
    private final NokanRepository nokanRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    @Value("${app.city.name:#{null}}")
    private String cityName;

    @Value("${app.jorei:#{null}}")
    private String jorei;

    @Override
    @Transactional(readOnly = true)
    public NozeiKanriShoninTsuchiDto getNozeiKanriInfo(String shiteiNo) {
        log.debug("納税管理人承認通知書情報取得開始: shiteiNo={}", shiteiNo);

        NozeiKanriShoninTsuchiDto dto = new NozeiKanriShoninTsuchiDto();
        dto.setShiteiNo(shiteiNo);
        dto.setCityName(cityName);
        dto.setJorei(jorei);

        // 特別徴収義務者情報を取得
        Tokugimu tokugimu = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("特別徴収義務者が見つかりません: " + shiteiNo));

        // 宛名情報を取得
        Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo())
                .orElseThrow(() -> new RuntimeException("宛名情報が見つかりません: " + tokugimu.getAtenaNo()));

        // 特別徴収義務者住所・名前を設定
        dto.setTokuJusho(buildAddress(atena.getYubinNo(), atena.getJusho()));
        dto.setTokuName(atena.getName());

        // 施設住所・名前を設定
        dto.setShisetsuJusho(buildAddress(tokugimu.getShisetsuYubinNo(), tokugimu.getShisetsuJusho()));
        dto.setShisetsuName(tokugimu.getShisetsuName());

        // 納税管理人情報を取得
        nokanRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .ifPresent(nokan -> {
                    dto.setNozeiKanriJusho(buildAddress(nokan.getYubinNo(), nokan.getJusho()));
                    dto.setNozeiKanriName(nokan.getName());
                });

        log.debug("納税管理人承認通知書情報取得完了: {}", dto);
        return dto;
    }

    /**
     * 郵便番号と住所を連結してフォーマット
     */
    private String buildAddress(String yubinNo, String jusho) {
        if (yubinNo != null && !yubinNo.isEmpty() && jusho != null && !jusho.isEmpty()) {
            return "〒" + yubinNo + " " + jusho;
        } else if (jusho != null && !jusho.isEmpty()) {
            return jusho;
        } else {
            return "";
        }
    }
}