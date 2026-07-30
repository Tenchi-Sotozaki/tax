package jp.lg.asp.accommodation.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;
import jp.lg.asp.accommodation.service.ShoreikinConfigService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 特別徴収事務交付金照会／登録／編集 Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/shoreikin")
public class ShoreikinConfigController {

	private final ShoreikinConfigService shoreikinConfigService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.SHOREIKIN_CONFIG;
	private static final String CONFIG_VIEW = "shoreikin/shoreikinConfig";

	@GetMapping("/config")
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String config(HttpSession session,
			@RequestParam(required = false) String nendo,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		String shiteiNo = SessionHelper.getShiteiNo(session);

		if (shiteiNo == null || shiteiNo.isEmpty()) {
			model.addAttribute("showShiteiGassanModal", true);
			model.addAttribute("configForm", new ShoreikinConfigDto());
			return CONFIG_VIEW;
		}

		ShoreikinConfigDto dto = shoreikinConfigService.getShoreikin(shiteiNo, nendo);
		model.addAttribute("configForm", dto);

		return CONFIG_VIEW;
	}

	@PostMapping("/config/edit")
	@OpeLog(screenId = SCREEN_ID, operation = "編集モード切替")
	public String editMode(@ModelAttribute ShoreikinConfigDto configForm, Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		configForm.setMode("edit");
		model.addAttribute("configForm", configForm);

		return CONFIG_VIEW;
	}

	@PostMapping("/config/calculate")
	@OpeLog(screenId = SCREEN_ID, operation = "算出")
	public String calculate(@ModelAttribute ShoreikinConfigDto configForm, Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		try {
			ShoreikinConfigDto result = shoreikinConfigService.calculateShoreikin(configForm);
			model.addAttribute("configForm", result);
		} catch (Exception e) {
			log.error("交付金情報算出エラー", e);
			model.addAttribute("configForm", configForm);
			model.addAttribute("errorMessage", "交付金情報算出に失敗しました: " + e.getMessage());
		}

		return CONFIG_VIEW;
	}

	@PostMapping("/config/create")
	@OpeLog(screenId = SCREEN_ID, operation = "新規登録")
	public String create(@Valid @ModelAttribute ShoreikinConfigDto configForm,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		if (bindingResult.hasErrors()) {
			configForm.setMode("create");
			model.addAttribute("configForm", configForm);
			return CONFIG_VIEW;
		}

		try {
			shoreikinConfigService.createShoreikin(configForm);
			redirectAttributes.addFlashAttribute("successMessage", "交付金情報を登録しました。");
		} catch (Exception e) {
			log.error("交付金登録エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "交付金情報登録に失敗しました: " + e.getMessage());
		}

		return "redirect:/shoreikin/list";
	}

	@PostMapping("/config/update")
	@OpeLog(screenId = SCREEN_ID, operation = "更新")
	public String update(@Valid @ModelAttribute ShoreikinConfigDto configForm,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		if (bindingResult.hasErrors()) {
			configForm.setMode("edit");
			model.addAttribute("configForm", configForm);
			return CONFIG_VIEW;
		}

		try {
			shoreikinConfigService.updateShoreikin(configForm);
			redirectAttributes.addFlashAttribute("successMessage", "交付金情報を更新しました。");
		} catch (Exception e) {
			log.error("交付金更新エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "交付金情報更新に失敗しました: " + e.getMessage());
		}

		return "redirect:/shoreikin/list";
	}
}
