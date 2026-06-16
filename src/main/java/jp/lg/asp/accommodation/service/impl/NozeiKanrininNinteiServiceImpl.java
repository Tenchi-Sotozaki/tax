package jp.lg.asp.accommodation.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.NozeiKanrininNinteiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 納税管理人選任免除認定（不認定）通知書 Service実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NozeiKanrininNinteiServiceImpl implements NozeiKanrininNinteiService {

    private final TokugimuRepository tokugimuRepository;
    private final AtenaRepository atenaRepository;
    private final JichitaiRepository jichitaiRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    @Override
    @Transactional(readOnly = true)
    public NozeiKanrininNinteiDto getNinteiInfo(String shiteiNo) {
        log.debug("納税管理人選任免除認定通知書情報取得開始: shiteiNo={}", shiteiNo);

        Jichitai jichitai = jichitaiRepository.findById(jichitaiCd).orElse(null);
        String cityName = jichitai != null ? jichitai.getName() : "";
        String jorei = jichitai != null ? jichitai.getName() + "宿泊税条例" : "宿泊税条例";

        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setShiteiNo(shiteiNo);
        dto.setCityName(cityName);
        dto.setJorei(jorei);
        dto.setNintei("認定");

        // 特別徴収義務者情報を取得
        Tokugimu tokugimu = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("特別徴収義務者が見つかりません: " + shiteiNo));

        // 宛名情報を取得
        Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo())
                .orElseThrow(() -> new RuntimeException("宛名情報が見つかりません: " + tokugimu.getAtenaNo()));

        dto.setTokuJusho(buildAddress(atena.getYubinNo(), atena.getJusho()));
        dto.setTokuName(atena.getName());
        dto.setShisetsuJusho(buildAddress(tokugimu.getShisetsuYubinNo(), tokugimu.getShisetsuJusho()));
        dto.setShisetsuName(tokugimu.getShisetsuName());

        log.debug("納税管理人選任免除認定通知書情報取得完了: {}", dto);
        return dto;
    }

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
