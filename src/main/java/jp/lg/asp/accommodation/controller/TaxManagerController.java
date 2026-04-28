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
import jp.lg.asp.accommodation.service.TokugimuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/tax-manager")
public class TaxManagerController {

	private final TokugimuService collectorService;
	private static final String FORM_VIEW = "collector/tTaxManagerConfig";

	@GetMapping("/edit/{id}")
	public String showForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
		try {
			// ★IDが存在しない場合はここでエラー（Exception）が発生する
			var collectorForm = collectorService.getTokugimuById(id);

			TaxManagerForm form = new TaxManagerForm();
			form.setCollectorId(id);
			form.setObligorName(collectorForm.getName());
			form.setFacilityName("グランドホテル東京本館（ID:" + id + "）");

			model.addAttribute("taxManagerForm", form);
			model.addAttribute("isEdit", true);   
			model.addAttribute("isView", false);  
			return FORM_VIEW;

		} catch (Exception e) {
			// ★安全装置：見つからなかったらエラーメッセージを出して一覧に戻す
			log.error("義務者が見つかりません。ID: {}", id);
			redirectAttributes.addFlashAttribute("errorMessage", "指定された特別徴収義務者（ID: " + id + "）が見つかりません。");
			return "redirect:/collector/list";
		}
	}

	@PostMapping("/edit/{id}")
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

		log.info("納税管理人登録: collectorId={}, manager={}", id, form.getManagerName());
		redirectAttributes.addFlashAttribute("successMessage", "納税管理人情報を登録しました。");
		return "redirect:/collector/list"; 
	}
}