package jp.lg.asp.accommodation.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ReportsConfigForm;
import jp.lg.asp.accommodation.entity.KoinTorikomi;
import jp.lg.asp.accommodation.service.KoinTorikomiService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reports-config")
public class ReportsConfigController {

	private final KoinTorikomiService koinTorikomiService;
	private final ScreenAccessChecker accessChecker;

	private final JichitaiContext jichitaiContext;

	private static final String SCREEN_ID = ScreenManagement.REPORTS_CONFIG;

	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		List<KoinTorikomi> importHistory = koinTorikomiService.getImportHistory();
		model.addAttribute("reportsConfigForm", new ReportsConfigForm());
		model.addAttribute("importHistory", importHistory);
		return "admin/reportsConfig";
	}

	@PostMapping("/import")
	@OpeLog(screenId = SCREEN_ID, operation = "取込")
	public String importFile(ReportsConfigForm form, RedirectAttributes redirectAttributes,
			Authentication authentication) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		try {
			if (form.getFile() == null || form.getFile().isEmpty()) {
				redirectAttributes.addFlashAttribute("errorMessage", "ファイルを選択してください。");
				return "redirect:/admin/reports-config";
			}

			// ファイルサイズチェック
			if (form.getFile().getSize() > 10 * 1024 * 1024) { // 10MB
				redirectAttributes.addFlashAttribute("errorMessage", "ファイルサイズが10MBを超えています。");
				return "redirect:/admin/reports-config";
			}

			// ファイルタイプチェック
			String contentType = form.getFile().getContentType();
			if (contentType == null || !contentType.equals("image/png")) {
				redirectAttributes.addFlashAttribute("errorMessage", "PNG画像ファイルのみアップロード可能です。");
				return "redirect:/admin/reports-config";
			}

			String userId = authentication.getName();

			koinTorikomiService.importReportFile(form.getFile(), jichitaiContext.getJichitaiCd(), userId);
			redirectAttributes.addFlashAttribute("successMessage", "帳票ファイルの取り込みが完了しました。");
		} catch (Exception e) {
			e.printStackTrace(); // コンソールにエラーを出力
			redirectAttributes.addFlashAttribute("errorMessage", "帳票ファイルの取り込みに失敗しました：" + e.getMessage());
		}

		return "redirect:/admin/reports-config";
	}
}