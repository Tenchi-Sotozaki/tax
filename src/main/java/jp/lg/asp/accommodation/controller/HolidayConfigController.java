package jp.lg.asp.accommodation.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.HolidayConfigForm;
import jp.lg.asp.accommodation.service.HolidayConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/admin/holiday")
@RequiredArgsConstructor
public class HolidayConfigController {

	private final HolidayConfigService holidayConfigService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.HOLIDAY_CONFIG;
	private static final String VIEW = "admin/holidayConfig";

	@GetMapping
	public String index(RedirectAttributes redirectAttributes) {
		//accessChecker.checkAccess(SCREEN_ID);
		List<String> nenList = holidayConfigService.findNendoList();
		if (!nenList.isEmpty()) {
			return "redirect:/admin/holiday/view/" + nenList.getLast();
		}
		return "redirect:/admin/holiday/view/" + LocalDate.now().getYear();
	}

	@GetMapping("/view/{nen}")
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String view(@PathVariable String nen, Model model) {
		//accessChecker.checkAccess(SCREEN_ID);
		model.addAttribute("form", holidayConfigService.findByNendo(nen));
		model.addAttribute("nenList", holidayConfigService.findNendoList());
		model.addAttribute("mode", "view");
		return VIEW;
	}

	@GetMapping("/edit/{nen}")
	@OpeLog(screenId = SCREEN_ID, operation = "編集画面表示")
	public String edit(@PathVariable String nen, Model model) {
		//accessChecker.checkWriteAccess(SCREEN_ID);
		model.addAttribute("form", holidayConfigService.findByNendo(nen));
		model.addAttribute("nenList", holidayConfigService.findNendoList());
		model.addAttribute("mode", "edit");
		return VIEW;
	}

	@PostMapping("/save")
	@OpeLog(screenId = SCREEN_ID, operation = "更新")
	public String save(@ModelAttribute("form") HolidayConfigForm form,
			Model model, RedirectAttributes redirectAttributes) {
		//accessChecker.checkWriteAccess(SCREEN_ID);
		if (form.getNendo() == null || form.getNendo().isBlank()) {
			model.addAttribute("form", form);
			model.addAttribute("mode", "edit");
			model.addAttribute("errorMessage", "年は必須です。");
			return VIEW;
		}
		try {
			holidayConfigService.save(form);
			redirectAttributes.addFlashAttribute("successMessage", "休業日設定を更新しました。");
		} catch (Exception e) {
			log.error("休業日設定更新エラー", e);
			model.addAttribute("form", form);
			model.addAttribute("mode", "edit");
			model.addAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
			return VIEW;
		}
		return "redirect:/admin/holiday/view/" + form.getNendo();
	}
}
