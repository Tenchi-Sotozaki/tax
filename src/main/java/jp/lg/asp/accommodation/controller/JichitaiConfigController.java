package jp.lg.asp.accommodation.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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

	@GetMapping("/register")
	public String register(Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		if (!model.containsAttribute("configForm")) {
			model.addAttribute("configForm", new JichitaiConfigDto());
		}
		model.addAttribute("mode", "register");
		return "admin/jichitaiConfig";
	}

	@GetMapping("/view/{jichitaiCd}")
	public String view(@PathVariable String jichitaiCd, Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		model.addAttribute("configForm", jichitaiConfigService.getJichitaiConfigDtoById(jichitaiCd));
		model.addAttribute("mode", "view");
		return "admin/jichitaiConfig";
	}

	@GetMapping("/edit/{jichitaiCd}")
	public String edit(@PathVariable String jichitaiCd, Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		if (!model.containsAttribute("configForm")) {
			model.addAttribute("configForm", jichitaiConfigService.getJichitaiConfigDtoById(jichitaiCd));
		}
		model.addAttribute("mode", "edit");
		return "admin/jichitaiConfig";
	}

	@PostMapping("/save")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "登録")
	public String save(@Valid @ModelAttribute("configForm") JichitaiConfigDto configForm,
			BindingResult bindingResult, Model model,
			@org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "register") String mode,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		if (bindingResult.hasErrors()) {
			List<String> fieldOrder = List.of(
					"jichitaiCd", "name", "kbnName", "param",
					"nendoStMonth", "nozeiShuki",
					"shiteiStChar", "gassanStChar", "atenaStNo",
					"userId");
			Map<String, String> fieldErrors = new LinkedHashMap<>();
			for (String field : fieldOrder) {
				FieldError fe = bindingResult.getFieldError(field);
				if (fe != null) {
					fieldErrors.put(field, fe.getDefaultMessage());
				}
			}
			redirectAttributes.addFlashAttribute("configForm", configForm);
			redirectAttributes.addFlashAttribute("fieldErrors", fieldErrors);
			if ("edit".equals(mode)) {
				return "redirect:/admin/jichitai-config/edit/" + configForm.getJichitaiCd();
			}
			return "redirect:/admin/jichitai-config/register";
		}

		try {
			if (configForm.getShiteiStChar().equals(configForm.getGassanStChar())) {
				Map<String, String> fieldErrors = new LinkedHashMap<>();
				fieldErrors.put("shiteiStChar", "指定番号と合算指定番号には異なる値を入力してください。");
				fieldErrors.put("gassanStChar", "指定番号と合算指定番号には異なる値を入力してください。");
				redirectAttributes.addFlashAttribute("configForm", configForm);
				redirectAttributes.addFlashAttribute("fieldErrors", fieldErrors);
				if ("edit".equals(mode)) {
					return "redirect:/admin/jichitai-config/edit/" + configForm.getJichitaiCd();
				}
				return "redirect:/admin/jichitai-config/register";
			}
			jichitaiConfigService.saveJichitaiConfig(configForm);
			String message = "edit".equals(mode) ? "自治体情報を更新しました。" : "自治体情報を登録しました。";
			redirectAttributes.addFlashAttribute("successMessage", message);
		} catch (Exception e) {
			log.error("自治体情報保存エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "自治体情報の保存に失敗しました: " + e.getMessage());
		}
		return "redirect:/admin/jichitai-config/view/" + configForm.getJichitaiCd();
	}
}