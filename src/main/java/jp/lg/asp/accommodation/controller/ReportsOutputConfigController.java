package jp.lg.asp.accommodation.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.service.ReportsOutputConfigService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reports-output-config")
public class ReportsOutputConfigController {

	private final ReportsOutputConfigService reportsOutputConfigService;
	private final ScreenAccessChecker accessChecker;
	private final JichitaiContext jichitaiContext;

	private static final String SCREEN_ID = ScreenManagement.REPORTS_OUTPUT_CONFIG;

	@GetMapping("/view")
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String view(Model model) {
//		accessChecker.checkAccess(SCREEN_ID);
		model.addAttribute("defTextMap", reportsOutputConfigService.getDefTextMap(jichitaiContext.getJichitaiCd()));
		model.addAttribute("mode", "view");
		return "admin/reportsOutputConfig";
	}

	@GetMapping("/edit")
	@OpeLog(screenId = SCREEN_ID, operation = "編集画面表示")
	public String edit(Model model) {
//		accessChecker.checkWriteAccess(SCREEN_ID);
		model.addAttribute("defTextMap", reportsOutputConfigService.getDefTextMap(jichitaiContext.getJichitaiCd()));
		model.addAttribute("mode", "edit");
		return "admin/reportsOutputConfig";
	}

	@PostMapping("/save")
	@OpeLog(screenId = SCREEN_ID, operation = "登録")
	public String save(@RequestParam Map<String, String> params, Authentication authentication,
			RedirectAttributes redirectAttributes) {
//		accessChecker.checkWriteAccess(SCREEN_ID);
		try {
			reportsOutputConfigService.saveDefText(jichitaiContext.getJichitaiCd(), authentication.getName(), params);
			redirectAttributes.addFlashAttribute("successMessage", "帳票出力項目を登録しました。");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "登録に失敗しました: " + e.getMessage());
			return "redirect:/admin/reports-output-config/edit";
		}
		return "redirect:/admin/reports-output-config/view";
	}
}
