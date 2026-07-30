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

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.annotation.RptLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuShinseiDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.KofuShinseiReportsService;
import jp.lg.asp.accommodation.service.KofuShinseiService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宿泊税特別徴収事務交付金交付申請書 Controller
 */
@Slf4j
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class KofuShinseiController {

	private final KofuShinseiService kofuShinseiService;
	private final KofuShinseiReportsService reportsService;
	private final ScreenAccessChecker accessChecker;
	private final ReportsCommonService reportsCommonService;
	private static final String SCREEN_ID = ScreenManagement.KOFU_SHINSEI;

	/**
	 * 画面表示
	 */
	@GetMapping("/kofuShinsei")
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(HttpSession session,
			@RequestParam(required = false) String nendo,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		String shiteiNo = SessionHelper.getShiteiNo(session);
		KofuShinseiDto dto = new KofuShinseiDto();

		if (shiteiNo == null || shiteiNo.isEmpty()) {
			model.addAttribute("showShiteiGassanModal", true);
			model.addAttribute("dto", dto);
			return "reports/kofuShinsei";
		}

		if (nendo != null && !nendo.isEmpty()) {
			String nendoYear = nendo.split("-")[0];
			dto = kofuShinseiService.getReportData(shiteiNo, nendoYear);
		} else {
			dto = kofuShinseiService.getReportData(shiteiNo);
		}

		if (dto == null) {
			dto = new KofuShinseiDto();
			dto.setShiteiNo(shiteiNo);
		}
		if (nendo != null && !nendo.isEmpty()) {
			dto.setNendo(nendo);
		} else if (dto.getNendo() == null || dto.getNendo().isEmpty()) {
			java.time.LocalDate now = java.time.LocalDate.now();
			int currentYear = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
			dto.setNendo(currentYear + "-04");
		}
		
		model.addAttribute("dto", dto);
		return "reports/kofuShinsei";
	}

	/**
	 * 年度変更時のデータ取得API
	 */
	@PostMapping("/kofuShinsei/reload")
	@OpeLog(screenId = SCREEN_ID, operation = "年度更新")
	public ResponseEntity<KofuShinseiDto> reloadData(@RequestParam String shiteiNo,
			@RequestParam String nendo) {
		try {
			accessChecker.checkAccess(SCREEN_ID);

			// 年度からyyyy部分のみを抽出
			String nendoYear = nendo.split("-")[0];
			KofuShinseiDto dto = kofuShinseiService.getReportData(shiteiNo, nendoYear);

			if (dto == null) {
				dto = new KofuShinseiDto();
				dto.setShiteiNo(shiteiNo);
				dto.setNendo(nendo);
			} else {
				dto.setNendo(nendo);
			}

			return ResponseEntity.ok(dto);
		} catch (Exception e) {
			log.error("データ再読み込み中にエラーが発生しました", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * PDF出力
	 */
	@PostMapping("/kofuShinsei/pdf")
	@OpeLog(screenId = SCREEN_ID, operation = "PDF")
	@RptLog(rptId = ReportsConstants.KOFU_SHINSEI, operation = ReportsConstants.SOUSA_PDF, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> generatePdf(KofuShinseiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);

			// 年度が指定されている場合はその年度で取得
			KofuShinseiDto reportData;
			if (dto.getNendo() != null && !dto.getNendo().isEmpty()) {
				String nendoYear = dto.getNendo().split("-")[0];
				reportData = kofuShinseiService.getReportData(dto.getShiteiNo(), nendoYear);
			} else {
				reportData = kofuShinseiService.getReportData(dto.getShiteiNo());
			}

			if (reportData == null) {
				log.error("報告データが取得できません。指定番号: {}, 年度: {}", dto.getShiteiNo(), dto.getNendo());
				return ResponseEntity.badRequest().build();
			}

			byte[] pdfData = reportsService.generateKofuShinseiPdf(reportData);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("inline", "kofu_shinsei.pdf");

			return ResponseEntity.ok().headers(headers).body(pdfData);
		} catch (Exception e) {
			log.error("PDF生成中にエラーが発生しました", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * プレビュー
	 */
	@PostMapping("/kofuShinsei/preview")
	@OpeLog(screenId = SCREEN_ID, operation = "プレビュー")
	@RptLog(rptId = ReportsConstants.KOFU_SHINSEI, operation = ReportsConstants.SOUSA_PREVIEW, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> preview(KofuShinseiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);

			// 年度が指定されている場合はその年度で取得
			KofuShinseiDto reportData;
			if (dto.getNendo() != null && !dto.getNendo().isEmpty()) {
				String nendoYear = dto.getNendo().split("-")[0];
				reportData = kofuShinseiService.getReportData(dto.getShiteiNo(), nendoYear);
			} else {
				reportData = kofuShinseiService.getReportData(dto.getShiteiNo());
			}

			if (reportData == null) {
				log.error("報告データが取得できません。指定番号: {}, 年度: {}", dto.getShiteiNo(), dto.getNendo());
				return ResponseEntity.badRequest().build();
			}

			byte[] pdfData = reportsService.generateKofuShinseiPdf(reportData);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.add("Content-Disposition", "inline; filename=kofu_shinsei_preview.pdf");
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
	@PostMapping("/kofuShinsei/print")
	@OpeLog(screenId = SCREEN_ID, operation = "印刷")
	@RptLog(rptId = ReportsConstants.KOFU_SHINSEI, operation = ReportsConstants.SOUSA_PRINT, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> print(KofuShinseiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);

			// 年度が指定されている場合はその年度で取得
			KofuShinseiDto reportData;
			if (dto.getNendo() != null && !dto.getNendo().isEmpty()) {
				String nendoYear = dto.getNendo().split("-")[0];
				reportData = kofuShinseiService.getReportData(dto.getShiteiNo(), nendoYear);
			} else {
				reportData = kofuShinseiService.getReportData(dto.getShiteiNo());
			}

			if (reportData == null) {
				log.error("報告データが取得できません。指定番号: {}, 年度: {}", dto.getShiteiNo(), dto.getNendo());
				return ResponseEntity.badRequest().build();
			}

			byte[] pdfData = reportsService.generateKofuShinseiPdf(reportData);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.add("Content-Disposition", "inline; filename=kofu_shinsei_print.pdf");
			headers.add("X-Print-Action", "true");
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");

			return ResponseEntity.ok().headers(headers).body(pdfData);
		} catch (Exception e) {
			log.error("印刷用PDF生成中にエラーが発生しました", e);
			return ResponseEntity.internalServerError().build();
		}
	}
}