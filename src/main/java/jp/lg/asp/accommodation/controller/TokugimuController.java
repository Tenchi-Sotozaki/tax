package jp.lg.asp.accommodation.controller;

import org.springframework.data.domain.Page;
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

import jakarta.servlet.http.HttpSession;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuListItem;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;
import jp.lg.asp.accommodation.service.NozeiShukiService;
import jp.lg.asp.accommodation.service.TokugimuService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/tokugimu")
public class TokugimuController {

	private final TokugimuService tokugimuService;
	private final NozeiShukiService nozeiShukiService;
	private final ScreenAccessChecker accessChecker;

	private static final String TOKUGIMU_DAICHO = ScreenManagement.TOKUGIMU_DAICHO;
	private static final String TOKUGIMU_CONFIG = ScreenManagement.TOKUGIMU_CONFIG;
	private static final String LIST_VIEW = "tokugimu/tTokugimuDaicho";
	private static final String FORM_VIEW = "tokugimu/tTokugimuConfig";
	private static final String REPORT_VIEW = "tokugimu/tTokugimuReport";

	// ========== 一覧・検索 ==========

	@GetMapping("/list")
	@OpeLog(screenId = TOKUGIMU_DAICHO, operation = "一覧表示")
	public String list(@ModelAttribute TokugimuSearchForm searchForm,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int pageSize, Model model) {
		accessChecker.checkAccess(TOKUGIMU_DAICHO);
		searchForm.setPage(page);
		searchForm.setPageSize(pageSize);
		Page<TokugimuListItem> pageResult = tokugimuService.search(searchForm);
		model.addAttribute("items", pageResult);
		model.addAttribute("searchForm", searchForm);
		return LIST_VIEW;
	}

	// ========== 新規登録 ==========

	@GetMapping("/registration")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "登録画面表示")
	public String showRegistrationForm(Model model) {
		accessChecker.checkWriteAccess(TOKUGIMU_CONFIG);
		model.addAttribute("TokugimuForm", new TokugimuForm());
		model.addAttribute("isEdit", false);
		return FORM_VIEW;
	}

	@PostMapping("/registration")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "登録")
	public String register(
			@Validated @ModelAttribute("TokugimuForm") TokugimuForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(TOKUGIMU_CONFIG);

		if (bindingResult.hasErrors()) {
			model.addAttribute("isEdit", false);
			model.addAttribute("validationErrors", TokugimuForm.TokugimuValidator.validate(form).values());
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

	@GetMapping("/view")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "照会")
	public String showView(HttpSession session,
			@RequestParam(required = false) Integer rno,
			Model model) {
		accessChecker.checkAccess(TOKUGIMU_CONFIG);
		String id = getShiteiNoFromSession(session);
		if (id == null) {
			model.addAttribute("showShiteiGassanModal", true);
			return LIST_VIEW;
		}
		TokugimuForm form = (rno != null)
				? tokugimuService.getTokugimuByShiteiNoAndRno(id, rno)
				: tokugimuService.getTokugimuByShiteiNo(id);
		storeSelectedShiteiGassan(session, id, form);
		model.addAttribute("TokugimuForm", form);
		model.addAttribute("isView", true);
		model.addAttribute("isEdit", false);
		model.addAttribute("editId", id);
		return FORM_VIEW;
	}

	// ========== 編集 ==========

	@GetMapping("/edit")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "編集画面表示")
	public String showEditForm(HttpSession session, Model model) {
		accessChecker.checkWriteAccess(TOKUGIMU_CONFIG);
		String id = getShiteiNoFromSession(session);
		if (id == null) {
			model.addAttribute("showShiteiGassanModal", true);
			return LIST_VIEW;
		}
		TokugimuForm form = tokugimuService.getTokugimuByShiteiNo(id);
		storeSelectedShiteiGassan(session, id, form);
		model.addAttribute("TokugimuForm", form);
		model.addAttribute("isView", false);
		model.addAttribute("isEdit", true);
		model.addAttribute("editId", id);
		return FORM_VIEW;
	}

	// ========== 編集（更新） ==========

	@PostMapping("/edit")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "編集")
	public String update(
			HttpSession session,
			@Validated @ModelAttribute("TokugimuForm") TokugimuForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(TOKUGIMU_CONFIG);
		String id = getShiteiNoFromSession(session);
		if (id == null) {
			model.addAttribute("showShiteiGassanModal", true);
			return LIST_VIEW;
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute("isEdit", true);
			model.addAttribute("editId", id);
			model.addAttribute("validationErrors", TokugimuForm.TokugimuValidator.validate(form).values());
			return FORM_VIEW;
		}
		try {
			tokugimuService.updateByShiteiNo(id, form);
		} catch (Exception e) {
			log.error("更新処理エラー", e);
			return FORM_VIEW;
		}
		redirectAttributes.addFlashAttribute("successMessage", "更新が完了しました。");
		return "redirect:/tokugimu/list";
	}

	// ========== 帳票出力 ==========

	@GetMapping("/report")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "帳票出力")
	public String showReport(HttpSession session, Model model) {
		accessChecker.checkAccess(ScreenManagement.TOKUGIMU_REPORT);
		String id = getShiteiNoFromSession(session);
		if (id == null) {
			model.addAttribute("showShiteiGassanModal", true);
			return LIST_VIEW;
		}
		TokugimuForm form = tokugimuService.getTokugimuByShiteiNo(id);
		storeSelectedShiteiGassan(session, id, form);
		model.addAttribute("shiteiNo", id);
		model.addAttribute("tokugimuName", form.getName());
		model.addAttribute("shisetsuName", form.getFacilityName());
		return REPORT_VIEW;
	}

	@GetMapping("/report/gassan")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "合算申告納入承認通知書")
	public String showGassanReport(HttpSession session,
			Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(ScreenManagement.TOKUGIMU_REPORT);
		ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
		if (selected == null || selected.getGassanShiteiNo() == null || selected.getGassanShiteiNo().isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "合算対象外の特別徴収義務者です");
			return "redirect:/tokugimu/report";
		}
		return "redirect:/reports/gassanNonyuTsuchi";
	}

	// ========== 削除 ==========

	@PostMapping("/delete/{id}")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "削除")
	public String delete(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(TOKUGIMU_CONFIG);
		tokugimuService.deleteByShiteiNo(id);
		redirectAttributes.addFlashAttribute("successMessage", "指定番号:" + id + " のデータを削除しました。");
		return "redirect:/tokugimu/list";
	}

	// ========== 共通処理 ==========

	private String getShiteiNoFromSession(HttpSession session) {
		return SessionHelper.getShiteiNo(session);
	}

	/**
	 * 表示中の特別徴収義務者をセッションに保持する。
	 * すでに同一の指定番号が選択済みの場合は、合算指定番号の選択状態を維持するため上書きしない。
	 */
	private void storeSelectedShiteiGassan(HttpSession session, String shiteiNo, TokugimuForm form) {
		ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
		if (selected != null && shiteiNo.equals(selected.getShiteiNo())) {
			return;
		}
		SessionHelper.saveShiteiGassan(session,
				new ShiteiGassanSearchDto(
						form.getAtenaNo() != null ? String.valueOf(form.getAtenaNo()) : null,
						shiteiNo,
						null,
						form.getName(),
						form.getFacilityName()));
	}
}
