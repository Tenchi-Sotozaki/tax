package jp.lg.asp.accommodation.service.impl;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiDto;
import jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto;
import jp.lg.asp.accommodation.service.KanpuMenjoTsuchiReportsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * 徴収不能額の還付又は納入義務の免除決定通知書PDF生成 Service実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KanpuMenjoTsuchiReportsServiceImpl implements KanpuMenjoTsuchiReportsService {

    private static final String JRXML_PATH = "reports/kanpuMenjoTsuchi.jrxml";

    @Override
    public byte[] generateTsuchiPdf(KanpuMenjoTsuchiDto dto) {
        try {
            InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            Map<String, Object> parameters = new HashMap<>();
            // 帳票で丸印の表示に使用するため、データソースとは別に、パラメータマップへ格納
            parameters.put("shinsei_kbn", dto.getShinsei_kbn() != null ? dto.getShinsei_kbn() : "");
            parameters.put("kettei_naiyou", dto.getKettei_naiyou() != null ? dto.getKettei_naiyou() : "");
            
            JRDataSource dataSource = buildParams(dto);
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport, parameters, dataSource);

            byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);
            return pdf;

        } catch (Exception e) {
            log.error("徴収不能額の還付又は納入義務の免除決定通知書PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
            throw new RuntimeException("PDF生成に失敗しました: " + e.getMessage(), e);
        }
    }

    private JRDataSource buildParams(KanpuMenjoTsuchiDto dto) {
        KanpuMenjoTsuchiReportsDto reportsDto = new KanpuMenjoTsuchiReportsDto();

        // 基本情報
        reportsDto.setCityName(dto.getCityName() != null ? dto.getCityName() : "");
        reportsDto.setJorei(dto.getJorei() != null ? dto.getJorei() : "");
        reportsDto.setTokuName(dto.getTokuName() != null ? dto.getTokuName() : "");
        reportsDto.setTokuJusho(dto.getTokuJusho() != null ? dto.getTokuJusho() : "");
        reportsDto.setShisetsuJusho(dto.getShisetsuJusho() != null ? dto.getShisetsuJusho() : "");
        reportsDto.setShisetsuName(dto.getShisetsuName() != null ? dto.getShisetsuName() : "");
        reportsDto.setZeigaku(dto.getZeigaku() != null ? dto.getZeigaku() : "");
        reportsDto.setKanpuMenjoGaku(dto.getKanpuMenjoGaku() != null ? dto.getKanpuMenjoGaku() : "");
        reportsDto.setRiyu(dto.getRiyu() != null ? dto.getRiyu() : "");
        reportsDto.setBiko(dto.getBiko() != null ? dto.getBiko() : "");

        // 申請の年月をyyyy年M月形式に変換
        if (dto.getShinseiYm() != null && !dto.getShinseiYm().isEmpty()) {
            String formattedShinseiYm = formatShinseiYm(dto.getShinseiYm());
            reportsDto.setShinseiYm(formattedShinseiYm);
        } else {
            reportsDto.setShinseiYm("");
        }

        // 発行日
        if (dto.getHakkoYmd() != null) {
            String strDate = dto.getHakkoYmd().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
            reportsDto.setHakkoYmd(strDate);
        } else {
            reportsDto.setHakkoYmd("");
        }

        // 申請受理日
        if (dto.getJuriYmd() != null) {
            String strDate = dto.getJuriYmd().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
            reportsDto.setJuriYmd(strDate);
        } else {
            reportsDto.setJuriYmd("");
        }

        List<KanpuMenjoTsuchiReportsDto> dataSourceList = Arrays.asList(reportsDto);
        JRDataSource params = new JRBeanCollectionDataSource(dataSourceList);

        return params;
    }

    /**
     * 申請年月を表示用フォーマットに変換
     * 入力形式: "2026-05" または "202605" など
     * 出力形式: "2026年5月"
     */
    private String formatShinseiYm(String shinseiYm) {
        try {
            // ハイフン区切りの場合 (例: "2026-05")
            if (shinseiYm.contains("-")) {
                String[] parts = shinseiYm.split("-");
                if (parts.length == 2) {
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    return year + "年" + month + "月";
                }
            }
            // 6桁数字の場合 (例: "202605")
            else if (shinseiYm.length() == 6 && shinseiYm.matches("\\d{6}")) {
                int year = Integer.parseInt(shinseiYm.substring(0, 4));
                int month = Integer.parseInt(shinseiYm.substring(4, 6));
                return year + "年" + month + "月";
            }
            // その他の形式は元の値をそのまま返す
            return shinseiYm;
        } catch (NumberFormatException e) {
            log.warn("申請年月の変換に失敗しました: {}", shinseiYm, e);
            return shinseiYm;
        }
    }
}