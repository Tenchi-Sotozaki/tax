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
import jp.lg.asp.accommodation.dto.TokugimuJuriTsuchiDto;
import jp.lg.asp.accommodation.service.TokugimuJuriTsuchiReportsService;
import jp.lg.asp.accommodation.service.TokugimuJuriTsuchiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 特別徴収義務者申請受理通知 Controller
 */
@Slf4j
@Controller
@RequestMapping("/reports/tokugimuJuriTsuchi")
@RequiredArgsConstructor
public class TokugimuJuriTsuchiController {

	private final TokugimuJuriTsuchiService tokugimuJuriTsuchiService;
	private final TokugimuJuriTsuchiReportsService reportsService;
	private final ScreenAccessChecker accessChecker;
	private static final String SCREEN_ID = ScreenManagement.TOKUGIMU_JURI_TSUCHI;

	/**
	 * 画面表示
	 */
	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(@RequestParam(required = false) String shiteiNo, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		TokugimuJuriTsuchiDto dto = new TokugimuJuriTsuchiDto();

		if (shiteiNo != null && !shiteiNo.isEmpty()) {
			TokugimuJuriTsuchiDto tokugimuInfo = tokugimuJuriTsuchiService.getTokugimuInfo(shiteiNo);
			if (tokugimuInfo != null) {
				dto = tokugimuInfo;
			}
		}

		// 発行日のデフォルト値を今日に設定
		if (dto.getHakkoYmd() == null) {
			dto.setHakkoYmd(LocalDate.now());
		}

		model.addAttribute("dto", dto);
		return "reports/tokugimuJuriTsuchi";
	}

	/**
	 * PDF出力
	 */
	@PostMapping("/pdf")
	@OpeLog(screenId = SCREEN_ID, operation = "PDF")
	public ResponseEntity<byte[]> generatePdf(TokugimuJuriTsuchiDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDispositionFormData("inline", "tokugimu_juri_tsuchi.pdf");

		return ResponseEntity.ok().headers(headers).body(pdfData);
	}

	/**
	 * プレビュー
	 */
	@PostMapping("/preview")
	@OpeLog(screenId = SCREEN_ID, operation = "プレビュー")
	public ResponseEntity<byte[]> preview(TokugimuJuriTsuchiDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		// inline指定でブラウザ内表示を促す
		headers.add("Content-Disposition", "inline; filename=tokugimu_juri_tsuchi_preview.pdf");
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
	public ResponseEntity<byte[]> print(TokugimuJuriTsuchiDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		// inline指定でブラウザ内表示
		headers.add("Content-Disposition", "inline; filename=tokugimu_juri_tsuchi_print.pdf");
		// 印刷用のカスタムヘッダー
		headers.add("X-Print-Action", "true");
		// キャッシュ制御
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		return ResponseEntity.ok().headers(headers).body(pdfData);
	}
}