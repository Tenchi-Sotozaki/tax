package jp.lg.asp.accommodation.controller;

import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.TokureiShiteiCancelDto;
import jp.lg.asp.accommodation.dto.TokureiShiteiDto;
import jp.lg.asp.accommodation.service.TokureiShiteiCancelReportsService;
import jp.lg.asp.accommodation.service.TokureiShiteiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 納入申告書の提出期限等の特例適用者指定取消通知 Controller
 */
@Slf4j
@Controller
@RequestMapping("/reports/tokureiShiteiCancel")
@RequiredArgsConstructor
public class TokureiShiteiCancelController {

	private final TokureiShiteiService tokureiShiteiService;
	private final TokureiShiteiCancelReportsService reportsService;
	private final ScreenAccessChecker accessChecker;
	private static final String SCREEN_ID = ScreenManagement.TOKUREI_SHITEI_CANCEL;

	/**
	 * 画面表示
	 */
	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(@RequestParam(required = false) String shiteiNo, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();

		if (shiteiNo != null && !shiteiNo.isEmpty()) {
			TokureiShiteiDto shiteiDto = tokureiShiteiService.getTokugimuInfo(shiteiNo);
			if (shiteiDto != null) {
				dto = convertToCancel(shiteiDto);
			}
		}

		if (dto.getHakkoYmd() == null) {
			dto.setHakkoYmd(LocalDate.now());
		}

		model.addAttribute("dto", dto);
		return "reports/tokureiShiteiCancel";
	}

	/**
	 * PDF出力
	 */
	@PostMapping("/pdf")
	@OpeLog(screenId = SCREEN_ID, operation = "PDF")
	public ResponseEntity<byte[]> generatePdf(TokureiShiteiCancelDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDispositionFormData("inline", "tokurei_shitei_cancel.pdf");

		return ResponseEntity.ok().headers(headers).body(pdfData);
	}

	/**
	 * プレビュー
	 */
	@PostMapping("/preview")
	@OpeLog(screenId = SCREEN_ID, operation = "プレビュー")
	public ResponseEntity<byte[]> preview(TokureiShiteiCancelDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.add("Content-Disposition", "inline; filename=tokurei_shitei_cancel_preview.pdf");
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		return ResponseEntity.ok().headers(headers).body(pdfData);
	}

	/**
	 * 印刷
	 */
	@PostMapping("/print")
	@OpeLog(screenId = SCREEN_ID, operation = "印刷")
	public ResponseEntity<byte[]> print(TokureiShiteiCancelDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.add("Content-Disposition", "inline; filename=tokurei_shitei_cancel_print.pdf");
		headers.add("X-Print-Action", "true");
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		return ResponseEntity.ok().headers(headers).body(pdfData);
	}

	/**
	 * TokureiShiteiDto から TokureiShiteiCancelDto への変換
	 */
	private TokureiShiteiCancelDto convertToCancel(TokureiShiteiDto src) {
		TokureiShiteiCancelDto dest = new TokureiShiteiCancelDto();
		dest.setShiteiNo(src.getShiteiNo());
		dest.setTokuName(src.getTokuName());
		dest.setTokuJusho(src.getTokuJusho());
		dest.setShisetsuName(src.getShisetsuName());
		dest.setShisetsuJusho(src.getShisetsuJusho());
		dest.setCity(src.getCity());
		dest.setJorei(src.getJorei());
		return dest;
	}
}
