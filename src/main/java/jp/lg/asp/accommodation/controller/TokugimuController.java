package jp.lg.asp.accommodation.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.TokugimuForm;
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

	/**
	 * 型変換に失敗した項目の表示メッセージ。
	 * 画面の入力欄はいずれもテキストのため、数値項目で起こりうる。
	 */
	private static final Map<String, String> TYPE_MISMATCH_MESSAGES = Map.of(
			"floorArea", "宿泊施設情報の延床面積は半角数字とピリオドで入力してください",
			"roomCount", "宿泊施設情報の客室数は半角数字で入力してください",
			"capacity", "宿泊施設情報の収容人数は半角数字で入力してください");

	private static final String TYPE_MISMATCH_DEFAULT_MESSAGE = "入力形式が正しくない項目があります";

	private static final String TOKUGIMU_DAICHO = ScreenManagement.TOKUGIMU_DAICHO;
	private static final String TOKUGIMU_CONFIG = ScreenManagement.TOKUGIMU_CONFIG;
	private static final String LIST_VIEW = "tokugimu/tTokugimuDaicho";
	private static final String FORM_VIEW = "tokugimu/tTokugimuConfig";
	private static final String REPORT_VIEW = "tokugimu/tTokugimuReport";

	// ========== 一覧・検索 ==========

	@GetMapping("/list")
	@OpeLog(screenId = TOKUGIMU_DAICHO, operation = "一覧表示")
	public String list(@ModelAttribute TokugimuSearchForm searchForm,
			@RequestParam(defaultValue = "false") boolean searched,
			Model model) {
		accessChecker.checkAccess(TOKUGIMU_DAICHO);

		// 初期表示時は検索結果一覧を表示しない
		java.util.List<jp.lg.asp.accommodation.dto.TokugimuListItem> items = searched
				? tokugimuService.searchAll(searchForm)
				: java.util.List.of();

		model.addAttribute("items", items);
		model.addAttribute("searchForm", searchForm);
		model.addAttribute("isSearched", searched);
		return LIST_VIEW;
	}

	/**
	 * 指定番号が未選択の状態で照会・編集に遷移した場合に、
	 * 遷移先の画面で指定番号選択モーダルを開いた状態で表示する。
	 */
	private String showSelectModalOnForm(Model model) {
		model.addAttribute("TokugimuForm", new TokugimuForm());
		model.addAttribute("isView", true);
		model.addAttribute("isEdit", false);
		model.addAttribute("showShiteiGassanModal", true);
		return FORM_VIEW;
	}

	/**
	 * 指定番号が未選択の状態で帳票発行に遷移した場合に、
	 * 帳票発行画面で指定番号選択モーダルを開いた状態で表示する。
	 */
	private String showSelectModalOnReport(Model model) {
		model.addAttribute("showShiteiGassanModal", true);
		return REPORT_VIEW;
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
			model.addAttribute("validationErrors", buildValidationMessages(bindingResult));
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
			return showSelectModalOnForm(model);
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
			return showSelectModalOnForm(model);
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
			return showSelectModalOnForm(model);
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute("isEdit", true);
			model.addAttribute("editId", id);
			model.addAttribute("validationErrors", buildValidationMessages(bindingResult));
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
		return buildReportView(session, model);
	}

	/**
	 * 合算申告納入承認通知書へ遷移する。
	 * 選択中の指定番号が合算対象でない場合は遷移せず、
	 * 帳票発行画面にエラーメッセージを表示する。
	 */
	@GetMapping("/report/gassan")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "合算申告納入承認通知書")
	public String showGassanReport(HttpSession session, Model model) {
		accessChecker.checkAccess(ScreenManagement.TOKUGIMU_REPORT);
		String id = getShiteiNoFromSession(session);
		if (id != null && tokugimuService.isGassanTarget(id)) {
			return "redirect:/reports/gassanNonyuTsuchi";
		}
		if (id != null) {
			model.addAttribute("errorMessage", "合算対象外の特別徴収義務者です。");
		}
		return buildReportView(session, model);
	}

	/**
	 * 帳票発行画面を表示する。
	 * 指定番号が未選択の場合は指定番号選択モーダルを開いた状態で表示する。
	 */
	private String buildReportView(HttpSession session, Model model) {
		String id = getShiteiNoFromSession(session);
		if (id == null) {
			return showSelectModalOnReport(model);
		}
		TokugimuForm form = tokugimuService.getTokugimuByShiteiNo(id);
		storeSelectedShiteiGassan(session, id, form);
		model.addAttribute("shiteiNo", id);
		model.addAttribute("tokugimuName", form.getName());
		model.addAttribute("shisetsuName", form.getFacilityName());
		return REPORT_VIEW;
	}

	// ========== 削除 ==========

	/**
	 * 削除対象はエンドポイントに含めず、セッションで選択中の特別徴収義務者を対象とする。
	 */
	@PostMapping("/delete")
	@OpeLog(screenId = TOKUGIMU_CONFIG, operation = "削除")
	public String delete(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(TOKUGIMU_CONFIG);
		String id = getShiteiNoFromSession(session);
		if (id == null) {
			return showSelectModalOnForm(model);
		}
		boolean historyRemains = tokugimuService.deleteByShiteiNo(id);
		if (historyRemains) {
			// 履歴が残っている場合は選択状態を維持し、最新履歴の照会画面へ戻る
			redirectAttributes.addFlashAttribute("successMessage", "指定番号:" + id + " の最新履歴を削除しました。");
			return "redirect:/tokugimu/view";
		}
		// すべての履歴が削除された場合は、存在しない特別徴収義務者が選択されたまま残らないよう解除する
		SessionHelper.saveShiteiGassan(session, null);
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

	/**
	 * 画面上部のサマリに出すメッセージを組み立てる。
	 *
	 * 必須チェックと桁数・形式チェックは、各アノテーションに指定した日本語メッセージを
	 * そのまま使う。型変換の失敗だけは英語の既定メッセージになるため、日本語へ差し替える。
	 */
	private List<String> buildValidationMessages(BindingResult bindingResult) {
		return bindingResult.getFieldErrors().stream()
				.sorted(Comparator.comparingInt((FieldError e) -> TokugimuForm.fieldOrder(e.getField())))
				.map(e -> e.isBindingFailure()
						? TYPE_MISMATCH_MESSAGES.getOrDefault(e.getField(), TYPE_MISMATCH_DEFAULT_MESSAGE)
						: e.getDefaultMessage())
				.filter(StringUtils::hasText)
				.distinct()
				.toList();
	}
}
