package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.GassanDaichoItem;
import jp.lg.asp.accommodation.dto.GassanDaichoSearchForm;
import jp.lg.asp.accommodation.dto.GassanForm;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.GassanDaichoService;
import jp.lg.asp.accommodation.service.GassanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/gassan")
public class GassanController {

	private final GassanService gassanService;
	private final GassanDaichoService gassanDaichoService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID_CONFIG = ScreenManagement.GASSAN_CONFIG;
	private static final String SCREEN_ID_LIST = ScreenManagement.GASSAN_LIST;
	private static final String FORM_VIEW = "gassan/tGassanConfig";
	private static final String DAICHO_VIEW = "gassan/tGassanDaicho";

	@GetMapping("/registration")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "初期表示")
	public String showRegistrationForm(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		// セッションから特別徴収義務者情報を取得
		ShiteiGassanSearchDto selected = (ShiteiGassanSearchDto) session
				.getAttribute(ShiteiGassanSearchApiController.SESSION_KEY);

		// セッション未設定の場合はモーダルを自動表示させる
		if (selected == null || selected.getShiteiNo() == null) {
			model.addAttribute("GassanForm", new GassanForm());
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", false);
			model.addAttribute("showShiteiModal", true);
			return FORM_VIEW;
		}

		// 合算指定番号がセッションに保存されている場合はエラー
		if (selected.getGassanShiteiNo() != null) {
			model.addAttribute("GassanForm", new GassanForm());
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", false);
			model.addAttribute("showShiteiModal", true);
			model.addAttribute("errorMessage", "合算申告登録済みの特別徴収義務者です。特別徴収義務者を再度指定してください。");
			return FORM_VIEW;
		}

		GassanForm form = gassanService.buildFormByShiteiNo(selected.getShiteiNo());
		model.addAttribute("GassanForm", form);
		model.addAttribute("isEdit", false);
		model.addAttribute("isView", false);
		return FORM_VIEW;
	}

	@GetMapping("/list")
	@OpeLog(screenId = SCREEN_ID_LIST, operation = "初期表示")
	public String showDaicho(
			@ModelAttribute("searchForm") GassanDaichoSearchForm searchForm,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID_LIST);

		Page<GassanDaichoItem> items = gassanDaichoService.search(searchForm);
		model.addAttribute("items", items);
		return DAICHO_VIEW;
	}

	@GetMapping("/view/{gassanShiteiNo}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "初期表示")
	public String showView(@PathVariable String gassanShiteiNo, Model model) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);

		GassanDaichoItem item = gassanDaichoService.getByGassanShiteiNo(gassanShiteiNo);
		if (item == null) {
			model.addAttribute("errorMessage", "指定された合算申告情報が見つかりません。");
			return "redirect:/gassan/list";
		}
		model.addAttribute("item", item);
		model.addAttribute("isView", true);
		return "gassan/tGassanView";
	}

	@GetMapping("/view-form/{gassanShiteiNo}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "照会")
	public String showViewForm(@PathVariable String gassanShiteiNo, Model model) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);

		try {
			GassanForm form = gassanService.getByGassanShiteiNo(gassanShiteiNo);
			model.addAttribute("GassanForm", form);
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", true);
			model.addAttribute("editId", gassanShiteiNo);
			return FORM_VIEW;
		} catch (Exception e) {
			log.error("合算申告情報照会エラー", e);
			model.addAttribute("errorMessage", "指定された合算申告情報が見つかりません。");
			return "redirect:/gassan/list";
		}
	}

	@GetMapping("/edit/{gassanShiteiNo}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集画面表示")
	public String showEditForm(@PathVariable String gassanShiteiNo, Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		try {
			GassanForm form = gassanService.getByGassanShiteiNo(gassanShiteiNo);
			model.addAttribute("GassanForm", form);
			model.addAttribute("isEdit", true);
			model.addAttribute("isView", false);
			model.addAttribute("editId", gassanShiteiNo);
			return FORM_VIEW;
		} catch (Exception e) {
			log.error("合算申告情報編集エラー", e);
			model.addAttribute("errorMessage", "指定された合算申告情報が見つかりません。");
			return "redirect:/gassan/list";
		}
	}

	@PostMapping("/edit/{gassanShiteiNo}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集")
	public String updateGassan(
			@PathVariable String gassanShiteiNo,
			@Validated @ModelAttribute("GassanForm") GassanForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		if (bindingResult.hasErrors()) {
			gassanService.reloadFacilityList(form);
			model.addAttribute("isEdit", true);
			model.addAttribute("isView", false);
			model.addAttribute("editId", gassanShiteiNo);
			model.addAttribute("validationErrors", GassanForm.validate(form).values());
			return FORM_VIEW;
		}

		try {
			gassanService.updateByGassanShiteiNo(gassanShiteiNo, form);
			redirectAttributes.addFlashAttribute("successMessage", "合算申告の更新が完了しました。");
			return "redirect:/gassan/list";
		} catch (Exception e) {
			log.error("合算申告更新エラー", e);
			gassanService.reloadFacilityList(form);
			model.addAttribute("isEdit", true);
			model.addAttribute("isView", false);
			model.addAttribute("editId", gassanShiteiNo);
			model.addAttribute("errorMessage", e.getMessage());
			return FORM_VIEW;
		}
	}

	@PostMapping("/facilities-by-atena")
	@ResponseBody
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "施設一覧取得")
	public List<GassanForm.FacilityItem> getFacilitiesByAtena(@RequestBody Map<String, Object> request) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);

		BigDecimal atenaNo = new BigDecimal(request.get("atenaNo").toString());
		return gassanService.getFacilitiesByAtenaNo(atenaNo);
	}

	@PostMapping("/registration")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "登録")
	public String register(
			@Validated @ModelAttribute("GassanForm") GassanForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		if (bindingResult.hasErrors()) {
			gassanService.reloadFacilityList(form);
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", false);
			model.addAttribute("validationErrors", GassanForm.validate(form).values());
			return FORM_VIEW;
		}
		try {
			gassanService.register(form);
			redirectAttributes.addFlashAttribute("successMessage", "合算申告の登録が完了しました。");
			return "redirect:/gassan/list";
		} catch (Exception e) {
			log.error("合算申告登録エラー", e);
			gassanService.reloadFacilityList(form);
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", false);
			model.addAttribute("errorMessage", e.getMessage());
			return FORM_VIEW;
		}
	}
}