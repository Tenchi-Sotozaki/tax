package jp.lg.asp.accommodation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.TokureiTekiyoForm;
import jp.lg.asp.accommodation.service.TokureiTekiyoService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/tekiyo-nozei-shuki")
public class TokureiTekiyoController {

	private final TokureiTekiyoService tokureiTekiyoService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.TEKIYO_NOZEI_SHUKI_CONFIG;
	private static final String LIST_VIEW = "tokugimu/tTokureiTekiyoList";
	private static final String FORM_VIEW = "tokugimu/tTokureiTekiyoConfig";

	// ========== 一覧 ==========

	@GetMapping("/edit")
	@OpeLog(screenId = SCREEN_ID, operation = "一覧表示（メニューから）")
	public String listFromMenu(HttpSession session, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		String shiteiNo = SessionHelper.getShiteiNo(session);
		if (shiteiNo == null) {
			model.addAttribute("showShiteiGassanModal", true);
			model.addAttribute("histories", java.util.List.of());
			return LIST_VIEW;
		}
		return "redirect:/tekiyo-nozei-shuki/list";
	}

	@GetMapping("/list")
	@OpeLog(screenId = SCREEN_ID, operation = "一覧表示")
	public String list(HttpSession session, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		String shiteiNo = SessionHelper.getShiteiNo(session);
		if (shiteiNo == null) {
			model.addAttribute("showShiteiGassanModal", true);
			model.addAttribute("histories", java.util.List.of());
			return LIST_VIEW;
		}
		model.addAttribute("showShiteiGassanModal", false);
		model.addAttribute("histories", tokureiTekiyoService.getHistories());
		return LIST_VIEW;
	}

	// ========== 照会 ==========

	@GetMapping("/view/{rno}")
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String view(@PathVariable Integer rno, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		model.addAttribute("tokureiTekiyoForm", tokureiTekiyoService.getForView(rno));
		model.addAttribute("isView", true);
		model.addAttribute("isEdit", false);
		return FORM_VIEW;
	}

	// ========== 登録 ==========

	@GetMapping("/register")
	@OpeLog(screenId = SCREEN_ID, operation = "登録画面表示")
	public String registerForm(Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		model.addAttribute("tokureiTekiyoForm", tokureiTekiyoService.getForRegister());
		model.addAttribute("isView", false);
		model.addAttribute("isEdit", false);
		return FORM_VIEW;
	}

	@PostMapping("/register")
	@OpeLog(screenId = SCREEN_ID, operation = "登録")
	public String register(@ModelAttribute("tokureiTekiyoForm") TokureiTekiyoForm form,
			Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		try {
			tokureiTekiyoService.save(form);
			redirectAttributes.addFlashAttribute("successMessage", "特例適用を登録しました。");
			return "redirect:/tekiyo-nozei-shuki/list";
		} catch (Exception e) {
			log.error("特例適用登録エラー: {}", e.getMessage());
			model.addAttribute("errorMessage", e.getMessage());
			model.addAttribute("isView", false);
			model.addAttribute("isEdit", false);
			return FORM_VIEW;
		}
	}

	// ========== 編集 ==========

	@GetMapping("/edit/{rno}")
	@OpeLog(screenId = SCREEN_ID, operation = "編集画面表示")
	public String editForm(@PathVariable Integer rno, Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		model.addAttribute("tokureiTekiyoForm", tokureiTekiyoService.getForView(rno));
		model.addAttribute("isView", false);
		model.addAttribute("isEdit", true);
		return FORM_VIEW;
	}

	@PostMapping("/edit/{rno}")
	@OpeLog(screenId = SCREEN_ID, operation = "更新")
	public String edit(@PathVariable Integer rno,
			@ModelAttribute("tokureiTekiyoForm") TokureiTekiyoForm form,
			Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		try {
			tokureiTekiyoService.update(rno, form);
			redirectAttributes.addFlashAttribute("successMessage", "特例適用を更新しました。");
			return "redirect:/tekiyo-nozei-shuki/list";
		} catch (Exception e) {
			log.error("特例適用更新エラー: {}", e.getMessage());
			model.addAttribute("errorMessage", e.getMessage());
			model.addAttribute("isView", false);
			model.addAttribute("isEdit", true);
			return FORM_VIEW;
		}
	}

	// ========== 削除 ==========

	@GetMapping("/delete/{rno}")
	@OpeLog(screenId = SCREEN_ID, operation = "削除")
	public String delete(@PathVariable Integer rno, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		try {
			tokureiTekiyoService.delete(rno);
			redirectAttributes.addFlashAttribute("successMessage", "特例適用を削除しました。");
		} catch (Exception e) {
			log.error("特例適用削除エラー: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/tekiyo-nozei-shuki/list";
	}
}
