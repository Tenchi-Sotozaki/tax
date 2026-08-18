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
import jp.lg.asp.accommodation.dto.NozeiKanrininNinteiDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.NozeiKanrininNinteiReportsService;
import jp.lg.asp.accommodation.service.NozeiKanrininNinteiService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 納税管理人選任免除認定（不認定）通知書 Controller
 */
@Slf4j
@Controller
@RequestMapping("/reports/nozeiKanrininNintei")
@RequiredArgsConstructor
public class NozeiKanrininNinteiController {

	private final NozeiKanrininNinteiService nozeiKanrininNinteiService;
	private final NozeiKanrininNinteiReportsService reportsService;
	private final ScreenAccessChecker accessChecker;
	private static final String SCREEN_ID = ScreenManagement.NOZEI_KANRININ_NINTEI;

	/**
	 * 画面表示
	 */
	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(HttpSession session, Model model) {
		try {
			accessChecker.checkAccess(SCREEN_ID);
			String shiteiNo = SessionHelper.getShiteiNo(session);
			
			// 指定番号が存在しない場合
			ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
			if (selected == null || selected.getShiteiNo() == null || selected.getShiteiNo().isEmpty()) {
				// 画面を戻して検索モーダルを表示
				model.addAttribute("showShiteiGassanModal", true);
				return "tokugimu/tTokugimuReport";
			}
			
			NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();

			try {
				log.debug("納税管理人選任免除認定情報取得開始: shiteiNo={}", shiteiNo);
				NozeiKanrininNinteiDto info = nozeiKanrininNinteiService.getNinteiInfo(shiteiNo);
				if (info != null) {
					dto = info;
				}
			} catch (RuntimeException e) {
				log.error("納税管理人選任免除認定情報取得エラー: {}", e.getMessage(), e);
				model.addAttribute("errorMessage", "指定番号: " + shiteiNo + " の情報が見つかりません。");
			}

			if (dto.getHakkoYmd() == null) {
				dto.setHakkoYmd(LocalDate.now());
			}
			if (dto.getNintei() == null) {
				dto.setNintei("認定");
			}

			model.addAttribute("dto", dto);
			return "reports/nozeiKanrininNintei";
		} catch (Exception e) {
			log.error("納税管理人選任免除認定通知書画面表示エラー", e);
			model.addAttribute("errorMessage", "システムエラーが発生しました。");
			NozeiKanrininNinteiDto dto = new NozeiKanrininNinteiDto();
			dto.setHakkoYmd(LocalDate.now());
			dto.setNintei("認定");
			model.addAttribute("dto", dto);
			return "reports/nozeiKanrininNintei";
		}
	}

	/**
	 * PDF出力
	 */
	@PostMapping("/pdf")
	@OpeLog(screenId = SCREEN_ID, operation = "PDF")
	@RptLog(rptId = ReportsConstants.NOZEI_KANRININ_NINTEI, operation = ReportsConstants.SOUSA_PDF, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> generatePdf(NozeiKanrininNinteiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);

			if (dto.getHakkoYmd() == null) {
				return ResponseEntity.badRequest().build();
			}

			log.debug("PDF生成開始: shiteiNo={}, hakkoYmd={}", dto.getShiteiNo(), dto.getHakkoYmd());
			byte[] pdfData = reportsService.generatePdf(dto);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("inline", "nozei_kanrinin_nintei.pdf");

			return ResponseEntity.ok().headers(headers).body(pdfData);
		} catch (Exception e) {
			log.error("PDF生成エラー", e);
			return ResponseEntity.status(500).build();
		}
	}

	/**
	 * プレビュー
	 */
	@PostMapping("/preview")
	@OpeLog(screenId = SCREEN_ID, operation = "プレビュー")
	@RptLog(rptId = ReportsConstants.NOZEI_KANRININ_NINTEI, operation = ReportsConstants.SOUSA_PREVIEW, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> preview(NozeiKanrininNinteiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);

			if (dto.getHakkoYmd() == null) {
				return ResponseEntity.badRequest().build();
			}

			byte[] pdfData = reportsService.generatePdf(dto);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.add("Content-Disposition", "inline; filename=nozei_kanrinin_nintei_preview.pdf");
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
			headers.add("Pragma", "no-cache");
			headers.add("Expires", "0");

			return ResponseEntity.ok().headers(headers).body(pdfData);
		} catch (Exception e) {
			log.error("プレビューエラー", e);
			return ResponseEntity.status(500).build();
		}
	}

	/**
	 * 印刷
	 */
	@PostMapping("/print")
	@OpeLog(screenId = SCREEN_ID, operation = "印刷")
	@RptLog(rptId = ReportsConstants.NOZEI_KANRININ_NINTEI, operation = ReportsConstants.SOUSA_PRINT, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> print(NozeiKanrininNinteiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);

			if (dto.getHakkoYmd() == null) {
				return ResponseEntity.badRequest().build();
			}

			byte[] pdfData = reportsService.generatePdf(dto);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.add("Content-Disposition", "inline; filename=nozei_kanrinin_nintei_print.pdf");
			headers.add("X-Print-Action", "true");
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
			headers.add("Pragma", "no-cache");
			headers.add("Expires", "0");

			return ResponseEntity.ok().headers(headers).body(pdfData);
		} catch (Exception e) {
			log.error("印刷エラー", e);
			return ResponseEntity.status(500).build();
		}
	}
}
