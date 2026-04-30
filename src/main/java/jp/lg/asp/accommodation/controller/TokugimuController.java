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

import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;
import jp.lg.asp.accommodation.service.NozeiShukiService;
import jp.lg.asp.accommodation.service.TokugimuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/tokugimu")
public class TokugimuController {

	private final TokugimuService tokugimuService;
	private final NozeiShukiService nozeiShukiService;

	private static final String LIST_VIEW = "tokugimu/tTokugimuDaicho";
	private static final String FORM_VIEW = "tokugimu/tTokugimuConfig";

	// ========== 一覧・検索 ==========

	@GetMapping("/list")
	public String list(@ModelAttribute TokugimuSearchForm searchForm, Model model) {
		model.addAttribute("items", tokugimuService.search(searchForm));
		model.addAttribute("searchForm", searchForm);
		return LIST_VIEW;
	}

	// ========== 新規登録 ==========

	@GetMapping("/registration")
	public String showRegistrationForm(Model model) {
		model.addAttribute("TokugimuForm", new TokugimuForm());
		model.addAttribute("isEdit", false);
		model.addAttribute("taxCycleOptions", nozeiShukiService.findAll());
		return FORM_VIEW;
	}

	@PostMapping("/registration")
	public String register(
			@Validated @ModelAttribute("TokugimuForm") TokugimuForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (bindingResult.hasErrors()) {
			model.addAttribute("isEdit", false);
			model.addAttribute("taxCycleOptions", nozeiShukiService.findAll());
			return FORM_VIEW;
		}
		try {
			tokugimuService.register(form);
		} catch (Exception e) {
			log.error("登録処理エラー", e);
			model.addAttribute("isEdit", false);
			model.addAttribute("taxCycleOptions", nozeiShukiService.findAll());
			model.addAttribute("errorMessage", e.getMessage());
			return FORM_VIEW;
		}
		redirectAttributes.addFlashAttribute("successMessage", "登録が完了しました。");
		return "redirect:/tokugimu/list";
	}

	// ========== 照会 ==========

	@GetMapping("/view/{id}")
	public String showViewForm(@PathVariable Long id, Model model) {
		model.addAttribute("TokugimuForm", tokugimuService.getTokugimuById(id));
		model.addAttribute("isView", true);
		model.addAttribute("isEdit", false);
		model.addAttribute("editId", id);
		model.addAttribute("taxCycleOptions", nozeiShukiService.findAll());
		return FORM_VIEW;
	}

	// ========== 編集 ==========

	@GetMapping("/edit/{id}")
	public String showEditForm(@PathVariable Long id, Model model) {
		model.addAttribute("TokugimuForm", tokugimuService.getTokugimuById(id));
		model.addAttribute("isView", false);
		model.addAttribute("isEdit", true);
		model.addAttribute("editId", id);
		model.addAttribute("taxCycleOptions", nozeiShukiService.findAll());
		return FORM_VIEW;
	}

	@PostMapping("/edit/{id}")
	public String update(
			@PathVariable Long id,
			@Validated @ModelAttribute("TokugimuForm") TokugimuForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (bindingResult.hasErrors()) {
			model.addAttribute("isEdit", true);
			model.addAttribute("editId", id);
			model.addAttribute("taxCycleOptions", nozeiShukiService.findAll());
			return FORM_VIEW;
		}
		try {
			tokugimuService.update(id, form);
		} catch (Exception e) {
			log.error("更新処理エラー", e);
			model.addAttribute("isEdit", true);
			model.addAttribute("editId", id);
			model.addAttribute("taxCycleOptions", nozeiShukiService.findAll());
			model.addAttribute("errorMessage", e.getMessage());
			return FORM_VIEW;
		}
		redirectAttributes.addFlashAttribute("successMessage", "更新が完了しました。");
		return "redirect:/tokugimu/list";
	}

	// ========== 削除 ==========

	@PostMapping("/delete/{id}")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		tokugimuService.delete(id);
		redirectAttributes.addFlashAttribute("successMessage", "ID:" + id + " のデータを削除しました。");
		return "redirect:/tokugimu/list";
	}
}
