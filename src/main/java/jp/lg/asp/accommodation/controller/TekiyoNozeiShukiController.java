package jp.lg.asp.accommodation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.config.SelectedJigyoshaResolver;
import jp.lg.asp.accommodation.config.SelectedJigyoshaResolver.Kind;
import jp.lg.asp.accommodation.dto.TekiyoNozeiShukiForm;
import jp.lg.asp.accommodation.service.TekiyoNozeiShukiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/tekiyo-nozei-shuki")
public class TekiyoNozeiShukiController {

	private final TekiyoNozeiShukiService tekiyoNozeiShukiService;
	private final ScreenAccessChecker accessChecker;
	private final SelectedJigyoshaResolver selectedJigyoshaResolver;

	private static final String SCREEN_ID = ScreenManagement.TEKIYO_NOZEI_SHUKI_CONFIG;
	private static final String FORM_VIEW = "tokugimu/tTekiyoNozeiShukiConfig";
	/** 特別徴収義務者が未選択の場合に表示する選択画面 */
	private static final String SELECT_VIEW = "tokugimu/shiteiGassanSelect";

	/**
	 * サイドメニューからの遷移用。
	 * 指定番号はセッションで選択中の特別徴収義務者から取得する。
	 */
	@GetMapping("/register")
	public String registerFromSession(HttpSession session, Model model) {
		// 特別徴収義務者のセッションを保持していない場合は指定モーダルで選択させる
		String shiteiNo = selectedJigyoshaResolver.resolveShiteiNo(session, Kind.TOKUGIMU);
		if (shiteiNo == null) {
			model.addAttribute("targetName", "納入期限特例登録");
			return SELECT_VIEW;
		}
		return "redirect:/tekiyo-nozei-shuki/edit/" + shiteiNo + "?from=register";
	}

	/**
	 * サイドメニューからの遷移用。
	 * 指定番号はセッションで選択中の特別徴収義務者から取得する。
	 */
	@GetMapping("/view")
	public String viewFromSession(HttpSession session, Model model) {
		// 特別徴収義務者のセッションを保持していない場合は指定モーダルで選択させる
		String shiteiNo = selectedJigyoshaResolver.resolveShiteiNo(session, Kind.TOKUGIMU);
		if (shiteiNo == null) {
			model.addAttribute("targetName", "納入期限特例照会");
			return SELECT_VIEW;
		}
		return "redirect:/tekiyo-nozei-shuki/view/" + shiteiNo;
	}

	@GetMapping("/edit/{id}")
	@OpeLog(screenId = SCREEN_ID, operation = "編集画面表示")
	public String edit(@PathVariable("id") String id, @RequestParam(required = false) String from, Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		TekiyoNozeiShukiForm form = tekiyoNozeiShukiService.getByShiteiNo(id);

		if (form.isEdit() && "register".equals(from)) {
			return "redirect:/tekiyo-nozei-shuki/view/" + id + "?from=register";
		}

		model.addAttribute("tekiyoNozeiShukiForm", form);
		model.addAttribute("nozeiShukiOptions", tekiyoNozeiShukiService.getNozeiShukiOptions());
		model.addAttribute("isEdit", form.isEdit());
		model.addAttribute("isView", false);
		return FORM_VIEW;
	}

	@GetMapping("/view/{id}")
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String view(@PathVariable("id") String id, @RequestParam(required = false) String from, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		TekiyoNozeiShukiForm form = tekiyoNozeiShukiService.getByShiteiNo(id);

		if ("register".equals(from)) {
			model.addAttribute("infoMessage", "この特別徴収義務者には既に適用納税周期が登録されています。");
		}

		model.addAttribute("tekiyoNozeiShukiForm", form);
		model.addAttribute("nozeiShukiOptions", tekiyoNozeiShukiService.getNozeiShukiOptions());
		model.addAttribute("isEdit", false);
		model.addAttribute("isView", true);
		return FORM_VIEW;
	}

	@PostMapping("/save/{id}")
	@OpeLog(screenId = SCREEN_ID, operation = "登録・更新")
	public String save(@PathVariable("id") String id,
			@ModelAttribute("tekiyoNozeiShukiForm") TekiyoNozeiShukiForm form,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		try {
			tekiyoNozeiShukiService.save(id, form);
			redirectAttributes.addFlashAttribute("successMessage", "適用納税周期情報を保存しました。");
			return "redirect:/tokugimu/list";
		} catch (Exception e) {
			log.error("適用納税周期保存エラー: {}", e.getMessage());
			model.addAttribute("errorMessage", e.getMessage());
			model.addAttribute("nozeiShukiOptions", tekiyoNozeiShukiService.getNozeiShukiOptions());
			model.addAttribute("isEdit", form.isEdit());
			model.addAttribute("isView", false);
			return FORM_VIEW;
		}
	}

	@GetMapping("/delete/{id}")
	@OpeLog(screenId = SCREEN_ID, operation = "削除")
	public String delete(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		try {
			tekiyoNozeiShukiService.delete(id);
			redirectAttributes.addFlashAttribute("successMessage", "適用納税周期情報を削除しました。");
		} catch (Exception e) {
			log.error("適用納税周期削除エラー: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/tokugimu/list";
	}
}
