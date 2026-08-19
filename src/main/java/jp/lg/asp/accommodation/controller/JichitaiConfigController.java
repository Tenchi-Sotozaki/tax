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
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.JichitaiConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;
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
	private final JichitaiContext jichitaiContext;

	private static final String SCREEN_ID_CONFIG = ScreenManagement.JICHITAI_CONFIG;

	@GetMapping
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "初期遷移")
	public String index(Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		Jichitai jichitai = jichitaiConfigService.findById(jichitaiCd);
		JichitaiConfigDto form = new JichitaiConfigDto();
		form.setJichitaiCd(jichitai.getJichitaiCd());
		form.setName(jichitai.getName());
		form.setKbnName(jichitai.getKbnName());
		form.setNendoStMonth(jichitai.getNendoStMonth());
		form.setNozeiShuki(jichitai.getNozeiShuki());
		form.setShiteiStChar(jichitai.getShiteiStChar());
		form.setGassanStChar(jichitai.getGassanStChar());
		form.setAtenaStNo(jichitai.getAtenaStNo());
		model.addAttribute("configForm", form);
		model.addAttribute("jichitai", jichitai);
		return "admin/jichitaiConfig";
	}

	@PostMapping("/save")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "登録")
	public String save(@Valid @ModelAttribute("configForm") JichitaiConfigDto configForm,
			BindingResult bindingResult, Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		String currentJichitaiCd = jichitaiContext.getJichitaiCd();
		if (bindingResult.hasErrors()) {
			Jichitai jichitai = jichitaiConfigService.findById(currentJichitaiCd);
			model.addAttribute("jichitai", jichitai);
			model.addAttribute("validationErrors",
					bindingResult.getAllErrors().stream()
							.map(e -> e.getDefaultMessage())
							.toList());
			return "admin/jichitaiConfig";
		}
		try {
			jichitaiConfigService.save(currentJichitaiCd, configForm);
			redirectAttributes.addFlashAttribute("successMessage", "自治体情報を保存しました。");
		} catch (Exception e) {
			log.error("自治体情報保存エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "自治体情報の保存に失敗しました: " + e.getMessage());
		}
		return "redirect:/admin/jichitai-config";
	}
}
