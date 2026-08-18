package jp.lg.asp.accommodation.controller;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.annotation.RptLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.NonyushoDataResponse;
import jp.lg.asp.accommodation.dto.NonyushoDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.service.NonyushoReportsService;
import jp.lg.asp.accommodation.service.TokugimuService;
import jp.lg.asp.accommodation.util.SessionHelper;
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
    public String index(Model model, HttpSession session) {
        String shiteiNo = SessionHelper.getShiteiNo(session);
        
		// 指定番号が存在しない場合
		ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
		if (selected == null || selected.getShiteiNo() == null || selected.getShiteiNo().isEmpty()) {
			// 画面を戻して検索モーダルを表示
			model.addAttribute("showShiteiGassanModal", true);
			return "tokugimu/tTokugimuReport";
		}
        
        log.debug("納入書発行画面表示: shiteiNo={}", shiteiNo);
        
        try {
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
        return "reports/nonyusho";
    }
    
	/**
	 * 納入書動的データ取得API
	 */
	@GetMapping("/data")
	@ResponseBody
	public ResponseEntity<NonyushoDataResponse> getNonyushoData(
			@RequestParam String shiteiNo,
			@RequestParam String nendo,
			@RequestParam(required = false) String shinkokuYm) {
		try {
			log.debug("納入書動的データ取得API呼び出し: shiteiNo={}, nendo={}, shinkokuYm={}", shiteiNo, nendo, shinkokuYm);

			NonyushoDataResponse response = nonyushoReportsService.getNonyushoData(shiteiNo, nendo, shinkokuYm);

			log.debug("納入書動的データ取得完了: shiteiNo={}, nendo={}, shinkokuYm={}", shiteiNo, nendo, shinkokuYm);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			log.error("納入書動的データ取得エラー: shiteiNo={}, nendo={}, shinkokuYm={}", shiteiNo, nendo, shinkokuYm, e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
     * 納入書PDFダウンロード
     */
    @PostMapping("/pdf")
    @OpeLog(screenId = ScreenManagement.NONYUSHO, operation = "PDF")
    @RptLog(rptId = ReportsConstants.NONYUSHO, operation = ReportsConstants.SOUSA_PDF, shiteiNo = "#dto.shiteiNo")
    public Object generatePdf(@RequestBody NonyushoDto dto) {
        try {
        	// データ無し
            if (nonyushoReportsService.dataCheck(dto)) {
            	return ResponseEntity.badRequest().body("指定された条件のデータが見つかりません。".getBytes(StandardCharsets.UTF_8));
            }
            
            byte[] pdf = nonyushoReportsService.generateNonyushoPdf(dto);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("nonyusho.pdf", StandardCharsets.UTF_8)
                    .build());
            
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
            
        } catch (RuntimeException e) {
			// サービス層でスローされた賦課情報未発見エラー
			log.warn("納入書PDFダウンロードエラー: shiteiNo={}, message={}", dto.getShiteiNo(), e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage().getBytes(StandardCharsets.UTF_8));
		}catch (Exception e) {
            log.error("納入書PDFダウンロードエラー: shiteiNo={}", dto.getShiteiNo(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 納入書PDFプレビュー
     */
    @PostMapping("/preview")
    @OpeLog(screenId = ScreenManagement.NONYUSHO, operation = "プレビュー")
    @RptLog(rptId = ReportsConstants.NONYUSHO, operation = ReportsConstants.SOUSA_PREVIEW, shiteiNo = "#dto.shiteiNo")
    public Object previewPdf(@RequestBody NonyushoDto dto) {
        try {
        	// データ無し
            if (nonyushoReportsService.dataCheck(dto)) {
            	return ResponseEntity.badRequest().body("指定された条件のデータが見つかりません。".getBytes(StandardCharsets.UTF_8));
            }
            
            byte[] pdf = nonyushoReportsService.generateNonyushoPdf(dto);
        
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename("nonyusho_preview.pdf", StandardCharsets.UTF_8)
                    .build());
            
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
            
        } catch (RuntimeException e) {
			// サービス層でスローされた賦課情報未発見エラー
			log.warn("納入書PDFダウンロードエラー: shiteiNo={}, message={}", dto.getShiteiNo(), e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage().getBytes(StandardCharsets.UTF_8));
		}catch (Exception e) {
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
    public Object printPDF(@RequestBody NonyushoDto dto) {
    	try {
			// データ無し
			if (nonyushoReportsService.dataCheck(dto)) {
				return ResponseEntity.badRequest().body("指定された条件のデータが見つかりません。".getBytes(StandardCharsets.UTF_8));
			}

            log.debug("納入書PDF生成開始: shiteiNo={}", dto.getShiteiNo());
        
            byte[] pdf = nonyushoReportsService.generateNonyushoPdf(dto);
			
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "nonyusho.pdf");
			
            log.debug("納入書PDF生成完了: shiteiNo={}", dto.getShiteiNo());
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
			
		} catch (RuntimeException e) {
			// サービス層でスローされた賦課情報未発見エラー
			log.warn("納入書PDFダウンロードエラー: shiteiNo={}, message={}", dto.getShiteiNo(), e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage().getBytes(StandardCharsets.UTF_8));
		}catch (Exception e) {
            log.error("納入書PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
	}
	
	/**
     * エラー時にアラートを表示して新しいタブを閉じるHTMLレスポンスを生成
     */
    private ResponseEntity<String> buildErrorScriptResponse(String message, HttpStatus status) {
		String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body><script>"
				+ "alert('" + message + "');"
				+ "window.close();"
				+ "</script></body></html>";
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8));
		
		// 引数で受け取ったステータスコードを設定して返却
		return new ResponseEntity<>(html, headers, status);
	}
}