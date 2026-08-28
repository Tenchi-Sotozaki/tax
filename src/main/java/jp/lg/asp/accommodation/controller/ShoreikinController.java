package jp.lg.asp.accommodation.controller;

import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.ShoreikinDto;
import jp.lg.asp.accommodation.service.ShoreikinService;
import jp.lg.asp.accommodation.util.SessionHelper;
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
	@OpeLog(screenId = SCREEN_ID, operation = "一覧表示")
	public String list(@ModelAttribute ShoreikinDto searchForm, Model model,
			@RequestParam(required = false) String searched) {
		accessChecker.checkAccess(SCREEN_ID);
		if (searched != null) {
			model.addAttribute("items", shoreikinService.search(searchForm));
		}
		model.addAttribute("searchForm", searchForm);
		return LIST_VIEW;
	}

	@PostMapping("/search")
	@OpeLog(screenId = SCREEN_ID, operation = "検索")
	public String search(@ModelAttribute ShoreikinDto searchForm, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		model.addAttribute("items", shoreikinService.search(searchForm));
		model.addAttribute("searchForm", searchForm);
		return LIST_VIEW;
	}

	@PostMapping("/viewKofu")
	@OpeLog(screenId = SCREEN_ID, operation = "交付金照会")
	public String viewKofu(@RequestParam List<String> selectedItems,
			@RequestParam(required = false) String shisetsuName,
			@RequestParam(required = false) String shimei,
			@RequestParam(required = false) String nendo,
			HttpSession session,
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

		// DTOに指定番号、施設名称、氏名を保存
		ShiteiGassanSearchDto gassanDto = new ShiteiGassanSearchDto();
		gassanDto.setShiteiNo(selectedItems.get(0));
		gassanDto.setShisetsuName(shisetsuName);
		gassanDto.setName(shimei);
		SessionHelper.saveShiteiGassan(session, gassanDto);
		
		if (nendo != null && !nendo.isEmpty()) {
			redirectAttributes.addAttribute("nendo", nendo);
		}
		return "redirect:/shoreikin/config";
	}

	@PostMapping("/viewKoza")
	@OpeLog(screenId = SCREEN_ID, operation = "口座照会")
	public String viewKoza(@RequestParam List<String> selectedItems,
			@RequestParam(required = false) String shisetsuName,
			@RequestParam(required = false) String shimei,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);
		if (selectedItems.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"口座照会する項目を選択してください。");
			return "redirect:/shoreikin/list";
		}

		// 単一選択のみサポート
		if (selectedItems.size() > 1) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"口座照会は1件ずつ選択してください。");
			return "redirect:/shoreikin/list";
		}

		// DTOに指定番号、施設名称、氏名を保存
		ShiteiGassanSearchDto gassanDto = new ShiteiGassanSearchDto();
		gassanDto.setShiteiNo(selectedItems.get(0));
		gassanDto.setShisetsuName(shisetsuName);
		gassanDto.setName(shimei);
		SessionHelper.saveShiteiGassan(session, gassanDto);

		return "redirect:/shoreikin/furikomiKoza";
	}
}
