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

import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.service.TaxManagerService; // ★ TokugimuService ではなくこちらをインポート
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/tax-manager")
public class TaxManagerController {

	private final TaxManagerService taxManagerService;

	private static final String FORM_VIEW = "collector/tTaxManagerConfig";

	@GetMapping("/edit/{id}")
	public String showForm(@PathVariable Long id, Model model) {
		
		TaxManagerForm form = taxManagerService.getById(id);

		model.addAttribute("taxManagerForm", form);
		model.addAttribute("isEdit", true);   
		model.addAttribute("isView", false);  
		return FORM_VIEW;
	}

	@PostMapping("/save/{id}")
	public String save(
			@PathVariable Long id,
			@Validated @ModelAttribute("taxManagerForm") TaxManagerForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (bindingResult.hasErrors()) {
			model.addAttribute("isEdit", true);   
			model.addAttribute("isView", false);  
			return FORM_VIEW;
		}

		taxManagerService.save(id, form);

		log.info("納税管理人登録: collectorId={}, manager={}", id, form.getManagerName());
		redirectAttributes.addFlashAttribute("successMessage", "納税管理人情報を保存しました。");
		return "redirect:/collector/list";
	}
}