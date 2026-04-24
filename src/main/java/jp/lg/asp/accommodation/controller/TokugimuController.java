package jp.lg.asp.accommodation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.dto.CollectorSearchForm;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.service.TokugimuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/collector")
public class TokugimuController {

	private final TokugimuService tokugimuService;

	private static final String LIST_VIEW = "collector/tTokugimuDaicho";
	private static final String FORM_VIEW = "collector/tTokugimuConfig";

	// ========== 一覧・検索 ==========

	@GetMapping("/list")
	public String list(@ModelAttribute CollectorSearchForm searchForm, Model model) {
		model.addAttribute("items", tokugimuService.search(searchForm));
		model.addAttribute("searchForm", searchForm);
		return LIST_VIEW;
	}

	// ========== 新規登録 ==========

	@GetMapping("/registration")
	public String showRegistrationForm(Model model) {
		model.addAttribute("collectorForm", new TokugimuForm());
		model.addAttribute("isEdit", false);
		return FORM_VIEW;
	}

	@PostMapping("/registration")
	public String register(
			@Validated @ModelAttribute("collectorForm") TokugimuForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (bindingResult.hasErrors()) {
			model.addAttribute("isEdit", false);
			return FORM_VIEW;
		}
		tokugimuService.register(form);
		redirectAttributes.addFlashAttribute("successMessage", "登録が完了しました。");
		return "redirect:/collector/list";
	}

	// ========== 編集 ==========

	@GetMapping("/edit/{id}")
	public String showEditForm(@PathVariable Long id, Model model) {
		model.addAttribute("collectorForm", tokugimuService.getTokugimuById(id));
		model.addAttribute("isEdit", true);
		model.addAttribute("editId", id);
		return FORM_VIEW;
	}

	@PostMapping("/edit/{id}")
	public String update(
			@PathVariable Long id,
			@Validated @ModelAttribute("tokugimuForm") TokugimuForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (bindingResult.hasErrors()) {
			model.addAttribute("isEdit", true);
			model.addAttribute("editId", id);
			return FORM_VIEW;
		}
		tokugimuService.update(id, form);
		redirectAttributes.addFlashAttribute("successMessage", "更新が完了しました。");
		return "redirect:/collector/list";
	}

	// ========== 削除 ==========

	@PostMapping("/delete/{id}")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		tokugimuService.delete(id);
		redirectAttributes.addFlashAttribute("successMessage", "ID:" + id + " のデータを削除しました。");
		return "redirect:/collector/list";
	}
}
