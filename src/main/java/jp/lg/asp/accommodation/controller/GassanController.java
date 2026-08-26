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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
import jp.lg.asp.accommodation.util.SessionHelper;
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
	public String showRegistrationForm(Model model, HttpSession session) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		session.removeAttribute(SessionHelper.SHITEI_GASSAN_KEY);

		model.addAttribute("GassanForm", new GassanForm());
		model.addAttribute("isEdit", false);
		model.addAttribute("isView", false);
		model.addAttribute("showAddressModal", true);
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

	@GetMapping("/view-form")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "照会")
	public String showViewForm(
			@RequestParam(required = false) BigDecimal rno,
			Model model,
			HttpSession session) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);

		String gassanShiteiNo = SessionHelper.getGassanShiteiNo(session);
		if (gassanShiteiNo == null) {
			model.addAttribute("showShiteiGassanModal", true);
			model.addAttribute("GassanForm", new GassanForm());
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", true);
			return FORM_VIEW;
		}

		try {
			GassanForm form = (rno != null)
					? gassanService.getByGassanShiteiNoAndRno(gassanShiteiNo, rno)
					: gassanService.getByGassanShiteiNo(gassanShiteiNo);
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

	@PostMapping("/select")
	public String select(
			@RequestParam String gassanShiteiNo,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) String shisetsuName,
			HttpSession session) {
		ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
		dto.setGassanShiteiNo(gassanShiteiNo);
		dto.setName(name);
		dto.setShisetsuName(shisetsuName);
		SessionHelper.saveShiteiGassan(session, dto);
		return "redirect:/gassan/view-form";
	}

	@GetMapping("/edit")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集画面表示")
	public String showEditForm(Model model, HttpSession session) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		String gassanShiteiNo = SessionHelper.getGassanShiteiNo(session);
		if (gassanShiteiNo == null) {
			model.addAttribute("showShiteiGassanModal", true);
			model.addAttribute("GassanForm", new GassanForm());
			model.addAttribute("isEdit", true);
			model.addAttribute("isView", false);
			return FORM_VIEW;
		}

		try {
			GassanForm form = gassanService.getByGassanShiteiNo(gassanShiteiNo);
			model.addAttribute("GassanForm", form);
			model.addAttribute("isEdit", true);
			model.addAttribute("isView", false);
			model.addAttribute("editId", gassanShiteiNo);
			model.addAttribute("tekiyoStYmdEditable",
					form.getTekiyoStYmd() != null && form.getTekiyoStYmd().isAfter(java.time.LocalDate.now()));
			model.addAttribute("editable",
					form.getTekiyoEdYmd() == null || form.getTekiyoEdYmd().isAfter(java.time.LocalDate.now()));
			return FORM_VIEW;
		} catch (Exception e) {
			log.error("合算申告情報編集エラー", e);
			model.addAttribute("errorMessage", "指定された合算申告情報が見つかりません。");
			return "redirect:/gassan/list";
		}
	}

	@PostMapping("/edit")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集")
	public String updateGassan(
			@Validated @ModelAttribute("GassanForm") GassanForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes,
			HttpSession session) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		String gassanShiteiNo = SessionHelper.getGassanShiteiNo(session);
		if (gassanShiteiNo == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "合算指定番号が未選択です。");
			return "redirect:/gassan/edit";
		}

		if (bindingResult.hasErrors()) {
			gassanService.reloadFacilityList(form);
			model.addAttribute("isEdit", true);
			model.addAttribute("isView", false);
			model.addAttribute("editId", gassanShiteiNo);
			model.addAttribute("tekiyoStYmdEditable",
					form.getTekiyoStYmd() != null && form.getTekiyoStYmd().isAfter(java.time.LocalDate.now()));
			model.addAttribute("editable",
					form.getTekiyoEdYmd() == null || form.getTekiyoEdYmd().isAfter(java.time.LocalDate.now()));
			model.addAttribute("validationErrors", GassanForm.validateForEdit(form).values());
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
			model.addAttribute("tekiyoStYmdEditable",
					form.getTekiyoStYmd() != null && form.getTekiyoStYmd().isAfter(java.time.LocalDate.now()));
			model.addAttribute("editable",
					form.getTekiyoEdYmd() == null || form.getTekiyoEdYmd().isAfter(java.time.LocalDate.now()));
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
			@Validated(GassanForm.RegisterGroup.class) @ModelAttribute("GassanForm") GassanForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes,
			HttpSession session) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		if (bindingResult.hasErrors()) {
			gassanService.reloadFacilityList(form);
			model.addAttribute("isEdit", false);
			model.addAttribute("isView", false);
			model.addAttribute("validationErrors", GassanForm.validate(form).values());
			return FORM_VIEW;
		}
		try {
			String registeredGassanShiteiNo = gassanService.register(form, SessionHelper.getGassanShiteiNo(session));
			ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
			dto.setGassanShiteiNo(registeredGassanShiteiNo);
			SessionHelper.saveShiteiGassan(session, dto);
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

	/**
	 * 合算申告の削除
	 * @author Atsumu Kuboichi
	 * @param form
	 * @param bindingResult
	 * @param model
	 * @param redirectAttributes
	 * @return 遷移先のURL
	 */
	@PostMapping("/delete")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "削除")
	public String delete(@Validated(GassanForm.RegisterGroup.class) @ModelAttribute("GassanForm") GassanForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {

		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		if (form.getGassanShiteiNo() == null || form.getGassanShiteiNo().isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "削除対象の指定がありません。");
			return "redirect:/gassan/list";
		}

		try {
			// 指定番号を指定して削除
			gassanService.deleteByGassanShiteiNo(form.getGassanShiteiNo());

			// 削除成功のメッセージを設定
			redirectAttributes.addFlashAttribute("successMessage", "合算申告の削除が完了しました。");

			return "redirect:/gassan/list";

		} catch (Exception e) {
			log.error("合算申告削除エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました: " + e.getMessage());
			return "redirect:/gassan/edit";
		}
	}
}