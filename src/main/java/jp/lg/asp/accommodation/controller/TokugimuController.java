package jp.lg.asp.accommodation.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import jp.lg.asp.accommodation.config.SelectedJigyoshaResolver;
import jp.lg.asp.accommodation.config.SelectedJigyoshaResolver.Kind;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuListItem;
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
	private final ScreenAccessChecker accessChecker;
	private final SelectedJigyoshaResolver selectedJigyoshaResolver;

	private static final String TOKUGIMU_DAICHO = ScreenManagement.TOKUGIMU_DAICHO;
	private static final String TOKUGIMU_CONFIG = ScreenManagement.TOKUGIMU_CONFIG;
	private static final String LIST_VIEW = "tokugimu/tTokugimuDaicho";
	private static final String FORM_VIEW = "tokugimu/tTokugimuConfig";
	private static final String REPORT_VIEW = "tokugimu/tTokugimuReport";
	/** 特別徴収義務者が未選択の場合に表示する選択画面 */
	private static final String SELECT_VIEW = "tokugimu/shiteiGassanSelect";

	// ========== 一覧・検索 ==========

	@GetMapping("/list")
	@OpeLog(screenId = TOKUGIMU_DAICHO, operation = "一覧表示")
	public String list(@ModelAttribute TokugimuSearchForm searchForm,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int pageSize,
			@RequestParam(defaultValue = "false") boolean searched,
			Model model) {
		accessChecker.checkAccess(TOKUGIMU_DAICHO);
		searchForm.setPage(page);
		searchForm.setPageSize(pageSize);

		// 初期表示時は検索結果一覧を表示しない
		Page<TokugimuListItem> pageResult = searched
				? tokugimuService.search(searchForm)
				: Page.empty(PageRequest.of(page, pageSize));

		model.addAttribute("items", pageResult);
		model.addAttribute("searchForm", searchForm);
		model.addAttribute("isSearched", searched);

		// 選択中のページを中央に固定するため、前後1ページ分の範囲を算出する
		int currentPage = pageResult.getNumber();
		int totalPages = pageResult.getTotalPages();
		model.addAttribute("startPage", Math.max(0, currentPage - 1));
		model.addAttribute("endPage", Math.min(Math.max(totalPages - 1, 0), currentPage + 1));
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

	@GetMapping("/view/{id}")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "照会")
	public String showView(@PathVariable("id") String id,
			@RequestParam(required = false) Integer rno,
			HttpSession session,
			Model model) {
		accessChecker.checkAccess(TOKUGIMU_CONFIG);
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

	@GetMapping("/edit/{id}")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "編集画面表示")
	public String showEditForm(@PathVariable("id") String id, HttpSession session, Model model) {
		accessChecker.checkWriteAccess(TOKUGIMU_CONFIG);
		TokugimuForm form = tokugimuService.getTokugimuByShiteiNo(id);
		storeSelectedShiteiGassan(session, id, form);
		model.addAttribute("TokugimuForm", form);
		model.addAttribute("isView", false);
		model.addAttribute("isEdit", true);
		model.addAttribute("editId", id);
		return FORM_VIEW;
	}

	/**
	 * 表示中の特別徴収義務者をセッションに保持する。
	 * 納税管理人照会・納入期限特例照会・納入申告管理・帳票発行は
	 * 指定番号をセッションから取得するため、照会・編集画面を開いた時点で選択状態を更新しておく。
	 * すでに同一の指定番号が選択済みの場合は、合算指定番号の選択状態を維持するため上書きしない。
	 */
	private void storeSelectedShiteiGassan(HttpSession session, String shiteiNo, TokugimuForm form) {
		ShiteiGassanSearchDto selected = (ShiteiGassanSearchDto) session
				.getAttribute(ShiteiGassanSearchApiController.SESSION_KEY);
		if (selected != null && shiteiNo.equals(selected.getShiteiNo())) {
			return;
		}
		session.setAttribute(ShiteiGassanSearchApiController.SESSION_KEY,
				new ShiteiGassanSearchDto(
						form.getAtenaNo() != null ? String.valueOf(form.getAtenaNo()) : null,
						shiteiNo,
						null,
						form.getName(),
						form.getFacilityName()));
	}

	// ========== 編集（更新） ==========

	@PostMapping("/edit/{id}")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "編集")
	public String update(
			@PathVariable("id") String id,
			@Validated @ModelAttribute("TokugimuForm") TokugimuForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(TOKUGIMU_CONFIG);

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

	/**
	 * サイドメニューからの遷移用。
	 * 指定番号はセッションで選択中の特別徴収義務者から取得する。
	 */
	@GetMapping("/report")
	public String showReportFromSession(HttpSession session, Model model) {
		// 事業者のセッションを保持していない場合は指定モーダルで選択させる（種別はどちらでも可）
		String shiteiNo = selectedJigyoshaResolver.resolveShiteiNo(session, Kind.ANY);
		if (shiteiNo == null) {
			model.addAttribute("targetName", "帳票発行");
			return SELECT_VIEW;
		}
		return "redirect:/tokugimu/report/" + shiteiNo;
	}

	@GetMapping("/report/{id}")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "帳票出力")
	public String showReport(@PathVariable("id") String id, Model model) {
		accessChecker.checkAccess(ScreenManagement.TOKUGIMU_REPORT);
		TokugimuForm form = tokugimuService.getTokugimuByShiteiNo(id);
		model.addAttribute("shiteiNo", id);
		model.addAttribute("tokugimuName", form.getName());
		model.addAttribute("shisetsuName", form.getFacilityName());
		// 合算指定番号がある場合は追加
		// model.addAttribute("gassanShiteiNo", form.getGassanShiteiNo()); // 合算関連フィールドが存在する場合
		return REPORT_VIEW;
	}

	@GetMapping("/report/{id}/gassan")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "合算申告納入承認通知書")
	public String showGassanReport(@PathVariable("id") String id, HttpSession session,
			Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(ScreenManagement.TOKUGIMU_REPORT);
		ShiteiGassanSearchDto selected = (ShiteiGassanSearchDto) session.getAttribute(ShiteiGassanSearchApiController.SESSION_KEY);
		if (selected == null || selected.getGassanShiteiNo() == null || selected.getGassanShiteiNo().isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "合算対象外の特別徴収義務者です");
			return "redirect:/tokugimu/report/" + id;
		}
		return "redirect:/reports/gassanNonyuTsuchi?shiteiNo=" + id;
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
}
