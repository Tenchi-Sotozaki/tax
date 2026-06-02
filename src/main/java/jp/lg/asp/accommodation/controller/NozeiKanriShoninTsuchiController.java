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

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.NozeiKanriShoninTsuchiDto;
import jp.lg.asp.accommodation.service.NozeiKanriShoninTsuchiReportsService;
import jp.lg.asp.accommodation.service.NozeiKanriShoninTsuchiService;
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
	private final ScreenAccessChecker accessChecker;
	private static final String SCREEN_ID = ScreenManagement.NOZEI_KANRININ_SHONIN_TSUCHI;

	/**
	 * 画面表示
	 */
	@GetMapping
	public String index(@RequestParam(required = false) String shiteiNo, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		NozeiKanriShoninTsuchiDto dto = new NozeiKanriShoninTsuchiDto();

		if (shiteiNo != null && !shiteiNo.isEmpty()) {
			try {
				NozeiKanriShoninTsuchiDto nozeiKanriInfo = nozeiKanriShoninTsuchiService.getNozeiKanriInfo(shiteiNo);
				if (nozeiKanriInfo != null) {
					dto = nozeiKanriInfo;
				}
			} catch (Exception e) {
				log.warn("納税管理人情報取得エラー: {}", e.getMessage());
				// エラーの場合は空のDTOを使用
			}
		}

		// 発行日のデフォルト値を今日に設定
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
	public ResponseEntity<byte[]> generatePdf(NozeiKanriShoninTsuchiDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDispositionFormData("inline", "nozei_kanri_shonin_tsuchi.pdf");

		return ResponseEntity.ok().headers(headers).body(pdfData);
	}

	/**
	 * プレビュー
	 */
	@PostMapping("/preview")
	public ResponseEntity<byte[]> preview(NozeiKanriShoninTsuchiDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		// inline指定でブラウザ内表示を促す
		headers.add("Content-Disposition", "inline; filename=nozei_kanri_shonin_tsuchi_preview.pdf");
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
	public ResponseEntity<byte[]> print(NozeiKanriShoninTsuchiDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		// inline指定でブラウザ内表示
		headers.add("Content-Disposition", "inline; filename=nozei_kanri_shonin_tsuchi_print.pdf");
		// 印刷用のカスタムヘッダー
		headers.add("X-Print-Action", "true");
		// キャッシュ制御
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		return ResponseEntity.ok().headers(headers).body(pdfData);
	}
}