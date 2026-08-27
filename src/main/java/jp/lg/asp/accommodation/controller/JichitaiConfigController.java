package jp.lg.asp.accommodation.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.JichitaiConfigDto;
import jp.lg.asp.accommodation.service.JichitaiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/jichitai-config")
public class JichitaiConfigController {

	private final JichitaiConfigService jichitaiConfigService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID_CONFIG = ScreenManagement.JICHITAI_CONFIG;

	@GetMapping
	public String index(Model model) {

	    model.addAttribute(
	        "configForm",
	        jichitaiConfigService.getJichitaiConfigDto());

	    return "admin/JichitaiConfig";
	}

	@PostMapping("/save")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "登録")
	public String save(@Valid @ModelAttribute("configForm") JichitaiConfigDto configForm,
			BindingResult bindingResult, Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		if (bindingResult.hasErrors()) {
		    model.addAttribute("configForm", configForm);

		    model.addAttribute("validationErrors",
		            bindingResult.getAllErrors().stream()
		                    .map(e -> e.getDefaultMessage())
		                    .toList());

		    return "admin/jichitaiConfig";
		}

		try {
			jichitaiConfigService.saveJichitaiConfig(configForm);
			redirectAttributes.addFlashAttribute("successMessage", "自治体情報を保存しました。");
		} catch (Exception e) {
			log.error("自治体情報保存エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "自治体情報の保存に失敗しました: " + e.getMessage());
		}
		return "redirect:/admin/jichitai-config";
	}
}