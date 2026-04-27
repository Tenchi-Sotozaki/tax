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

	/**
	 * GET /tax-manager/edit/{id}
	 * 特別徴収義務者IDを受け取り、義務者名・施設名をフォームにセットして画面表示。
	 */
	@GetMapping("/edit/{id}")
	public String showForm(@PathVariable Long id, Model model) {
		// 特別徴収義務者情報をServiceから取得（現在はダミー）
		var collectorForm = collectorService.getTokugimuById(id);

		TaxManagerForm form = new TaxManagerForm();
		form.setCollectorId(id);
		form.setObligorName(collectorForm.getName());
		// TODO: DB実装後は施設名を正しく取得する
		form.setFacilityName("グランドホテル東京本館（ID:" + id + "）");

		model.addAttribute("taxManagerForm", form);
		return "collector/tax-manager-registration";
	}

	/**
	 * POST /tax-manager/edit/{id}
	 * 納税管理人情報を登録・更新する。
	 */
	@PostMapping("/edit/{id}")
	public String save(
			@PathVariable Long id,
			@Validated @ModelAttribute("taxManagerForm") TaxManagerForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (bindingResult.hasErrors()) {
			return "collector/tax-manager-registration";
		}

		// TODO: DB登録処理
		log.info("納税管理人登録: collectorId={}, manager={}", id, form.getManagerName());
		redirectAttributes.addFlashAttribute("successMessage", "納税管理人情報を登録しました。");
		return "redirect:/collector/list";
	}
}
