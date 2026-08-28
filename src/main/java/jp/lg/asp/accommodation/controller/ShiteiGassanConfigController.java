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
import jp.lg.asp.accommodation.dto.ShiteiGassanConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.service.ShiteiGassanConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/shitei-gassan")
public class ShiteiGassanConfigController {

	private final ShiteiGassanConfigService shiteiGassanConfigService;
	private final ScreenAccessChecker accessChecker;
	private final JichitaiContext jichitaiContext;

	private static final String SCREEN_ID_CONFIG = ScreenManagement.SHITEI_GASSAN_CONFIG;
	private static final String SCREEN_ID = ScreenManagement.SHITEI_GASSAN;
	private static final String VIEW = "admin/shiteiGassanConfig";

	@GetMapping("/register")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "登録")
	public String register(Model model, RedirectAttributes redirectAttributes) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		Jichitai jichitai = shiteiGassanConfigService.findById(jichitaiCd);
		if (jichitai != null && (jichitai.getShiteiStChar() != null || jichitai.getGassanStChar() != null)) {
			redirectAttributes.addFlashAttribute("infoMessage", "既に登録されています。照会画面に遷移しました。");
			return "redirect:/admin/shitei-gassan/view";
		}
		model.addAttribute("configDto", new ShiteiGassanConfigDto());
		model.addAttribute("mode", "register");
		return VIEW;
	}

	@GetMapping("/view")
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String view(Model model, RedirectAttributes redirectAttributes) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(SCREEN_ID);
		Jichitai jichitai = shiteiGassanConfigService.findById(jichitaiCd);
		if (jichitai.getShiteiStChar() == null && jichitai.getGassanStChar() == null) {
			redirectAttributes.addFlashAttribute("infoMessage", "登録された情報がありません。登録画面に遷移しました。");
			return "redirect:/admin/shitei-gassan/register";
		}
		model.addAttribute("configDto", toDto(jichitai));
		model.addAttribute("mode", "view");
		return VIEW;
	}

	@GetMapping("/edit")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集")
	public String edit(Model model, RedirectAttributes redirectAttributes) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		Jichitai jichitai = shiteiGassanConfigService.findById(jichitaiCd);
		model.addAttribute("configDto", toDto(jichitai));
		model.addAttribute("mode", "edit");
		return VIEW;
	}

	@PostMapping("/save")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "登録・更新")
	public String save(@Valid @ModelAttribute("configDto") ShiteiGassanConfigDto dto,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		if (bindingResult.hasErrors()) {
			Jichitai jichitai = shiteiGassanConfigService.findById(jichitaiCd);
			boolean isNew = jichitai == null || (jichitai.getShiteiStChar() == null && jichitai.getGassanStChar() == null);
			model.addAttribute("mode", isNew ? "register" : "edit");
			model.addAttribute("validationErrors", ShiteiGassanConfigDto.validate(dto).values());
			return VIEW;
		}
		var errors = ShiteiGassanConfigDto.validate(dto);
		if (!errors.isEmpty()) {
			Jichitai jichitai = shiteiGassanConfigService.findById(jichitaiCd);
			boolean isNew = jichitai == null || (jichitai.getShiteiStChar() == null && jichitai.getGassanStChar() == null);
			model.addAttribute("mode", isNew ? "register" : "edit");
			model.addAttribute("validationErrors", errors.values());
			return VIEW;
		}
		Jichitai jichitai = shiteiGassanConfigService.findById(jichitaiCd);
		boolean isNew = jichitai == null || (jichitai.getShiteiStChar() == null && jichitai.getGassanStChar() == null);
		shiteiGassanConfigService.save(jichitaiCd, dto);
		log.debug("指定番号・合算指定番号を{}しました。jichitaiCd: {}", isNew ? "登録" : "更新", jichitaiCd);
		redirectAttributes.addFlashAttribute("successMessage",
				"指定番号・合算指定番号を" + (isNew ? "登録" : "更新") + "しました。");
		return "redirect:/admin/shitei-gassan/view";
	}

	private ShiteiGassanConfigDto toDto(Jichitai jichitai) {
		ShiteiGassanConfigDto dto = new ShiteiGassanConfigDto();
		dto.setShiteiStChar(jichitai.getShiteiStChar());
		dto.setGassanStChar(jichitai.getGassanStChar());
		dto.setVersion(jichitai.getVersion());
		return dto;
	}
}
