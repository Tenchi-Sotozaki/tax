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
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
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
    private final JichitaiRepository jichitaiRepository;

    @Override
    public byte[] generateTsuchiPdf(KanpuMenjoTsuchiDto dto) {
        try {
            InputStream jrxmlStream = new ClassPathResource(JRXML_PATH).getInputStream();
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            Map<String, Object> parameters = new HashMap<>();
            // 帳票で丸印の表示に使用するため、データソースとは別に、パラメータマップへ格納
            if (dto.getShinsei_kbn() == null) {
                throw new IllegalArgumentException("申請の区分は必須です。");
            }
            if (dto.getKettei_naiyou() == null) {
                throw new IllegalArgumentException("決定の内容は必須です。");
            }
            parameters.put("shinsei_kbn", dto.getShinsei_kbn());
            parameters.put("kettei_naiyou", dto.getKettei_naiyou());
            
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
        if (jorei == null) {
            throw new IllegalArgumentException("帳票出力項目が未設定です。管理者にお問い合わせください。");
        }
        if (dto.getKoin() == null || dto.getKoin().length == 0) {
            throw new IllegalArgumentException("公印が未設定です。管理者にお問い合わせください。");
        }
        if (dto.getZeigaku() == null || dto.getZeigaku().isEmpty()) {
            throw new IllegalArgumentException("申請した税額は必須です。");
        }
        reportsDto.setCityName(dto.getCityName() != null ? dto.getCityName() : "");
        reportsDto.setJorei(jorei);
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
        reportsDto.setKoin(dto.getKoin());

        // 申請の年月
        if (dto.getShinseiYm() == null || dto.getShinseiYm().isEmpty()) {
            throw new IllegalArgumentException("対象年月は必須です。");
        }
        YearMonth yearMonth = java.time.YearMonth.parse(dto.getShinseiYm());
        LocalDate shinseiYm = yearMonth.atDay(1);
        JapaneseDate shinseiJapaneseDate = JapaneseDate.from(shinseiYm);
        reportsDto.setShinseiYm(shinseiJapaneseDate.format(DateTimeFormatter.ofPattern("Gy年M月", Locale.JAPANESE)));

        // 発行日
        if (dto.getHakkoYmd() == null) {
            throw new IllegalArgumentException("発行年月日は必須です。");
        }
        JapaneseDate hakkoJapaneseDate = JapaneseDate.from(dto.getHakkoYmd());
        reportsDto.setHakkoYmd(hakkoJapaneseDate.format(DateTimeFormatter.ofPattern("Gy年M月d日", Locale.JAPANESE)));

        // 申請受理日
        if (dto.getJuriYmd() == null) {
            throw new IllegalArgumentException("申請受理年月日は必須です。");
        }
        JapaneseDate juriJapaneseDate = JapaneseDate.from(dto.getJuriYmd());
        reportsDto.setJuriYmd(juriJapaneseDate.format(DateTimeFormatter.ofPattern("Gy年M月d日", Locale.JAPANESE)));

        List<KanpuMenjoTsuchiReportsDto> dataSourceList = Arrays.asList(reportsDto);
        JRDataSource params = new JRBeanCollectionDataSource(dataSourceList);

        return params;
    }

    @Override
    public Jichitai findJichitai(String jichitaiCd) {
        return jichitaiRepository.findById(jichitaiCd)
            .orElseThrow(() -> new IllegalArgumentException("自治体情報が見つかりませんでした。"));
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
            throw new IllegalArgumentException("申請した税額が不正です。");
        }
    }
}