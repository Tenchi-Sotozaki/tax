package jp.lg.asp.accommodation.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

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
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.dto.KofukinBulkPrintForm;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiReportsService;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class KofukinBulkPrintController {

	private final KofuKetteiTsuchiShinseiService kofuKetteiTsuchiShinseiService;
	private final KofuKetteiTsuchiShinseiReportsService kofuKetteiTsuchiShinseiReportsService;
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
		form.setKofuShinsei(true);
		form.setKofuKetteiTsuchi(true);
		model.addAttribute("form", form);
		return VIEW;
	}

	@PostMapping("/kofukinBulkPrint/pdf")
	@OpeLog(screenId = SCREEN_ID, operation = "PDF")
	public ResponseEntity<byte[]> pdf(@ModelAttribute("form") KofukinBulkPrintForm form, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		return generateResponse(form, "kofukin_bulk.pdf", false, true, ReportsConstants.SOUSA_PDF);
	}

	@PostMapping("/kofukinBulkPrint/preview")
	@OpeLog(screenId = SCREEN_ID, operation = "プレビュー")
	public ResponseEntity<byte[]> preview(@ModelAttribute("form") KofukinBulkPrintForm form, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		return generateResponse(form, "kofukin_bulk_preview.pdf", false, false, ReportsConstants.SOUSA_PREVIEW);
	}

	@PostMapping("/kofukinBulkPrint/print")
	@OpeLog(screenId = SCREEN_ID, operation = "印刷")
	public ResponseEntity<byte[]> print(@ModelAttribute("form") KofukinBulkPrintForm form, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		return generateResponse(form, "kofukin_bulk_print.pdf", true, false, ReportsConstants.SOUSA_PRINT);
	}

	private ResponseEntity<byte[]> generateResponse(KofukinBulkPrintForm form, String filename, boolean printAction,
			boolean download, String operation) {
		try {
			if (!form.isKofuShinsei() && !form.isKofuKetteiTsuchi()) {
				byte[] errorBytes = "交付申請または交付決定通知のいずれかを選択してください。".getBytes(StandardCharsets.UTF_8);
				return ResponseEntity.badRequest().body(errorBytes);
			}

			List<KofuKetteiTsuchiShinseiDto> dtoList = kofuKetteiTsuchiShinseiService.getAllReportData(form.getNendo());
			if (dtoList == null || dtoList.isEmpty()) {
				byte[] errorBytes = "対象年度のレポートデータが存在しません。".getBytes(StandardCharsets.UTF_8);
				return ResponseEntity.badRequest().body(errorBytes);
			}

			String formattedDate = formatDate(form.getHakkoYmd());
			for (KofuKetteiTsuchiShinseiDto dto : dtoList) {
				dto.setHakkoYmd(formattedDate);
				dto.setShinsei(form.isKofuShinsei());
				dto.setKetteiTsuchi(form.isKofuKetteiTsuchi());
				dto.setOperation(operation);
			}

			byte[] pdfData = kofuKetteiTsuchiShinseiReportsService.generateBulkPdf(dtoList);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.add("Content-Disposition", (download ? "attachment" : "inline") + "; filename=" + filename);
			headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
			if (printAction) {
				headers.add("X-Print-Action", "true");
			}
			return ResponseEntity.ok().headers(headers).body(pdfData);

		} catch (Exception e) {
			log.error("帳票一括発行エラー", e);
			byte[] errorBytes = "帳票一括発行処理でエラーが発生しました。".getBytes(StandardCharsets.UTF_8);
			return ResponseEntity.internalServerError().body(errorBytes);
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
