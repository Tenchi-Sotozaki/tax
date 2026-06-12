package jp.lg.asp.accommodation.controller;

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
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiDto;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiReportsService;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宿泊税特別徴収事務交付金交付決定通知書 Controller
 */
@Slf4j
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class KofuKetteiTsuchiController {

	private final KofuKetteiTsuchiService kofuKetteiTsuchiService;
	private final KofuKetteiTsuchiReportsService reportsService;
	private final ScreenAccessChecker accessChecker;
	private static final String SCREEN_ID = ScreenManagement.KOFU_KETTEI_TSUCHI;

	/**
	 * 画面表示
	 */
	@GetMapping("/kofuKetteiTsuchi")
	public String index(@RequestParam(required = false) String shiteiNo,
			@RequestParam(required = false) String hakkoYmd,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		KofuKetteiTsuchiDto dto = new KofuKetteiTsuchiDto();

		if (shiteiNo != null && !shiteiNo.isEmpty()) {
			dto = kofuKetteiTsuchiService.getReportData(shiteiNo);
			if (dto == null) {
				dto = new KofuKetteiTsuchiDto();
				dto.setShiteiNo(shiteiNo);
			}
		}

		// 発行年月日が指定されている場合はそれを使用、されていない場合は当日を設定
		if (hakkoYmd != null && !hakkoYmd.isEmpty()) {
			dto.setHakkoYmd(hakkoYmd);
		} else if (dto.getHakkoYmd() == null || dto.getHakkoYmd().isEmpty()) {
			// 当日をデフォルトとして設定
			java.time.LocalDate today = java.time.LocalDate.now();
			dto.setHakkoYmd(today.toString());
		}

		model.addAttribute("dto", dto);
		return "reports/kofuKetteiTsuchi";
	}

	/**
	 * PDF出力
	 */
	@PostMapping("/kofuKetteiTsuchi/pdf")
	public ResponseEntity<byte[]> generatePdf(KofuKetteiTsuchiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);
			
			KofuKetteiTsuchiDto reportData = kofuKetteiTsuchiService.getReportData(dto.getShiteiNo());
			if (reportData == null) {
				log.error("報告データが取得できません。指定番号: {}", dto.getShiteiNo());
				return ResponseEntity.badRequest().build();
			}

			// 画面入力値をコピー（日付フォーマット変換）
			if (dto.getHakkoYmd() != null) {
				// yyyy-MM-dd 形式から yyyy年MM月dd日 形式に変換
				try {
					java.time.LocalDate date = java.time.LocalDate.parse(dto.getHakkoYmd());
					String formattedDate = String.format("%d年%d月%d日", 
							date.getYear(), 
							date.getMonthValue(), 
							date.getDayOfMonth());
					reportData.setHakkoYmd(formattedDate);
				} catch (Exception e) {
					// パースエラーの場合はそのまま使用
					reportData.setHakkoYmd(dto.getHakkoYmd());
				}
			}

			byte[] pdfData = reportsService.generateKofuKetteiTsuchiPdf(reportData);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("inline", "kofu_kettei_tsuchi.pdf");

			return ResponseEntity.ok().headers(headers).body(pdfData);
		} catch (Exception e) {
			log.error("PDF生成中にエラーが発生しました", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * プレビュー
	 */
	@PostMapping("/kofuKetteiTsuchi/preview")
	public ResponseEntity<byte[]> preview(KofuKetteiTsuchiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);
			
			KofuKetteiTsuchiDto reportData = kofuKetteiTsuchiService.getReportData(dto.getShiteiNo());
			if (reportData == null) {
				log.error("報告データが取得できません。指定番号: {}", dto.getShiteiNo());
				return ResponseEntity.badRequest().build();
			}

			// 画面入力値をコピー（日付フォーマット変換）
			if (dto.getHakkoYmd() != null) {
				// yyyy-MM-dd 形式から yyyy年MM月dd日 形式に変換
				try {
					java.time.LocalDate date = java.time.LocalDate.parse(dto.getHakkoYmd());
					String formattedDate = String.format("%d年%d月%d日", 
							date.getYear(), 
							date.getMonthValue(), 
							date.getDayOfMonth());
					reportData.setHakkoYmd(formattedDate);
				} catch (Exception e) {
					// パースエラーの場合はそのまま使用
					reportData.setHakkoYmd(dto.getHakkoYmd());
				}
			}

			byte[] pdfData = reportsService.generateKofuKetteiTsuchiPdf(reportData);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.add("Content-Disposition", "inline; filename=kofu_kettei_tsuchi_preview.pdf");
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");

			return ResponseEntity.ok().headers(headers).body(pdfData);
		} catch (Exception e) {
			log.error("プレビュー生成中にエラーが発生しました", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * 印刷
	 */
	@PostMapping("/kofuKetteiTsuchi/print")
	public ResponseEntity<byte[]> print(KofuKetteiTsuchiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);
			
			KofuKetteiTsuchiDto reportData = kofuKetteiTsuchiService.getReportData(dto.getShiteiNo());
			if (reportData == null) {
				log.error("報告データが取得できません。指定番号: {}", dto.getShiteiNo());
				return ResponseEntity.badRequest().build();
			}

			// 画面入力値をコピー（日付フォーマット変換）
			if (dto.getHakkoYmd() != null) {
				// yyyy-MM-dd 形式から yyyy年MM月dd日 形式に変換
				try {
					java.time.LocalDate date = java.time.LocalDate.parse(dto.getHakkoYmd());
					String formattedDate = String.format("%d年%d月%d日", 
							date.getYear(), 
							date.getMonthValue(), 
							date.getDayOfMonth());
					reportData.setHakkoYmd(formattedDate);
				} catch (Exception e) {
					// パースエラーの場合はそのまま使用
					reportData.setHakkoYmd(dto.getHakkoYmd());
				}
			}

			byte[] pdfData = reportsService.generateKofuKetteiTsuchiPdf(reportData);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.add("Content-Disposition", "inline; filename=kofu_kettei_tsuchi_print.pdf");
			headers.add("X-Print-Action", "true");
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");

			return ResponseEntity.ok().headers(headers).body(pdfData);
		} catch (Exception e) {
			log.error("印刷用PDF生成中にエラーが発生しました", e);
			return ResponseEntity.internalServerError().build();
		}
	}
}