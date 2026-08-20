package jp.lg.asp.accommodation.service.impl;

import java.io.InputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.chrono.JapaneseDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiDto;
import jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiReportsDto;
import jp.lg.asp.accommodation.service.KanpuMenjoTsuchiReportsService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
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
    private final ReportsCommonService reportsCommonService;

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
    	
    	// 条例を取得
    	String jorei = reportsCommonService.getReportsDefText(ReportsConstants.KANPU_MENJO_SHINSEI_JOREI);
    	
        KanpuMenjoTsuchiReportsDto reportsDto = new KanpuMenjoTsuchiReportsDto();

        // 基本情報
        reportsDto.setCityName(dto.getCityName() != null ? dto.getCityName() : "");
        reportsDto.setJorei(jorei != null ? jorei : "");
        reportsDto.setTokuName(dto.getTokuName() != null ? dto.getTokuName() : "");
        reportsDto.setTokuYubin(dto.getTokuYubin() != null ? dto.getTokuYubin() : "");
        reportsDto.setTokuJusho(dto.getTokuJusho() != null ? dto.getTokuJusho() : "");
        reportsDto.setShisetsuYubin(dto.getShisetsuYubin() != null ? dto.getShisetsuYubin() : "");
        reportsDto.setShisetsuJusho(dto.getShisetsuJusho() != null ? dto.getShisetsuJusho() : "");
        reportsDto.setShisetsuName(dto.getShisetsuName() != null ? dto.getShisetsuName() : "");
        reportsDto.setZeigaku(formatMoney(dto.getZeigaku()));
        reportsDto.setKanpuMenjoGaku(formatMoney(dto.getKanpuMenjoGaku()));
        reportsDto.setRiyu(dto.getRiyu() != null ? dto.getRiyu() : "");
        reportsDto.setBiko(dto.getBiko() != null ? dto.getBiko() : "");
        reportsDto.setKoin(dto.getKoin() != null && dto.getKoin().length > 0 ? dto.getKoin() : null);

        // 申請の年月
        if (dto.getShinseiYm() != null && !dto.getShinseiYm().isEmpty()) {
        	// YearMonth型に変換
        	YearMonth yearMonth = java.time.YearMonth.parse(dto.getShinseiYm());
        	
        	// 仮で日付を代入
            LocalDate shinseiYm = yearMonth.atDay(1);
        	
        	// 和暦形式に変換
        	JapaneseDate japaneseDate = JapaneseDate.from(shinseiYm);
        	
        	// フォーマット定義
        	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年M月", Locale.JAPANESE);
        	String strDate = japaneseDate.format(formatter);
        	
        	// 和暦形式の申請年月を設定
            reportsDto.setShinseiYm(strDate);
        } else {
            reportsDto.setShinseiYm("");
        }

        // 発行日
        if (dto.getHakkoYmd() != null) {
        	// 和暦形式に変換
        	JapaneseDate japaneseDate = JapaneseDate.from(dto.getHakkoYmd());
        	
        	// フォーマット定義
        	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年M月d日", Locale.JAPANESE);
        	String strDate = japaneseDate.format(formatter);
        	
            reportsDto.setHakkoYmd(strDate);
        } else {
            reportsDto.setHakkoYmd("");
        }

        // 申請受理日
        if (dto.getJuriYmd() != null) {
        	// 和暦形式に変換
        	JapaneseDate japaneseDate = JapaneseDate.from(dto.getJuriYmd());
        	
        	// フォーマット定義
        	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("Gy年M月d日", Locale.JAPANESE);
        	String strDate = japaneseDate.format(formatter);
        	
        	// 和暦形式の申請受理日を設定
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
    
    /**
     * 金額をカンマ区切り形式に変換
     * @param money：金額
     * @return 変換結果
     */
    private String formatMoney(String money) {
    	
    	// 金額なし
        if (money == null || money.isEmpty()) {
            return "";
        }
        
        try {
        	// カンマ区切り形式のインスタンスを取得
        	NumberFormat nf = NumberFormat.getNumberInstance(Locale.JAPAN);
        	
            // 文字列を数値lpng型に変換してカンマ付き文字列に変換
            long amount = Long.parseLong(money);
            return nf.format(amount);
        } catch (NumberFormatException e) {
            // 数値変換できない場合はそのまま返す
            return money;
        }
    }
}