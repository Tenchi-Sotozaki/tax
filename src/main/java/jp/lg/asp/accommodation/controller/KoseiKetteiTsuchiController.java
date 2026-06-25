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

    /** PDFファイル名 */
    private static final String PDF_FILENAME        = "kosei_kettei_tsuchi.pdf";
    /** プレビューファイル名 */
    private static final String PREVIEW_FILENAME    = "kosei_kettei_tsuchi_preview.pdf";

    /**
     * 画面表示
     */
    @GetMapping("/koseiKetteiTsuchi")
    public String index(
            @RequestParam(required = false) String shiteiNo,
            Model model) {

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

        return buildPdfResponse(shiteiNo, b1Ym, b2Ym, b3Ym, PDF_FILENAME, false);
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

        return buildPdfResponse(shiteiNo, b1Ym, b2Ym, b3Ym, PREVIEW_FILENAME, true);
    }

    /**
     * PDF ResponseEntityを構築する共通メソッド
     * @param shiteiNo  指定番号
     * @param b1Ym      対象月b1（YYYYMM）
     * @param b2Ym      対象月b2（YYYYMM、任意）
     * @param b3Ym      対象月b3（YYYYMM、任意）
     * @param filename  ダウンロードファイル名
     * @param noCache   キャッシュ無効化フラグ（プレビュー時はtrue）
     * @return PDF ResponseEntity
     */
    private ResponseEntity<byte[]> buildPdfResponse(
            String shiteiNo, String b1Ym, String b2Ym, String b3Ym,
            String filename, boolean noCache) {
        try {
            byte[] pdfData = reportsService.generatePdf(shiteiNo, b1Ym, b2Ym, b3Ym);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline; filename=" + filename);
            if (noCache) {
                headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            }

            return ResponseEntity.ok().headers(headers).body(pdfData);

        } catch (Exception e) {
            log.error("PDF生成中にエラーが発生しました: shiteiNo={}, b1Ym={}", shiteiNo, b1Ym, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
