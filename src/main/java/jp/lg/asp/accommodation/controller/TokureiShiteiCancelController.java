package jp.lg.asp.accommodation.controller;

import java.time.LocalDate;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.annotation.RptLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TokureiShiteiCancelDto;
import jp.lg.asp.accommodation.dto.TokureiShiteiDto;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.service.TokureiShiteiCancelReportsService;
import jp.lg.asp.accommodation.service.TokureiShiteiService;
import jp.lg.asp.accommodation.util.SessionHelper;
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
	private final ReportsCommonService reportsCommonService;
	private final ScreenAccessChecker accessChecker;
	private static final String SCREEN_ID = ScreenManagement.TOKUREI_SHITEI_CANCEL;

	/**
	 * 画面表示
	 */
	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(HttpSession session, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		String shiteiNo = SessionHelper.getShiteiNo(session);
		
		// 指定番号が存在しない場合
		ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
		if (selected == null || selected.getShiteiNo() == null || selected.getShiteiNo().isEmpty()) {
			// 画面を戻して検索モーダルを表示
			model.addAttribute("showShiteiGassanModal", true);
			return "tokugimu/tTokugimuReport";
		}
		
		TokureiShiteiCancelDto dto = new TokureiShiteiCancelDto();

		TokureiShiteiDto shiteiDto = tokureiShiteiService.getTokugimuInfo(shiteiNo);
		if (shiteiDto != null) {
			dto = convertToCancel(shiteiDto);
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
	@RptLog(rptId = ReportsConstants.TOKUREI_SHITEI_CANCEL, operation = ReportsConstants.SOUSA_PDF, shiteiNo = "#dto.shiteiNo")
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
	@RptLog(rptId = ReportsConstants.TOKUREI_SHITEI_CANCEL, operation = ReportsConstants.SOUSA_PREVIEW, shiteiNo = "#dto.shiteiNo")
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
	@RptLog(rptId = ReportsConstants.TOKUREI_SHITEI_CANCEL, operation = ReportsConstants.SOUSA_PRINT, shiteiNo = "#dto.shiteiNo")
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

	private TokureiShiteiCancelDto convertToCancel(TokureiShiteiDto src) {
		TokureiShiteiCancelDto dest = new TokureiShiteiCancelDto();
		dest.setShiteiNo(src.getShiteiNo());
		dest.setTokuName(src.getTokuName());
		dest.setTokuJusho(src.getTokuJusho());
		dest.setShisetsuName(src.getShisetsuName());
		dest.setShisetsuJusho(src.getShisetsuJusho());
		dest.setCity(src.getCity());
		dest.setBiko(src.getBiko());
		dest.setKoin(src.getKoin());
		
		// 条例は指定通知書とは別の条項になるため、取消通知書専用の定義を読む
		dest.setJorei(reportsCommonService.getReportsDefText(ReportsConstants.TOKUREI_CANCEL_JOREI));
	
		return dest;
	}
}
