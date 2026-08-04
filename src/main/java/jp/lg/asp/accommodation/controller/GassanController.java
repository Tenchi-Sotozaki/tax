package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
	public String showRegistrationForm(Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

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
		
		int currentPage = items.getNumber();
		int totalPages = items.getTotalPages();

		// 画面に表示するページ番号の範囲
		int windowSize = 1;
		int startPage = Math.max(0, currentPage - windowSize);
		int endPage = Math.min(totalPages - 1, currentPage + windowSize);

		model.addAttribute("currentPage", currentPage);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("startPage", startPage);
		model.addAttribute("endPage", endPage);

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
	public String showViewForm(@PathVariable String gassanShiteiNo,
			@org.springframework.web.bind.annotation.RequestParam(required = false) java.math.BigDecimal rno,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);

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
	public String delete(@Validated @ModelAttribute("GassanForm") GassanForm form,
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
			// 指定番号を指定して削除
			gassanService.deleteByGassanShiteiNo(form.getGassanShiteiNo());
			
			// 削除成功のメッセージを設定
			redirectAttributes.addFlashAttribute("successMessage", "合算申告の削除が完了しました。");
			
			return "redirect:/gassan/list";
			
		} catch (Exception e) {
			log.error("合算申告削除エラー", e);
			gassanService.reloadFacilityList(form);
			model.addAttribute("isEdit", true);
			model.addAttribute("isView", false);
			model.addAttribute("errorMessage", e.getMessage());
			return FORM_VIEW;
		}
	}
}