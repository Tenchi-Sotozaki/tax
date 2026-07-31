package jp.lg.asp.accommodation.controller;

import java.nio.charset.StandardCharsets;
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
import jp.lg.asp.accommodation.annotation.RptLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiReportsService;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 宿泊税特別徴収事務交付金決定通知書・交付申請書 Controller
 */
@Slf4j
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class KofuKetteiTsuchiShinseiController {

	private final KofuKetteiTsuchiShinseiService KofuKetteiTsuchiShinseiService;
	private final KofuKetteiTsuchiShinseiReportsService shinseiReportsService;
	private final ScreenAccessChecker accessChecker;
	private final ReportsCommonService reportsCommonService;
	private static final String SCREEN_ID = ScreenManagement.KOFU_SHINSEI;

	/**
	 * 画面表示
	 */
	@GetMapping("/kofuKetteiTsuchiShinsei")
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(@RequestParam(required = false) String shiteiNo,
			@RequestParam(required = false) String nendo,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
		
		// 現在の日付を取得
		LocalDate now = LocalDate.now();

		// YYYY形式の算定ロジックを共通化
		String targetNendo = nendo;
		if (targetNendo == null || targetNendo.isEmpty()) {
			int currentYear = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
			targetNendo = String.valueOf(currentYear);
		}

		// YYYY形式の年度をDTOにセット
		dto.setNendo(targetNendo);
		
		// 指定番号を設定
		dto.setShiteiNo(shiteiNo);

		model.addAttribute("dto", dto);
		return "reports/kofuKetteiTsuchiShinsei";
	}

	/**
	 * PDF出力
	 */
	@PostMapping("/kofuKetteiTsuchiShinsei/pdf")
	@OpeLog(screenId = SCREEN_ID, operation = "PDF")
	@RptLog(rptId = ReportsConstants.KOFU_SHINSEI, operation = ReportsConstants.SOUSA_PDF, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> generatePdf(KofuKetteiTsuchiShinseiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);
			
			// 年度を取得
			String nendo = dto.getNendo();

			// 年度が入力されていない場合
			if (nendo == null || nendo.isEmpty()) {
				return ResponseEntity.badRequest().body("年度が入力されていません。".getBytes(StandardCharsets.UTF_8));
			}

			// 年度を指定して帳票データを取得
			KofuKetteiTsuchiShinseiDto reportData = KofuKetteiTsuchiShinseiService.getReportData(dto.getShiteiNo(),
					nendo);
			
			if (reportData == null) {
				// データが発見出来なかった時のエラーメッセージを送信
				return ResponseEntity.badRequest().body("指定された条件のデータが見つかりません。".getBytes(StandardCharsets.UTF_8));
			}
			
			// 発行年月日を設定
			reportData.setHakkoYmd(dto.getHakkoYmd());

			// 印刷対象を設定
			reportData.setKetteiTsuchi(dto.isKetteiTsuchi());
			reportData.setShinsei(dto.isShinsei());

			byte[] pdfData = shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(reportData);

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
	@PostMapping("/kofuKetteiTsuchiShinsei/preview")
	@OpeLog(screenId = SCREEN_ID, operation = "プレビュー")
	@RptLog(rptId = ReportsConstants.KOFU_SHINSEI, operation = ReportsConstants.SOUSA_PREVIEW, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> preview(KofuKetteiTsuchiShinseiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);
			
			/// 年度を取得
			String nendo = dto.getNendo();

			// 年度が入力されていない場合
			if (nendo == null || nendo.isEmpty()) {
				return ResponseEntity.badRequest().body("年度が入力されていません。".getBytes(StandardCharsets.UTF_8));
			}

			// 年度を指定して帳票データを取得
			KofuKetteiTsuchiShinseiDto reportData = KofuKetteiTsuchiShinseiService.getReportData(dto.getShiteiNo(),
					nendo);

			if (reportData == null) {
				
				// データが発見出来なかった時のエラーメッセージを送信
				return ResponseEntity.badRequest().body("指定された条件のデータが見つかりません。".getBytes(StandardCharsets.UTF_8));
			}
			
			// 発行年月日を設定
			reportData.setHakkoYmd(dto.getHakkoYmd());

			// 印刷対象を設定
			reportData.setKetteiTsuchi(dto.isKetteiTsuchi());
			reportData.setShinsei(dto.isShinsei());

			byte[] pdfData = shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(reportData);

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
	@PostMapping("/kofuKetteiTsuchiShinsei/print")
	@OpeLog(screenId = SCREEN_ID, operation = "印刷")
	@RptLog(rptId = ReportsConstants.KOFU_SHINSEI, operation = ReportsConstants.SOUSA_PRINT, shiteiNo = "#dto.shiteiNo")
	public ResponseEntity<byte[]> print(KofuKetteiTsuchiShinseiDto dto) {
		try {
			accessChecker.checkAccess(SCREEN_ID);
			
			// 年度を取得
			String nendo = dto.getNendo();

			// 年度が入力されていない場合
			if (nendo == null || nendo.isEmpty()) {
				return ResponseEntity.badRequest().body("年度が入力されていません。".getBytes(StandardCharsets.UTF_8));
			}

			// 年度を指定して帳票データを取得
			KofuKetteiTsuchiShinseiDto reportData = KofuKetteiTsuchiShinseiService.getReportData(dto.getShiteiNo(),
					nendo);

			if (reportData == null) {
				
				// データが発見出来なかった時のエラーメッセージを送信
				return ResponseEntity.badRequest().body("指定された条件のデータが見つかりません。".getBytes(StandardCharsets.UTF_8));
			}
			
			// 発行年月日を設定
			reportData.setHakkoYmd(dto.getHakkoYmd());
			
			// 印刷対象を設定
			reportData.setKetteiTsuchi(dto.isKetteiTsuchi());
			reportData.setShinsei(dto.isShinsei());

			byte[] pdfData = shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(reportData);

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