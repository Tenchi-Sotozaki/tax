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

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.FurikomiKozaDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.FurikomiKozaService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 振込先口座照会／登録／編集 Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/shoreikin/furikomiKoza")
public class FurikomiKozaController {

	private final FurikomiKozaService furikomiKozaService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.FURIKOMI_KOZA;
	private static final String KOZA_VIEW = "shoreikin/furikomiKoza";

	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String view(HttpSession session, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		String shiteiNo = SessionHelper.getShiteiNo(session);

		if (shiteiNo == null || shiteiNo.isEmpty()) {
			model.addAttribute("showShiteiGassanModal", true);
			model.addAttribute("kozaForm", new FurikomiKozaDto());
			return KOZA_VIEW;
		}

		FurikomiKozaDto dto = furikomiKozaService.getFurikomiKoza(shiteiNo);
		model.addAttribute("kozaForm", dto);

		return KOZA_VIEW;
	}

	@PostMapping("/edit")
	@OpeLog(screenId = SCREEN_ID, operation = "編集切り替え")
	public String editMode(@ModelAttribute FurikomiKozaDto kozaForm, Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		kozaForm.setMode("edit");
		model.addAttribute("kozaForm", kozaForm);

		return KOZA_VIEW;
	}

	@PostMapping("/create")
	@OpeLog(screenId = SCREEN_ID, operation = "登録")
	public String create(@Valid @ModelAttribute FurikomiKozaDto kozaForm,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		if (bindingResult.hasErrors()) {
			kozaForm.setMode("create");
			model.addAttribute("kozaForm", kozaForm);
			return KOZA_VIEW;
		}

		try {
			furikomiKozaService.createFurikomiKoza(kozaForm);
			redirectAttributes.addFlashAttribute("successMessage", "振込先口座情報を登録しました。");
		} catch (Exception e) {
			log.error("振込先口座登録エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "振込先口座情報登録に失敗しました: " + e.getMessage());
		}

		return "redirect:/shoreikin/list";
	}

	@PostMapping("/update")
	@OpeLog(screenId = SCREEN_ID, operation = "更新")
	public String update(@Valid @ModelAttribute FurikomiKozaDto kozaForm,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		if (bindingResult.hasErrors()) {
			kozaForm.setMode("edit");
			model.addAttribute("kozaForm", kozaForm);
			return KOZA_VIEW;
		}

		try {
			furikomiKozaService.updateFurikomiKoza(kozaForm);
			redirectAttributes.addFlashAttribute("successMessage", "振込先口座情報を更新しました。");
		} catch (Exception e) {
			log.error("振込先口座更新エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "振込先口座更新に失敗しました: " + e.getMessage());
		}

		return "redirect:/shoreikin/list";
	}
}
