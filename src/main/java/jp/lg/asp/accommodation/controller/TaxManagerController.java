package jp.lg.asp.accommodation.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
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

	private static final String SCREEN_ID = ScreenManagement.TAXMANAGER_CONFIG;
	private static final String FORM_VIEW = "tokugimu/tTaxManagerConfig";

	/**
	 * 特別徴収義務者との同一人物チェックAPI
	 */
	@PostMapping("/check-atena-duplicate")
	public ResponseEntity<Map<String, Object>> checkAtenaDuplicate(
			@RequestParam String taxManagerAtenaNo,
			@RequestParam String obligorAtenaNo) {
		try {
			String trimmedTaxManagerAtenaNo = taxManagerAtenaNo != null ? taxManagerAtenaNo.trim() : "";
			String trimmedObligorAtenaNo = obligorAtenaNo != null ? obligorAtenaNo.trim() : "";
			log.debug("API同一人物チェック: 納税管理人={}, 特徴={}", trimmedTaxManagerAtenaNo, trimmedObligorAtenaNo);
			
			boolean isDuplicate = taxManagerService.isSamePerson(trimmedTaxManagerAtenaNo, trimmedObligorAtenaNo);
			log.debug("API同一人物チェック結果: {}", isDuplicate);
			
			return ResponseEntity.ok(Map.of(
				"isDuplicate", isDuplicate,
				"message", isDuplicate ? "特別徴収義務者と同一人物のため、納税管理人として登録できません。" : "登録可能です。"
			));
		} catch (Exception e) {
			log.error("宛名番号同一人物チェックエラー", e);
			return ResponseEntity.ok(Map.of(
				"isDuplicate", false,
				"message", "チェック中にエラーが発生しました。"
			));
		}
	}

	@GetMapping("/edit/{id}")
	@OpeLog(screenId = SCREEN_ID, operation = "編集画面表示")
	public String edit(@PathVariable("id") String id, @RequestParam(required = false) String from, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		TaxManagerForm form = taxManagerService.getByShiteiNo(id);
		
		// 既に納税管理人が登録されている場合の処理
		if (form.isEdit()) {
			// 納税管理人登録ボタンからの遷移の場合は照会画面にリダイレクト
			if ("register".equals(from)) {
				return "redirect:/tax-manager/view/" + id + "?from=register";
			}
			// 納税管理人照会ボタンからの遷移の場合は編集画面を表示
			model.addAttribute("taxManagerForm", form);
			model.addAttribute("isEdit", true);
			model.addAttribute("isView", false);
			return FORM_VIEW;
		}
		
		model.addAttribute("taxManagerForm", form);
		model.addAttribute("isEdit", form.isEdit());
		model.addAttribute("isView", false);
		return FORM_VIEW;
	}

	@GetMapping("/view/{id}")
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String view(@PathVariable("id") String id, @RequestParam(required = false) String from, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		TaxManagerForm form = taxManagerService.getByShiteiNo(id);
		
		// 納税管理人登録ボタンからの遷移の場合はメッセージを表示
		if ("register".equals(from)) {
			model.addAttribute("infoMessage", "この特別徴収義務者には既に納税管理人が登録されています。");
		}
		
		model.addAttribute("taxManagerForm", form);
		model.addAttribute("isEdit", false);
		model.addAttribute("isView", true);
		return FORM_VIEW;
	}

	@PostMapping("/save/{id}")
	@OpeLog(screenId = SCREEN_ID, operation = "登録")
	public String save(@PathVariable("id") String id,
			@Validated @ModelAttribute("taxManagerForm") TaxManagerForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);

		// デバッグ情報を出力
		log.info("納税管理人保存処理: shiteiNo={}, atenaNo={}, managerName={}, exemptionFlag={}", 
				id, form.getAtenaNo(), form.getManagerName(), form.isExemptionFlag());

		if (bindingResult.hasErrors()) {
			model.addAttribute("isEdit", form.isEdit());
			model.addAttribute("isView", false);
			model.addAttribute("validationErrors", TaxManagerForm.TaxManagerValidator.validate(form).values());
			return FORM_VIEW;
		}

		try {
			taxManagerService.saveByShiteiNo(id, form);
			log.info("納税管理人情報を保存しました。collectorId: {}", id);
			redirectAttributes.addFlashAttribute("successMessage", "納税管理人情報を保存しました。");
			return "redirect:/tokugimu/list";
		} catch (IllegalArgumentException e) {
			log.warn("納税管理人登録エラー: {}", e.getMessage());
			model.addAttribute("errorMessage", e.getMessage());
			model.addAttribute("isEdit", form.isEdit());
			model.addAttribute("isView", false);
			return FORM_VIEW;
		}
	}

	@PostMapping("/delete/{id}")
	public String delete(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);

		log.info("納税管理人削除処理: shiteiNo={}", id);

		try {
			taxManagerService.deleteByShiteiNo(id);
			log.info("納税管理人を削除しました。shiteiNo: {}", id);
			redirectAttributes.addFlashAttribute("successMessage", "納税管理人を削除しました。");
			return "redirect:/tokugimu/list";
		} catch (IllegalArgumentException e) {
			log.warn("納税管理人削除エラー: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/tax-manager/edit/" + id;
		}
	}
}
