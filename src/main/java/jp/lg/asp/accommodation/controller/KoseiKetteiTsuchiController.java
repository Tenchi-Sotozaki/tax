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

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.service.KoseiKetteiTsuchiReportsService;
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
    private final ScreenAccessChecker accessChecker;
    private static final String SCREEN_ID = ScreenManagement.KOSEI_KETTEI_TSUCHI;

    /**
     * 画面表示
     */
    @GetMapping("/koseiKetteiTsuchi")
    public String index(
            @RequestParam(required = false) String shiteiNo,
            Model model) {
        accessChecker.checkAccess(SCREEN_ID);

        String safeShiteiNo = shiteiNo != null ? shiteiNo : "";

        model.addAttribute("dto", reportsService.buildDtoForDisplay(safeShiteiNo));
        model.addAttribute("taishoYmList",
                !safeShiteiNo.isEmpty()
                        ? reportsService.findTaishoYmList(safeShiteiNo)
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
            accessChecker.checkAccess(SCREEN_ID);

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
            accessChecker.checkAccess(SCREEN_ID);

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

    /**
     * 印刷
     */
    @PostMapping("/koseiKetteiTsuchi/print")
    public ResponseEntity<byte[]> print(
            @RequestParam String shiteiNo,
            @RequestParam String b1Ym,
            @RequestParam(required = false) String b2Ym,
            @RequestParam(required = false) String b3Ym) {
        try {
            accessChecker.checkAccess(SCREEN_ID);

            byte[] pdfData = reportsService.generatePdf(shiteiNo, b1Ym, b2Ym, b3Ym);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline; filename=kosei_kettei_tsuchi_print.pdf");
            headers.add("X-Print-Action", "true");
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");

            return ResponseEntity.ok().headers(headers).body(pdfData);
        } catch (Exception e) {
            log.error("印刷用PDF生成中にエラーが発生しました: shiteiNo={}, b1Ym={}", shiteiNo, b1Ym, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
