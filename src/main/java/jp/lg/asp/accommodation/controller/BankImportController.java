package jp.lg.asp.accommodation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.BankImportResultDto;
import jp.lg.asp.accommodation.service.BankImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 金融機関コード取込 Controller
 *
 * 全自治体共通の金融機関マスタ・支店マスタを更新するGE運用者向けの画面。
 * サイドメニューには表示せず、SecurityConfig の /admin/** により ROLE_ADMIN のみアクセスできる。
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/bank-import")
public class BankImportController {

	private static final String SCREEN_ID = ScreenManagement.BANK_IMPORT;
	private static final String VIEW = "admin/bankImport";

	private final BankImportService bankImportService;
	private final ScreenAccessChecker accessChecker;

	/**
	 * 画面表示
	 */
	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index() {
		accessChecker.checkAccess(SCREEN_ID);
		return VIEW;
	}

	/**
	 * zipを取り込み、結果を同じ画面に表示する。
	 */
	@PostMapping("/upload")
	@OpeLog(screenId = SCREEN_ID, operation = "取込")
	public String upload(@RequestParam("file") MultipartFile file, Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		try {
			BankImportResultDto result = bankImportService.importFromZip(file);
			model.addAttribute("result", result);
			model.addAttribute("successMessage", "金融機関コードの取込が完了しました。");
		} catch (IllegalStateException e) {
			model.addAttribute("errorMessage", e.getMessage());
		} catch (Exception e) {
			log.error("金融機関コード取込エラー", e);
			model.addAttribute("errorMessage", "取込処理中にエラーが発生しました。");
		}
		return VIEW;
	}
}
