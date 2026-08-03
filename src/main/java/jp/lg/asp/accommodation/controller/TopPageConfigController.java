package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.TopPageForm;
import jp.lg.asp.accommodation.entity.TopPage;
import jp.lg.asp.accommodation.entity.TopPageId;
import jp.lg.asp.accommodation.repository.TopPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * トップページ編集（sc00000010）
 * <p>
 * トップページに掲載する項目を登録・削除する。
 * 掲載後の編集は行わず、削除して再登録する運用のため更新機能は持たない。
 * 自治体ごとのカスタマイズは画面設計書の書き込みにより対象外。
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/top-page-config")
public class TopPageConfigController {

	private static final String SCREEN_ID = ScreenManagement.TOP_PAGE_CONFIG;
	private static final String LIST_VIEW = "admin/topPageConfigList";
	private static final String FORM_VIEW = "admin/topPageConfig";

	private final TopPageRepository topPageRepository;
	private final ScreenAccessChecker accessChecker;

	/**
	 * 登録済みの掲載項目の一覧を表示する。
	 */
	@GetMapping("/list")
	@OpeLog(screenId = SCREEN_ID, operation = "一覧表示")
	public String list(Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		List<TopPage> items = topPageRepository
				.findByJichitaiCdOrderBySeqDesc(TopPage.COMMON_JICHITAI_CD);
		model.addAttribute("items", items);
		model.addAttribute("today", LocalDate.now());
		return LIST_VIEW;
	}

	/**
	 * 登録画面を表示する。
	 */
	@GetMapping("/register")
	@OpeLog(screenId = SCREEN_ID, operation = "登録画面表示")
	public String register(Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		TopPageForm form = new TopPageForm();
		form.setKeisaiStYmd(LocalDate.now());
		model.addAttribute("topPageForm", form);
		return FORM_VIEW;
	}

	/**
	 * 掲載項目を登録する。
	 */
	@PostMapping("/save")
	@OpeLog(screenId = SCREEN_ID, operation = "登録")
	public String save(@ModelAttribute("topPageForm") TopPageForm form,
			Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		List<String> errors = validate(form);
		if (!errors.isEmpty()) {
			model.addAttribute("validationErrors", errors);
			return FORM_VIEW;
		}

		try {
			TopPage entity = new TopPage();
			entity.setJichitaiCd(TopPage.COMMON_JICHITAI_CD);
			entity.setSeq(topPageRepository.findMaxSeq(TopPage.COMMON_JICHITAI_CD).add(BigDecimal.ONE));
			entity.setContents(form.getContents());
			entity.setKeisaiStYmd(form.getKeisaiStYmd());
			entity.setKeisaiEdYmd(form.getKeisaiEdYmd());
			topPageRepository.save(entity);
			redirectAttributes.addFlashAttribute("successMessage", "掲載項目を登録しました。");
		} catch (Exception e) {
			log.error("トップページ掲載項目の登録に失敗しました", e);
			model.addAttribute("validationErrors", List.of("登録に失敗しました。"));
			return FORM_VIEW;
		}
		return "redirect:/admin/top-page-config/list";
	}

	/**
	 * 掲載項目を削除する。
	 */
	@PostMapping("/delete/{seq}")
	@OpeLog(screenId = SCREEN_ID, operation = "削除")
	public String delete(@PathVariable BigDecimal seq, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);
		try {
			topPageRepository.deleteById(new TopPageId(TopPage.COMMON_JICHITAI_CD, seq));
			redirectAttributes.addFlashAttribute("successMessage", "掲載項目を削除しました。");
		} catch (Exception e) {
			log.error("トップページ掲載項目の削除に失敗しました: seq={}", seq, e);
			redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました。");
		}
		return "redirect:/admin/top-page-config/list";
	}

	/**
	 * 入力内容を検証する。
	 *
	 * @param form 入力内容
	 * @return エラーメッセージ。エラーが無い場合は空リスト
	 */
	private List<String> validate(TopPageForm form) {
		List<String> errors = new ArrayList<>();
		if (form.getContents() == null || form.getContents().isBlank()) {
			errors.add("掲載内容を入力してください。");
		}
		if (form.getKeisaiStYmd() == null) {
			errors.add("掲載開始日を入力してください。");
		}
		if (form.getKeisaiStYmd() != null && form.getKeisaiEdYmd() != null
				&& form.getKeisaiEdYmd().isBefore(form.getKeisaiStYmd())) {
			errors.add("掲載終了日は掲載開始日以降の日付を入力してください。");
		}
		return errors;
	}
}
