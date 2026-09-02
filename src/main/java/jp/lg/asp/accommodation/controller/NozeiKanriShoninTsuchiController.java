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
import jp.lg.asp.accommodation.dto.NozeiKanriShoninTsuchiDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.entity.Nokan;
import jp.lg.asp.accommodation.service.NokanService;
import jp.lg.asp.accommodation.service.NozeiKanriShoninTsuchiReportsService;
import jp.lg.asp.accommodation.service.NozeiKanriShoninTsuchiService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 納税管理人承認(不承認)通知書 Controller
 */
@Slf4j
@Controller
@RequestMapping("/reports/nozeiKanrininShoninTsuchi")
@RequiredArgsConstructor
public class NozeiKanriShoninTsuchiController {

	private final NozeiKanriShoninTsuchiService nozeiKanriShoninTsuchiService;
	private final NozeiKanriShoninTsuchiReportsService reportsService;
	private final NokanService nokanService;
	private final ScreenAccessChecker accessChecker;
	private static final String SCREEN_ID = ScreenManagement.NOZEI_KANRININ_SHONIN_TSUCHI;

	/**
	 * 画面表示
	 */
	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(HttpSession session, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
		String shiteiNo = SessionHelper.getShiteiNo(session);
		String gassanShiteiNo = SessionHelper.getGassanShiteiNo(session);

		// 指定番号または合算指定番号が存在しない場合
		if (selected == null || (shiteiNo == null && gassanShiteiNo == null)) {
			// 画面を戻して検索モーダルを表示
			model.addAttribute("showShiteiGassanModal", true);
			return "tokugimu/tTokugimuReport";
		}

		String effectiveShiteiNo = shiteiNo != null ? shiteiNo : gassanShiteiNo;

		// 納税管理人情報が未登録
		Nokan nokan = nokanService.findByJichitaiCdAndShiteiNo(effectiveShiteiNo).orElse(null);
		if (nokan == null) {
			model.addAttribute("errorMessage", "納税管理人情報が登録されていません。");
			return "tokugimu/tTokugimuReport";
		}

		// 選任免除（kbn = "3"）の場合は承認(不承認)通知書を発行できない
		if ("3".equals(nokan.getKbn())) {
			model.addAttribute("errorMessage", "納税管理人が選任免除のため、承認(不承認)通知書は発行できません。");
			return "tokugimu/tTokugimuReport";
		}

		NozeiKanriShoninTsuchiDto dto = new NozeiKanriShoninTsuchiDto();

		try {
			log.debug("納税管理人情報取得開始: shiteiNo={}", effectiveShiteiNo);
			NozeiKanriShoninTsuchiDto nozeiKanriInfo = nozeiKanriShoninTsuchiService.getNozeiKanriInfo(effectiveShiteiNo);
			if (nozeiKanriInfo != null) {
				dto = nozeiKanriInfo;
				log.debug("納税管理人情報取得成功");
			}
		} catch (RuntimeException e) {
			log.error("納税管理人情報取得エラー: {}", e.getMessage(), e);
			model.addAttribute("errorMessage", "指定番号: " + effectiveShiteiNo + " の情報が見つかりません。");
		}

		if (dto.getHakkoYmd() == null) {
			dto.setHakkoYmd(LocalDate.now());
		}

		model.addAttribute("dto", dto);
		return "reports/nozeiKanrininShoninTsuchi";
	}

	/**
	 * PDF出力
	 */
	@PostMapping("/pdf")
	@OpeLog(screenId = SCREEN_ID, operation = "PDF")
	@RptLog(rptId = ReportsConstants.NOZEI_KANRININ_SHONIN_TSUCHI, operation = ReportsConstants.SOUSA_PDF, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> generatePdf(NozeiKanriShoninTsuchiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);

			if (dto.getHakkoYmd() == null) {
				log.error("PDF生成エラー: 発行日が未入力です");
				return ResponseEntity.badRequest().build();
			}

			log.debug("PDF生成開始: shiteiNo={}, hakkoYmd={}", dto.getShiteiNo(), dto.getHakkoYmd());
			byte[] pdfData = reportsService.generateTsuchiPdf(dto);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("inline", "nozei_kanri_shonin_tsuchi.pdf");

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
	@RptLog(rptId = ReportsConstants.NOZEI_KANRININ_SHONIN_TSUCHI, operation = ReportsConstants.SOUSA_PREVIEW, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> preview(NozeiKanriShoninTsuchiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);

			if (dto.getHakkoYmd() == null) {
				log.error("プレビューエラー: 発行日が未入力です");
				return ResponseEntity.badRequest().build();
			}

			log.debug("プレビュー開始: shiteiNo={}, hakkoYmd={}", dto.getShiteiNo(), dto.getHakkoYmd());
			byte[] pdfData = reportsService.generateTsuchiPdf(dto);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.add("Content-Disposition", "inline; filename=nozei_kanri_shonin_tsuchi_preview.pdf");
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
	@RptLog(rptId = ReportsConstants.NOZEI_KANRININ_SHONIN_TSUCHI, operation = ReportsConstants.SOUSA_PRINT, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> print(NozeiKanriShoninTsuchiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);

			if (dto.getHakkoYmd() == null) {
				log.error("印刷エラー: 発行日が未入力です");
				return ResponseEntity.badRequest().build();
			}

			log.debug("印刷開始: shiteiNo={}, hakkoYmd={}", dto.getShiteiNo(), dto.getHakkoYmd());
			byte[] pdfData = reportsService.generateTsuchiPdf(dto);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.add("Content-Disposition", "inline; filename=nozei_kanri_shonin_tsuchi_print.pdf");
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
