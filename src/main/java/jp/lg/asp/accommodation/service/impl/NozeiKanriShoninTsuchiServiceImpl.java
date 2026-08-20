package jp.lg.asp.accommodation.service.impl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.NozeiKanriShoninTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.NokanRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.NozeiKanriShoninTsuchiService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
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
    private final JichitaiRepository jichitaiRepository;
    private final ReportsCommonService reportsCommonService;

    private final JichitaiContext jichitaiContext;

    @Override
    @Transactional(readOnly = true)
    public NozeiKanriShoninTsuchiDto getNozeiKanriInfo(String shiteiNo) {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        log.debug("納税管理人承認通知書情報取得開始: shiteiNo={}", shiteiNo);

        // 自治体情報をDBから取得
        Jichitai jichitai = jichitaiRepository.findById(jichitaiCd).orElse(null);
        String cityName = jichitai != null ? jichitai.getName() : "";
        // 条項を含む条例文は自治体ごとに異なるため設定値を優先し、
        // 未設定の場合のみ従来どおり自治体名からの組み立てにフォールバックする
        String jorei = reportsCommonService.getReportsDefText(ReportsConstants.NOZEI_KANRININ_SHONIN_JOREI);
        if (jorei == null || jorei.isEmpty()) {
            jorei = jichitai != null ? jichitai.getName() + "宿泊税条例" : "宿泊税条例";
        }

        NozeiKanriShoninTsuchiDto dto = new NozeiKanriShoninTsuchiDto();
        dto.setShiteiNo(shiteiNo);
        dto.setCityName(cityName);
        dto.setJorei(jorei);
        dto.setKoin(reportsCommonService.getReportsDefData(ReportsConstants.KOIN));

        // 特別徴収義務者情報を取得
        Tokugimu tokugimu = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("特別徴収義務者が見つかりません: " + shiteiNo));

        // 宛名情報を取得
        Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo())
                .orElseThrow(() -> new RuntimeException("宛名情報が見つかりません: " + tokugimu.getAtenaNo()));

        // 特別徴収義務者郵便番号・住所・名前を設定
        dto.setTokuYubin("〒"+atena.getYubinNo());
        dto.setTokuJusho(atena.getJusho());
        dto.setTokuName(atena.getName());

        // 施設郵便番号・住所・名前を設定
        dto.setShisetsuYubin("〒"+tokugimu.getShisetsuYubinNo());
        dto.setShisetsuJusho(tokugimu.getShisetsuJusho());
        dto.setShisetsuName(tokugimu.getShisetsuName());

        // 納税管理人情報を取得
		nokanRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
				.ifPresent(nokan -> {
					dto.setNozeiKanriYubin("〒"+nokan.getYubinNo());
					dto.setNozeiKanriJusho(nokan.getJusho());
					dto.setNozeiKanriName(nokan.getName());
					dto.setKbn(nokan.getKbn());
					dto.setRiyu(nokan.getRiyu());
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