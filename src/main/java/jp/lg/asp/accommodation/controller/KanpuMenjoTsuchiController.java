package jp.lg.asp.accommodation.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiDto;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.service.AtenaImportService;
import jp.lg.asp.accommodation.service.KanpuMenjoTsuchiReportsService;
import jp.lg.asp.accommodation.service.TokugimuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 徴収不能額の還付又は納入義務の免除決定通知書コントローラー
 */
@Slf4j
@Controller
@RequestMapping("/kanpuMenjoTsuchi")
@RequiredArgsConstructor
public class KanpuMenjoTsuchiController {

    private final TokugimuService tokugimuService;
    private final AtenaImportService atenaImportService;
    private final KanpuMenjoTsuchiReportsService kanpuMenjoTsuchiReportsService;

    @Value("${app.city-name}")
    private String cityName;

    @Value("${app.jorei}")
    private String jorei;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    private String getJichitaiCdFromUser() {
        return jichitaiCd;
    }

    /**
     * 徴収不能額の還付又は納入義務の免除決定通知書画面表示
     */
    @GetMapping
    public String index(@RequestParam String shiteiNo,
                       @AuthenticationPrincipal User userDetails,
                       Model model) {
        try {
            log.info("徴収不能額の還付又は納入義務の免除決定通知書画面表示開始: shiteiNo={}", shiteiNo);

            // 特別徴収義務者情報取得
            TokugimuForm tokugimuForm = tokugimuService.getTokugimuByShiteiNo(shiteiNo);

            if (tokugimuForm == null) {
                model.addAttribute("errorMessage", "指定された特別徴収義務者が見つかりません。");
                return "error";
            }

            // 宛名情報取得
            Atena atena = null; // TODO: 既存のAtena取得ロジックに変更

            // DTO作成
            KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
            dto.setShiteiNo(shiteiNo);
            dto.setCityName(cityName);
            dto.setJorei(jorei);
            dto.setHakkoYmd(LocalDate.now());

            if (tokugimuForm != null) {
                String tokuJusho = "";
                // TokugimuFormから住所情報を取得
                if (tokugimuForm.getTokugimuAddress() != null && !tokugimuForm.getTokugimuAddress().isEmpty()) {
                    tokuJusho = tokugimuForm.getTokugimuAddress();
                }
                dto.setTokuJusho(tokuJusho);
                dto.setTokuName(tokugimuForm.getName());
            }

            String shisetsuJusho = "";
            if (tokugimuForm.getFacilityAddressNo() != null && !tokugimuForm.getFacilityAddressNo().isEmpty()) {
                shisetsuJusho += "〒" + tokugimuForm.getFacilityAddressNo() + " ";
            }
            if (tokugimuForm.getFacilityAddress() != null && !tokugimuForm.getFacilityAddress().isEmpty()) {
                shisetsuJusho += tokugimuForm.getFacilityAddress();
            }
            dto.setShisetsuJusho(shisetsuJusho);
            dto.setShisetsuName(tokugimuForm.getFacilityName());

            model.addAttribute("dto", dto);

            log.info("徴収不能額の還付又は納入義務の免除決定通知書画面表示成功");
            return "reports/kanpuMenjoTsuchi";

        } catch (Exception e) {
            log.error("徴収不能額の還付又は納入義務の免除決定通知書画面表示エラー", e);
            model.addAttribute("errorMessage", "画面表示中にエラーが発生しました。");
            return "error";
        }
    }

    /**
     * PDF生成
     */
    @PostMapping("/generatePdf")
    public ResponseEntity<byte[]> generatePdf(@ModelAttribute KanpuMenjoTsuchiDto dto,
                                             @AuthenticationPrincipal User userDetails) {
        try {
            log.info("PDF生成開始: shiteiNo={}, hakkoYmd={}", dto.getShiteiNo(), dto.getHakkoYmd());

            byte[] pdfData = kanpuMenjoTsuchiReportsService.generateTsuchiPdf(dto);

            String filename = "kanpu_menjo_tsuchi_" + dto.getShiteiNo() + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);

        } catch (Exception e) {
            log.error("PDF生成エラー", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * プレビュー
     */
    @PostMapping("/preview")
    public ResponseEntity<byte[]> preview(@ModelAttribute KanpuMenjoTsuchiDto dto,
                                         @AuthenticationPrincipal User userDetails) {
        try {
            log.info("プレビュー開始: shiteiNo={}, hakkoYmd={}", dto.getShiteiNo(), dto.getHakkoYmd());

            byte[] pdfData = kanpuMenjoTsuchiReportsService.generateTsuchiPdf(dto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline");
            headers.setContentLength(pdfData.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);

        } catch (Exception e) {
            log.error("プレビューエラー", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 印刷
     */
    @PostMapping("/print")
    public ResponseEntity<byte[]> print(@ModelAttribute KanpuMenjoTsuchiDto dto,
                                       @AuthenticationPrincipal User userDetails) {
        try {
            log.info("印刷開始: shiteiNo={}, hakkoYmd={}", dto.getShiteiNo(), dto.getHakkoYmd());

            byte[] pdfData = kanpuMenjoTsuchiReportsService.generateTsuchiPdf(dto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline");
            headers.setContentLength(pdfData.length);

            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);

        } catch (Exception e) {
            log.error("印刷エラー", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}