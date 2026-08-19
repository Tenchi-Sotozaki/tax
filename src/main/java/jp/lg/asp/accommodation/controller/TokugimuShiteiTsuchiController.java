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
import jp.lg.asp.accommodation.dto.TokugimuShiteiTsuchiDto;
import jp.lg.asp.accommodation.service.TokugimuShiteiTsuchiReportsService;
import jp.lg.asp.accommodation.service.TokugimuShiteiTsuchiService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 特別徴収義務者指定通知 Controller
 */
@Slf4j
@Controller
@RequestMapping("/reports/tokugimuShiteiTsuchi")
@RequiredArgsConstructor
public class TokugimuShiteiTsuchiController {

	private final TokugimuShiteiTsuchiService tokugimuShiteiTsuchiService;
	private final TokugimuShiteiTsuchiReportsService reportsService;
	private final ScreenAccessChecker accessChecker;
	private static final String SCREEN_ID = ScreenManagement.TOKUGIMU_SHITEI_TSUCHI;

	/**
	 * 画面表示
	 */
	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(HttpSession session, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		TokugimuShiteiTsuchiDto dto = new TokugimuShiteiTsuchiDto();
		
		// 指定番号が存在しない場合
		ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
		if (selected == null || selected.getShiteiNo() == null || selected.getShiteiNo().isEmpty()) {
			// 画面を戻して検索モーダルを表示
			model.addAttribute("showShiteiGassanModal", true);
			return "tokugimu/tTokugimuReport";
		}

		// 指定番号はセッションから取得
		String shiteiNo = SessionHelper.getShiteiNo(session);
		
		TokugimuShiteiTsuchiDto tokugimuInfo = tokugimuShiteiTsuchiService.getTokugimuInfo(shiteiNo);
		if (tokugimuInfo != null) {
			dto = tokugimuInfo;
		}
		
		// 発行日のデフォルト値を今日に設定
		if (dto.getHakkoYmd() == null) {
			dto.setHakkoYmd(LocalDate.now());
		}

		model.addAttribute("dto", dto);
		return "reports/tokugimuShiteiTsuchi";
	}

	/**
	 * PDF出力
	 */
	@PostMapping("/pdf")
	@OpeLog(screenId = SCREEN_ID, operation = "PDF")
	@RptLog(rptId = ReportsConstants.TOKUGIMU_SHITEI_TSUCHI, operation = ReportsConstants.SOUSA_PDF, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> generatePdf(TokugimuShiteiTsuchiDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDispositionFormData("inline", "tokugimu_shitei_tsuchi.pdf");

		return ResponseEntity.ok().headers(headers).body(pdfData);
	}

	/**
	 * プレビュー
	 */
	@PostMapping("/preview")
	@OpeLog(screenId = SCREEN_ID, operation = "プレビュー")
	@RptLog(rptId = ReportsConstants.TOKUGIMU_SHITEI_TSUCHI, operation = ReportsConstants.SOUSA_PREVIEW, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> preview(TokugimuShiteiTsuchiDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		// inline指定でブラウザ内表示を促す
		headers.add("Content-Disposition", "inline; filename=tokugimu_shitei_tsuchi_preview.pdf");
		// キャッシュ制御
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
	@RptLog(rptId = ReportsConstants.TOKUGIMU_SHITEI_TSUCHI, operation = ReportsConstants.SOUSA_PRINT, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> print(TokugimuShiteiTsuchiDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		// inline指定でブラウザ内表示
		headers.add("Content-Disposition", "inline; filename=tokugimu_shitei_tsuchi_print.pdf");
		// 印刷用のカスタムヘッダー
		headers.add("X-Print-Action", "true");
		// キャッシュ制御
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		return ResponseEntity.ok().headers(headers).body(pdfData);
	}
}