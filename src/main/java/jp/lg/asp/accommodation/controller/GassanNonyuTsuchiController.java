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
import jp.lg.asp.accommodation.dto.GassanNonyuTsuchiDto;
import jp.lg.asp.accommodation.service.GassanNonyuTsuchiReportsService;
import jp.lg.asp.accommodation.service.GassanNonyuTsuchiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 合算申告納入承認通知書 Controller
 */
@Slf4j
@Controller
@RequestMapping("/reports/gassanNonyuTsuchi")
@RequiredArgsConstructor
public class GassanNonyuTsuchiController {

	private final GassanNonyuTsuchiService gassanNonyuTsuchiService;
	private final GassanNonyuTsuchiReportsService reportsService;
	private final ScreenAccessChecker accessChecker;
	private static final String SCREEN_ID = ScreenManagement.GASSAN_NONYU_TSUCHI;

	/**
	 * 画面表示
	 */
	@GetMapping
	public String index(@RequestParam(required = false) String shiteiNo, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		GassanNonyuTsuchiDto dto = new GassanNonyuTsuchiDto();

		if (shiteiNo != null && !shiteiNo.isEmpty()) {
			GassanNonyuTsuchiDto info = gassanNonyuTsuchiService.getGassanNonyuTsuchiInfo(shiteiNo);
			if (info != null) {
				dto = info;
			}
		}

		if (dto.getHakkoYmd() == null) {
			dto.setHakkoYmd(LocalDate.now());
		}

		model.addAttribute("dto", dto);
		return "reports/gassanNonyuTsuchi";
	}

	/**
	 * PDF出力
	 */
	@PostMapping("/pdf")
	public ResponseEntity<byte[]> generatePdf(GassanNonyuTsuchiDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDispositionFormData("inline", "gassan_nonyu_tsuchi.pdf");

		return ResponseEntity.ok().headers(headers).body(pdfData);
	}

	/**
	 * プレビュー
	 */
	@PostMapping("/preview")
	public ResponseEntity<byte[]> preview(GassanNonyuTsuchiDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.add("Content-Disposition", "inline; filename=gassan_nonyu_tsuchi_preview.pdf");
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		return ResponseEntity.ok().headers(headers).body(pdfData);
	}

	/**
	 * 印刷
	 */
	@PostMapping("/print")
	public ResponseEntity<byte[]> print(GassanNonyuTsuchiDto dto) {
		accessChecker.checkAccess(SCREEN_ID);
		byte[] pdfData = reportsService.generateTsuchiPdf(dto);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.add("Content-Disposition", "inline; filename=gassan_nonyu_tsuchi_print.pdf");
		headers.add("X-Print-Action", "true");
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");

		return ResponseEntity.ok().headers(headers).body(pdfData);
	}
}
