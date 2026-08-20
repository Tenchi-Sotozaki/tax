package jp.lg.asp.accommodation.service.impl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.NozeiKanrininNinteiService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
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
    private final ReportsCommonService reportsCommonService;

    private final JichitaiContext jichitaiContext;

    @Override
    @Transactional(readOnly = true)
    public NozeiKanrininNinteiDto getNinteiInfo(String shiteiNo) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        log.debug("納税管理人選任免除認定通知書情報取得開始: shiteiNo={}", shiteiNo);

        Jichitai jichitai = jichitaiRepository.findById(jichitaiCd).orElse(null);
        String cityName = jichitai != null ? jichitai.getName() : "";
        // 条項を含む条例文は自治体ごとに異なるため設定値を優先し、
        // 未設定の場合のみ従来どおり自治体名からの組み立てにフォールバックする
        String jorei = reportsCommonService.getReportsDefText(ReportsConstants.NOZEI_KANRININ_NINTEI_JOREI);
        if (jorei == null || jorei.isEmpty()) {
            jorei = jichitai != null ? jichitai.getName() + "宿泊税条例" : "宿泊税条例";
        }

        NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
        dto.setShiteiNo(shiteiNo);
        dto.setCityName(cityName);
        dto.setJorei(jorei);
        dto.setNintei("認定");
        dto.setKoin(reportsCommonService.getReportsDefData(ReportsConstants.KOIN));

        // 特別徴収義務者情報を取得
        Tokugimu tokugimu = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("特別徴収義務者が見つかりません: " + shiteiNo));

        // 宛名情報を取得
        Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo())
                .orElseThrow(() -> new RuntimeException("宛名情報が見つかりません: " + tokugimu.getAtenaNo()));

        dto.setTokuYubinNo(atena.getYubinNo() != null ? atena.getYubinNo() : "");
        dto.setTokuJusho(atena.getJusho() != null ? atena.getJusho() : "");
        dto.setTokuName(atena.getName());
        dto.setShisetsuYubinNo(tokugimu.getShisetsuYubinNo() != null ? tokugimu.getShisetsuYubinNo() : "");
        dto.setShisetsuJusho(tokugimu.getShisetsuJusho() != null ? tokugimu.getShisetsuJusho() : "");
        dto.setShisetsuName(tokugimu.getShisetsuName());

        log.debug("納税管理人選任免除認定通知書情報取得完了: {}", dto);
        return dto;
    }
}
