package jp.lg.asp.accommodation.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.util.SessionHelper;

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

	@GetMapping("/register")
	@OpeLog(screenId = SCREEN_ID, operation = "登録画面表示")
	public String register(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		String shiteiNo = SessionHelper.getShiteiNo(session);
		if (shiteiNo == null) {
			model.addAttribute("taxManagerForm", new TaxManagerForm());
			model.addAttribute("showShiteiModal", true);
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", false);
			return FORM_VIEW;
		}
		TaxManagerForm form = taxManagerService.getByShiteiNo(shiteiNo);

		// 登録済みの場合は照会画面にリダイレクト
		if (form.isEdit()) {
			redirectAttributes.addFlashAttribute("infoMessage", "納税管理人が登録済みのため、照会画面に遷移しました。");
			return "redirect:/tax-manager/view";
		}

		model.addAttribute("taxManagerForm", form);
		model.addAttribute("isEdit", false);
		model.addAttribute("isView", false);
		return FORM_VIEW;
	}

	@GetMapping("/edit")
	@OpeLog(screenId = SCREEN_ID, operation = "編集画面表示")
	public String edit(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		String shiteiNo = SessionHelper.getShiteiNo(session);
		if (shiteiNo == null) {
			model.addAttribute("taxManagerForm", new TaxManagerForm());
			model.addAttribute("showShiteiModal", true);
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", false);
			return FORM_VIEW;
		}
		TaxManagerForm form = taxManagerService.getByShiteiNo(shiteiNo);

		// 未登録の場合は登録画面にリダイレクト
		if (!form.isEdit()) {
			redirectAttributes.addFlashAttribute("infoMessage", "納税管理人が未登録のため、登録画面に遷移しました。");
			return "redirect:/tax-manager/register";
		}

		model.addAttribute("taxManagerForm", form);
		model.addAttribute("isEdit", true);
		model.addAttribute("isView", false);
		return FORM_VIEW;
	}

	@GetMapping("/view")
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String view(@RequestParam(required = false) String from,
			@RequestParam(required = false) Integer rno, Model model, HttpSession session,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);
		String shiteiNo = SessionHelper.getShiteiNo(session);
		if (shiteiNo == null) {
			model.addAttribute("taxManagerForm", new TaxManagerForm());
			model.addAttribute("showShiteiModal", true);
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", true);
			return FORM_VIEW;
		}
		TaxManagerForm form = (rno != null)
				? taxManagerService.getByShiteiNoAndRno(shiteiNo, rno)
				: taxManagerService.getByShiteiNo(shiteiNo);

		// 未登録の場合は登録画面にリダイレクト
		if (!form.isEdit()) {
			redirectAttributes.addFlashAttribute("infoMessage", "納税管理人が未登録のため、登録画面に遷移しました。");
			return "redirect:/tax-manager/register";
		}

		model.addAttribute("taxManagerForm", form);
		model.addAttribute("isEdit", false);
		model.addAttribute("isView", true);
		return FORM_VIEW;
	}

	@PostMapping("/save")
	@OpeLog(screenId = SCREEN_ID, operation = "登録")
	public String save(
			@Validated @ModelAttribute("taxManagerForm") TaxManagerForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes,
			HttpSession session) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		String shiteiNo = SessionHelper.getShiteiNo(session);
		if (shiteiNo == null) {
			model.addAttribute("taxManagerForm", new TaxManagerForm());
			model.addAttribute("showShiteiModal", true);
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", false);
			return FORM_VIEW;
		}

		log.debug("納税管理人保存処理: shiteiNo={}, atenaNo={}, managerName={}, kbn={}", 
				shiteiNo, form.getAtenaNo(), form.getManagerName(), form.getKbn());

		if (bindingResult.hasErrors()) {
			model.addAttribute("isEdit", form.isEdit());
			model.addAttribute("isView", false);
			model.addAttribute("validationErrors", TaxManagerForm.TaxManagerValidator.validate(form).values());
			return FORM_VIEW;
		}

		try {
			taxManagerService.saveByShiteiNo(shiteiNo, form);
			log.debug("納税管理人情報を保存しました。shiteiNo: {}", shiteiNo);
			redirectAttributes.addFlashAttribute("successMessage", "納税管理人情報を保存しました。");
			return "redirect:/tokugimu/list";
		} catch (Exception e) {
			log.error("納税管理人登録エラー: {}", e.getMessage());
			model.addAttribute("errorMessage", e.getMessage());
			model.addAttribute("isEdit", form.isEdit());
			model.addAttribute("isView", false);
			return FORM_VIEW;
		}
	}

	@PostMapping("/delete")
	public String delete(RedirectAttributes redirectAttributes, HttpSession session) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		String shiteiNo = SessionHelper.getShiteiNo(session);
		if (shiteiNo == null) {
			return "redirect:/tax-manager/edit";
		}

		log.debug("納税管理人削除処理: shiteiNo={}", shiteiNo);

		try {
			taxManagerService.deleteByShiteiNo(shiteiNo);
			log.debug("納税管理人を削除しました。shiteiNo: {}", shiteiNo);
			redirectAttributes.addFlashAttribute("successMessage", "納税管理人を削除しました。");
			return "redirect:/tokugimu/list";
		} catch (Exception e) {
			log.error("納税管理人削除エラー: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/tax-manager/edit";
		}
	}
}
