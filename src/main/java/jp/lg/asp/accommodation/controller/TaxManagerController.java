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

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.service.TaxManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/tax-manager")
public class TaxManagerController {

	private final TaxManagerService taxManagerService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.TAX_MANAGER;
	private static final String FORM_VIEW = "tokugimu/tTaxManagerConfig";

	@GetMapping("/edit/{id}")
	public String edit(@PathVariable("id") String id, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		TaxManagerForm form = taxManagerService.getByShiteiNo(id);
		model.addAttribute("taxManagerForm", form);
		model.addAttribute("isEdit", form.isEdit());
		model.addAttribute("isView", false);
		return FORM_VIEW;
	}

	@GetMapping("/view/{id}")
	public String view(@PathVariable("id") String id, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		TaxManagerForm form = taxManagerService.getByShiteiNo(id);
		model.addAttribute("taxManagerForm", form);
		model.addAttribute("isEdit", false);
		model.addAttribute("isView", true);
		return FORM_VIEW;
	}

	@PostMapping("/save/{id}")
	public String save(@PathVariable("id") String id,
			@Validated @ModelAttribute("taxManagerForm") TaxManagerForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);

		if (bindingResult.hasErrors()) {
			model.addAttribute("isEdit", form.isEdit());
			model.addAttribute("isView", false);
			return FORM_VIEW;
		}

		taxManagerService.saveByShiteiNo(id, form);

		log.info("納税管理人情報を保存しました。collectorId: {}", id);
		redirectAttributes.addFlashAttribute("successMessage", "納税管理人情報を保存しました。");

		return "redirect:/collector/list";
	}
}
