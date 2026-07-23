package jp.lg.asp.accommodation.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.annotation.RptLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.NonyushoDto;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.service.NonyushoReportsService;
import jp.lg.asp.accommodation.service.TokugimuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 納入書コントローラー
 */
@Slf4j
@Controller
@RequestMapping("/nonyusho")
@RequiredArgsConstructor
public class NonyushoController {

    private final NonyushoReportsService nonyushoReportsService;
    private final TokugimuService tokugimuService;
    private final ScreenAccessChecker accessChecker;

    /**
     * 納入書発行画面表示
     */
    @GetMapping
    public String index(Model model, @RequestParam(required = false) String shiteiNo) {
        log.debug("納入書発行画面表示: shiteiNo={}", shiteiNo);
        
        if (shiteiNo != null && !shiteiNo.trim().isEmpty()) {
            try {
                // 指定番号に紐づく特別徴収義務者情報を取得
                TokugimuForm tokugimuForm = tokugimuService.getTokugimuByShiteiNo(shiteiNo);
                model.addAttribute("shiteiNo", shiteiNo);
                model.addAttribute("shisetsuName", tokugimuForm.getFacilityName());
                model.addAttribute("tokuName", tokugimuForm.getName());
                model.addAttribute("tokuJusho", tokugimuForm.getTokugimuAddress());
                model.addAttribute("tokuYubinNo", tokugimuForm.getTokugimuYubinNo());
                model.addAttribute("shisetsuJusho", tokugimuForm.getFacilityAddress());
            } catch (Exception e) {
                log.error("特別徴収義務者情報の取得に失敗: shiteiNo={}", shiteiNo, e);
                model.addAttribute("shiteiNo", shiteiNo);
            }
        }
        
        return "reports/nonyusho";
    }

    /**
     * 納入書PDFダウンロード
     */
    @PostMapping("/pdf")
    @OpeLog(screenId = ScreenManagement.NONYUSHO, operation = "PDF")
    @RptLog(rptId = ReportsConstants.NONYUSHO, operation = ReportsConstants.SOUSA_PDF, shiteiNo = "#dto.shiteiNo")
    public ResponseEntity<byte[]> generatePdf(@ModelAttribute NonyushoDto dto) {
        try {
            byte[] pdf = nonyushoReportsService.generateNonyushoPdf(dto);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("nonyusho.pdf", StandardCharsets.UTF_8)
                    .build());
            
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("納入書PDFダウンロードエラー: shiteiNo={}", dto.getShiteiNo(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 納入書PDFプレビュー
     */
    @PostMapping("/preview")
    @OpeLog(screenId = ScreenManagement.NONYUSHO, operation = "プレビュー")
    @RptLog(rptId = ReportsConstants.NONYUSHO, operation = ReportsConstants.SOUSA_PDF, shiteiNo = "#dto.shiteiNo")
    public ResponseEntity<byte[]> previewPdf(@ModelAttribute NonyushoDto dto) {
        try {
        	byte[] pdf = nonyushoReportsService.generateNonyushoPdf(dto);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename("nonyusho_preview.pdf", StandardCharsets.UTF_8)
                    .build());
            
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("納入書PDFプレビューエラー: shiteiNo={}", dto.getShiteiNo(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 納入書印刷
     */
    @PostMapping("/print")
	@OpeLog(screenId = ScreenManagement.NONYUSHO, operation = "印刷")
	@RptLog(rptId = ReportsConstants.NONYUSHO, operation = ReportsConstants.SOUSA_PRINT, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> print(@ModelAttribute NonyushoDto dto) {
		try {
			accessChecker.checkAccess(ScreenManagement.NONYUSHO);
			byte[] pdf = nonyushoReportsService.generateNonyushoPdf(dto);
			
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.add("Content-Disposition", "inline; filename=nonyusho_print.pdf");
			headers.add("X-Print-Action", "true");
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
			headers.add("Pragma", "no-cache");
			headers.add("Expires", "0");
			
			return ResponseEntity.ok().headers(headers).body(pdf);
			
		} catch (Exception e) {
            log.error("納入書印刷エラー: shiteiNo={}", dto.getShiteiNo(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
	}
}