package jp.lg.asp.accommodation.controller;

import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.annotation.RptLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiDto;
import jp.lg.asp.accommodation.dto.KofuShinseiDto;
import jp.lg.asp.accommodation.dto.KofukinBulkPrintForm;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiReportsService;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiService;
import jp.lg.asp.accommodation.service.KofuShinseiReportsService;
import jp.lg.asp.accommodation.service.KofuShinseiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class KofukinBulkPrintController {

	private final KofuShinseiService kofuShinseiService;
	private final KofuShinseiReportsService kofuShinseiReportsService;
	private final KofuKetteiTsuchiService kofuKetteiTsuchiService;
	private final KofuKetteiTsuchiReportsService kofuKetteiTsuchiReportsService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.KOFUKIN_BULK_PRINT;
	private static final String VIEW = "reports/kofukinBulkPrint";

	@GetMapping("/kofukinBulkPrint")
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		KofukinBulkPrintForm form = new KofukinBulkPrintForm();
		form.setHakkoYmd(LocalDate.now().toString());
		int currentYear = LocalDate.now().getMonthValue() >= 4 ? LocalDate.now().getYear()
				: LocalDate.now().getYear() - 1;
		form.setNendo(String.valueOf(currentYear));
		model.addAttribute("form", form);
		return VIEW;
	}

	@PostMapping("/kofukinBulkPrint/pdf")
	@OpeLog(screenId = SCREEN_ID, operation = "PDF")
	@RptLog(rptId = ReportsConstants.KOFU_SHINSEI, operation = ReportsConstants.SOUSA_PDF, shiteiNo = "")
	public ResponseEntity<byte[]> pdf(@ModelAttribute("form") KofukinBulkPrintForm form, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		return generateResponse(form, ReportsConstants.SOUSA_PDF, "kofukin_bulk.pdf", false);
	}

	@PostMapping("/kofukinBulkPrint/preview")
	@OpeLog(screenId = SCREEN_ID, operation = "プレビュー")
	@RptLog(rptId = ReportsConstants.KOFU_SHINSEI, operation = ReportsConstants.SOUSA_PREVIEW, shiteiNo = "")
	public ResponseEntity<byte[]> preview(@ModelAttribute("form") KofukinBulkPrintForm form, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		return generateResponse(form, ReportsConstants.SOUSA_PREVIEW, "kofukin_bulk_preview.pdf", false);
	}

	@PostMapping("/kofukinBulkPrint/print")
	@OpeLog(screenId = SCREEN_ID, operation = "印刷")
	@RptLog(rptId = ReportsConstants.KOFU_SHINSEI, operation = ReportsConstants.SOUSA_PRINT, shiteiNo = "")
	public ResponseEntity<byte[]> print(@ModelAttribute("form") KofukinBulkPrintForm form, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		return generateResponse(form, ReportsConstants.SOUSA_PRINT, "kofukin_bulk_print.pdf", true);
	}

	private ResponseEntity<byte[]> generateResponse(KofukinBulkPrintForm form, String sousa, String filename,
			boolean printAction) {
		try {
			if (!form.isKofuShinsei() && !form.isKofuKetteiTsuchi()) {
				return ResponseEntity.badRequest().build();
			}

			byte[] pdfData = null;

			if (form.isKofuShinsei()) {
				KofuShinseiDto dto = kofuShinseiService.getReportData(null, form.getNendo());
				if (dto != null) {
					dto.setHakkoYmd(form.getHakkoYmd());
					pdfData = kofuShinseiReportsService.generateKofuShinseiPdf(dto);
				}
			}

			if (form.isKofuKetteiTsuchi()) {
				KofuKetteiTsuchiDto dto = kofuKetteiTsuchiService.getReportData(null);
				if (dto != null) {
					dto.setHakkoYmd(formatDate(form.getHakkoYmd()));
					pdfData = kofuKetteiTsuchiReportsService.generateKofuKetteiTsuchiPdf(dto);
				}
			}

			if (pdfData == null) {
				return ResponseEntity.badRequest().build();
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.add("Content-Disposition", "inline; filename=" + filename);
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
			if (printAction) {
				headers.add("X-Print-Action", "true");
			}
			return ResponseEntity.ok().headers(headers).body(pdfData);

		} catch (Exception e) {
			log.error("帳票一括発行エラー", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	private String formatDate(String ymd) {
		if (ymd == null || ymd.isEmpty())
			return ymd;
		try {
			LocalDate date = LocalDate.parse(ymd);
			return String.format("%d年%d月%d日", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
		} catch (Exception e) {
			return ymd;
		}
	}
}
