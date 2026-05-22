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

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.FurikomiKozaDto;
import jp.lg.asp.accommodation.exception.ResourceNotFoundException;
import jp.lg.asp.accommodation.service.FurikomiKozaService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/shoreikin/furikomiKoza")
@RequiredArgsConstructor
public class FurikomiKozaController {

	private final FurikomiKozaService furikomiKozaService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.FURIKOMI_KOZA;

	/**
	 * 振込先口座照会画面表示
	 */
	@GetMapping
	public String view(@RequestParam String shiteiNo, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		try {
			FurikomiKozaDto dto = furikomiKozaService.getFurikomiKoza(shiteiNo);
			model.addAttribute("furikomiKozaDto", dto);
			return "shoreikin/furikomiKoza";
		} catch (ResourceNotFoundException e) {
			model.addAttribute("errorMessage", e.getMessage());
			return "error";
		}
	}

	/**
	 * 編集画面表示
	 */
	@GetMapping("/edit")
	public String edit(@RequestParam String shiteiNo, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		try {
			FurikomiKozaDto dto = furikomiKozaService.getFurikomiKoza(shiteiNo);
			dto.setMode("edit");
			model.addAttribute("furikomiKozaDto", dto);
			return "shoreikin/furikomiKoza";
		} catch (ResourceNotFoundException e) {
			model.addAttribute("errorMessage", e.getMessage());
			return "error";
		}
	}

	/**
	 * 登録処理
	 */
	@PostMapping("/register")
	public String register(@Valid @ModelAttribute FurikomiKozaDto furikomiKozaDto,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		accessChecker.checkAccess(SCREEN_ID);
		if (bindingResult.hasErrors()) {
			furikomiKozaDto.setMode("register");
			model.addAttribute("furikomiKozaDto", furikomiKozaDto);
			return "shoreikin/furikomiKoza";
		}

		try {
			furikomiKozaService.registerFurikomiKoza(furikomiKozaDto);
			redirectAttributes.addFlashAttribute("successMessage", "振込先口座情報を登録しました");
			return "redirect:/shoreikin";
		} catch (Exception e) {
			model.addAttribute("errorMessage", "登録に失敗しました: " + e.getMessage());
			furikomiKozaDto.setMode("register");
			model.addAttribute("furikomiKozaDto", furikomiKozaDto);
			return "shoreikin/furikomiKoza";
		}
	}

	/**
	 * 更新処理
	 */
	@PostMapping("/update")
	public String update(@Valid @ModelAttribute FurikomiKozaDto furikomiKozaDto,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (bindingResult.hasErrors()) {
			furikomiKozaDto.setMode("edit");
			model.addAttribute("furikomiKozaDto", furikomiKozaDto);
			return "shoreikin/furikomiKoza";
		}

		try {
			furikomiKozaService.updateFurikomiKoza(furikomiKozaDto);
			redirectAttributes.addFlashAttribute("successMessage", "振込先口座情報を更新しました");
			return "redirect:/shoreikin";
		} catch (Exception e) {
			model.addAttribute("errorMessage", "更新に失敗しました: " + e.getMessage());
			furikomiKozaDto.setMode("edit");
			model.addAttribute("furikomiKozaDto", furikomiKozaDto);
			return "shoreikin/furikomiKoza";
		}
	}
}