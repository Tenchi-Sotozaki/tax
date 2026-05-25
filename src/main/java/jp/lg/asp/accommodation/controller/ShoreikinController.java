package jp.lg.asp.accommodation.controller;

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
import jp.lg.asp.accommodation.dto.ShoreikinDto;
import jp.lg.asp.accommodation.service.ShoreikinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/shoreikin")
public class ShoreikinController {

	private final ShoreikinService shoreikinService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.SHOREIKIN;
	private static final String LIST_VIEW = "shoreikin/shoreikin";

	@GetMapping("/list")
	public String list(@ModelAttribute ShoreikinDto searchForm, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		model.addAttribute("items", shoreikinService.search(searchForm));
		model.addAttribute("searchForm", searchForm);
		return LIST_VIEW;
	}

	@PostMapping("/search")
	public String search(@ModelAttribute ShoreikinDto searchForm, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		model.addAttribute("items", shoreikinService.search(searchForm));
		model.addAttribute("searchForm", searchForm);
		return LIST_VIEW;
	}

	@PostMapping("/bulkCalculate")
	public String bulkCalculate(@ModelAttribute ShoreikinDto searchForm,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);
		// 一括算出画面に遷移
		redirectAttributes.addAttribute("nendo", searchForm.getNendo());
		return "redirect:/shoreikin/bulk";
	}

	@PostMapping("/viewKofu")
	public String viewKofu(@RequestParam List<String> selectedItems,
			@RequestParam(required = false) String nendo,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);
		if (selectedItems.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"交付金照会する項目を選択してください。");
			return "redirect:/shoreikin/list";
		}

		// 単一選択のみサポート
		if (selectedItems.size() > 1) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"交付金照会は1件ずつ選択してください。");
			return "redirect:/shoreikin/list";
		}

		// 交付金照会画面への遷移（単一指定番号）
		redirectAttributes.addAttribute("shiteiNo", selectedItems.get(0));
		if (nendo != null && !nendo.isEmpty()) {
			redirectAttributes.addAttribute("nendo", nendo);
		}
		return "redirect:/shoreikin/config";
	}

	@PostMapping("/viewKoza")
	public String viewKoza(@RequestParam List<String> selectedItems,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);
		if (selectedItems.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"口座照会する項目を選択してください。");
			return "redirect:/shoreikin/list";
		}
		// 振込先口座照会画面への遷移（実装は別途）
		redirectAttributes.addAttribute("shiteiNos", String.join(",", selectedItems));
		return "redirect:/shoreikin/kozaDetail";
	}
}
