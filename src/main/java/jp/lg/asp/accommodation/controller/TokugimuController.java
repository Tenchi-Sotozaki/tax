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
		public String showView(@PathVariable("id") String id, Model model) {
			// Service側も getTokugimuByShiteiNo(id) に差し替える
			model.addAttribute("TokugimuForm", tokugimuService.getTokugimuByShiteiNo(id));
			model.addAttribute("isView", true);
			model.addAttribute("isEdit", false);
			model.addAttribute("editId", id); // JS側で id として扱うならこのままでもOK
			model.addAttribute("taxCycleOptions", nozeiShukiService.findAll());
			return FORM_VIEW;
		}

		// ========== 編集 ==========

		@GetMapping("/edit/{id}")
		public String showEditForm(@PathVariable("id") String id, Model model) {
			model.addAttribute("TokugimuForm", tokugimuService.getTokugimuByShiteiNo(id));
			model.addAttribute("isView", false);
			model.addAttribute("isEdit", true);
			model.addAttribute("editId", id);
			model.addAttribute("taxCycleOptions", nozeiShukiService.findAll());
			return FORM_VIEW;
		}

		// ========== 編集（更新） ==========
		@PostMapping("/edit/{id}")
		public String update(
				@PathVariable("id") String id, // ← ★ Long から String に変更！
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
		
				tokugimuService.updateByShiteiNo(id, form); 
			} catch (Exception e) {
				log.error("更新処理エラー", e);
				// ... 省略 ...
				return FORM_VIEW;
			}
			redirectAttributes.addFlashAttribute("successMessage", "更新が完了しました。");
			return "redirect:/tokugimu/list";
		}

		// ========== 削除 ==========
		@PostMapping("/delete/{id}")
		public String delete(@PathVariable("id") String id, RedirectAttributes redirectAttributes) { // ← ★ Long から String に変更！
			// ★ 古い delete(id) から変更！
			tokugimuService.deleteByShiteiNo(id); 
			redirectAttributes.addFlashAttribute("successMessage", "指定番号:" + id + " のデータを削除しました。");
			return "redirect:/tokugimu/list";
		}
}