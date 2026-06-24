package jp.lg.asp.accommodation.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.lg.asp.accommodation.dto.KoseiKetteiTsuchiReportsDto;
import jp.lg.asp.accommodation.service.KoseiKetteiTsuchiReportsService;
import jp.lg.asp.accommodation.service.impl.KoseiKetteiTsuchiReportsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 更正・決定通知書 Controller
 */
@Slf4j
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class KoseiKetteiTsuchiController {

    private final KoseiKetteiTsuchiReportsService reportsService;
    private final KoseiKetteiTsuchiReportsServiceImpl reportsServiceImpl;

    /**
     * 画面表示
     */
    @GetMapping("/koseiKetteiTsuchi")
    public String index(
            @RequestParam(required = false) String shiteiNo,
            @RequestParam(required = false) String nendo,
            Model model) {

        KoseiKetteiTsuchiReportsDto dto =
            reportsServiceImpl.buildDtoForDisplay(shiteiNo != null ? shiteiNo : "");

        model.addAttribute("dto", dto);
        model.addAttribute("taishoYmList",
            shiteiNo != null && !shiteiNo.isEmpty()
                ? reportsService.findTaishoYmList(shiteiNo)
                : java.util.Collections.emptyList());
        return "reports/koseiKetteiTsuchi";
    }

    /**
     * PDF出力
     */
    @PostMapping("/koseiKetteiTsuchi/pdf")
    public ResponseEntity<byte[]> generatePdf(
            @RequestParam String shiteiNo,
            @RequestParam String b1Ym,
            @RequestParam(required = false) String b2Ym,
            @RequestParam(required = false) String b3Ym) {
        try {
            byte[] pdfData = reportsService.generatePdf(shiteiNo, b1Ym, b2Ym, b3Ym);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "kosei_kettei_tsuchi.pdf");

            return ResponseEntity.ok().headers(headers).body(pdfData);
        } catch (Exception e) {
            log.error("PDF生成中にエラーが発生しました: shiteiNo={}, b1Ym={}", shiteiNo, b1Ym, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * プレビュー
     */
    @PostMapping("/koseiKetteiTsuchi/preview")
    public ResponseEntity<byte[]> preview(
            @RequestParam String shiteiNo,
            @RequestParam String b1Ym,
            @RequestParam(required = false) String b2Ym,
            @RequestParam(required = false) String b3Ym) {
        try {
            byte[] pdfData = reportsService.generatePdf(shiteiNo, b1Ym, b2Ym, b3Ym);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline; filename=kosei_kettei_tsuchi_preview.pdf");
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");

            return ResponseEntity.ok().headers(headers).body(pdfData);
        } catch (Exception e) {
            log.error("プレビュー生成中にエラーが発生しました: shiteiNo={}, b1Ym={}", shiteiNo, b1Ym, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
