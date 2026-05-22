package jp.lg.asp.accommodation.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;
import jp.lg.asp.accommodation.service.ShoreikinConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
	public String config(@RequestParam String shiteiNos,
			@RequestParam(required = false) String nendo,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		List<String> shiteiNoList = Arrays.asList(shiteiNos.split(","));

		if (shiteiNoList.size() == 1) {
			// 単一選択の場合は詳細画面
			String shiteiNo = shiteiNoList.get(0);
			ShoreikinConfigDto dto = shoreikinConfigService.getShoreikin(shiteiNo, nendo);
			model.addAttribute("configForm", dto);
			model.addAttribute("singleMode", true);
		} else {
			// 複数選択の場合は一覧画面
			List<ShoreikinConfigDto> dtoList = shoreikinConfigService.getShoreikinList(shiteiNoList, nendo);
			model.addAttribute("configList", dtoList);
			model.addAttribute("singleMode", false);
			model.addAttribute("nendo", nendo);
		}

		return CONFIG_VIEW;
	}

	@PostMapping("/config/edit")
	public String editMode(@ModelAttribute ShoreikinConfigDto configForm, Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		configForm.setMode("edit");
		model.addAttribute("configForm", configForm);
		model.addAttribute("singleMode", true);

		return CONFIG_VIEW;
	}

	@PostMapping("/config/calculate")
	public String calculate(@ModelAttribute ShoreikinConfigDto configForm, Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		try {
			ShoreikinConfigDto result = shoreikinConfigService.calculateShoreikin(configForm);
			model.addAttribute("configForm", result);
			model.addAttribute("singleMode", true);
			model.addAttribute("successMessage", "交付金を算出しました。");
		} catch (Exception e) {
			log.error("交付金算出エラー", e);
			model.addAttribute("configForm", configForm);
			model.addAttribute("singleMode", true);
			model.addAttribute("errorMessage", "交付金算出に失敗しました: " + e.getMessage());
		}

		return CONFIG_VIEW;
	}

	@PostMapping("/config/create")
	public String create(@ModelAttribute ShoreikinConfigDto configForm,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);

		try {
			shoreikinConfigService.createShoreikin(configForm);
			redirectAttributes.addFlashAttribute("successMessage", "交付金情報を登録しました。");
		} catch (Exception e) {
			log.error("交付金登録エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "交付金登録に失敗しました: " + e.getMessage());
		}

		return "redirect:/shoreikin/list";
	}

	@PostMapping("/config/update")
	public String update(@ModelAttribute ShoreikinConfigDto configForm,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);

		try {
			shoreikinConfigService.updateShoreikin(configForm);
			redirectAttributes.addFlashAttribute("successMessage", "交付金情報を更新しました。");
		} catch (Exception e) {
			log.error("交付金更新エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "交付金更新に失敗しました: " + e.getMessage());
		}

		return "redirect:/shoreikin/list";
	}
}