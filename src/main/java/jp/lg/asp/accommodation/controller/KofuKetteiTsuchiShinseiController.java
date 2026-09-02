package jp.lg.asp.accommodation.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.chrono.JapaneseDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import jakarta.servlet.http.HttpSession;

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
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiReportsService;
import jp.lg.asp.accommodation.service.KofuKetteiTsuchiShinseiService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.util.SessionHelper;
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
	public String index(HttpSession session,
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
		
		// 指定番号または合算指定番号が存在しない場合
		ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
		String shiteiNo = SessionHelper.getShiteiNo(session);
		String gassanShiteiNo = SessionHelper.getGassanShiteiNo(session);
		if (selected == null || (shiteiNo == null && gassanShiteiNo == null)) {
			// 画面を戻して検索モーダルを表示
			model.addAttribute("showShiteiGassanModal", true);
			return "tokugimu/tTokugimuReport";
		}

		// 指定番号を設定
		dto.setShiteiNo(shiteiNo != null ? shiteiNo : gassanShiteiNo);

		model.addAttribute("dto", dto);
		return "reports/kofuKetteiTsuchiShinsei";
	}

	/**
	 * PDF出力
	 */
	@PostMapping("/kofuKetteiTsuchiShinsei/pdf")
	@OpeLog(screenId = SCREEN_ID, operation = "PDF")
	public ResponseEntity<byte[]> generatePdf(KofuKetteiTsuchiShinseiDto dto) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDispositionFormData("inline", "kofu_shinsei.pdf");
		return processReport(dto, ReportsConstants.SOUSA_PDF, headers, "PDF生成中にエラーが発生しました");
	}

	/**
	 * プレビュー
	 */
	@PostMapping("/kofuKetteiTsuchiShinsei/preview")
	@OpeLog(screenId = SCREEN_ID, operation = "プレビュー")
	public ResponseEntity<byte[]> preview(KofuKetteiTsuchiShinseiDto dto) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.add("Content-Disposition", "inline; filename=kofu_shinsei_preview.pdf");
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		return processReport(dto, ReportsConstants.SOUSA_PREVIEW, headers, "プレビュー生成中にエラーが発生しました");
	}

	/**
	 * 印刷
	 */
	@PostMapping("/kofuKetteiTsuchiShinsei/print")
	@OpeLog(screenId = SCREEN_ID, operation = "印刷")
	public ResponseEntity<byte[]> print(KofuKetteiTsuchiShinseiDto dto) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.add("Content-Disposition", "inline; filename=kofu_shinsei_print.pdf");
		headers.add("X-Print-Action", "true");
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		return processReport(dto, ReportsConstants.SOUSA_PRINT, headers, "印刷用PDF生成中にエラーが発生しました");
	}
	
	/**
	 * 帳票PDF生成の共通処理
	 * @param dto：帳票データ
	 * @param operation：操作
	 * @param headers：HTTPレスポンスに付与するヘッダー情報
	 * @param errorMessage：エラーメッセージ
	 * @return 処理の結果
	 */
	private ResponseEntity<byte[]> processReport(KofuKetteiTsuchiShinseiDto dto, String operation, HttpHeaders headers, String errorMessage) {
		try {
			accessChecker.checkAccess(SCREEN_ID);

			// 年度を取得
			String nendo = dto.getNendo();

			// 年度が入力されていない場合
			if (nendo == null || nendo.isEmpty()) {
				return ResponseEntity.badRequest().body("年度が入力されていません。".getBytes(StandardCharsets.UTF_8));
			}

			// 帳票データの生成
			KofuKetteiTsuchiShinseiDto reportData = KofuKetteiTsuchiShinseiService.getReportData(dto.getShiteiNo(), nendo);
			if (reportData == null) {
				return ResponseEntity.badRequest().body("指定された条件のデータが見つかりません。".getBytes(StandardCharsets.UTF_8));
			}
			
			// 発行年月日を取得
			String seirekiYmd = dto.getHakkoYmd();

			// 発行年月日の情報が存在する
			if (seirekiYmd != null && !seirekiYmd.isEmpty()) {
				// 西暦の文字列を LocalDate に変換
				LocalDate localDate = LocalDate.parse(seirekiYmd, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

				// LocalDate から和暦に変換
				JapaneseDate japaneseDate = JapaneseDate.from(localDate);

				// フォーマット
				DateTimeFormatter warekiFormatter = DateTimeFormatter.ofPattern("Gy年M月d日", Locale.JAPAN);
				String warekiYmd = warekiFormatter.format(japaneseDate);

				// reportData へ設定
				reportData.setHakkoYmd(warekiYmd);
			}
			
			// データの設定
			reportData.setKetteiTsuchi(dto.isKetteiTsuchi()); // 決定通知書の生成フラグ
			reportData.setShinsei(dto.isShinsei()); // 交付申請書の生成フラグ
			reportData.setOperation(operation); // 操作

			// PDF生成
			byte[] pdfData = shinseiReportsService.generatekofuKetteiTsuchiShinseiPdf(reportData);

			return ResponseEntity.ok().headers(headers).body(pdfData);
		} catch (Exception e) {
			log.error(errorMessage, e);
			return ResponseEntity.internalServerError().build();
		}
	}
}