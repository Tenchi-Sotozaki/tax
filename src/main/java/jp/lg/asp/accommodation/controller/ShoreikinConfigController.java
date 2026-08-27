package jp.lg.asp.accommodation.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;
import jp.lg.asp.accommodation.service.ShoreikinConfigService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 特別徴収事務交付金照会／登録／編集 Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/shoreikin")
public class ShoreikinConfigController {

	private final ShoreikinConfigService shoreikinConfigService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.SHOREIKIN_CONFIG;
	private static final String CONFIG_VIEW = "shoreikin/shoreikinConfig";
	/** 更新失敗時に画面へ出す文言。事由によらず共通にする */
	private static final String UPDATE_ERROR_MESSAGE
			= "交付金情報の更新に失敗しました。時間をおいて再度お試しください。";

	@GetMapping("/config")
	@OpeLog(screenId = SCREEN_ID, operation = "照会")
	public String config(HttpSession session,
			@RequestParam(required = false) String nendo,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		String shiteiNo = SessionHelper.getShiteiNo(session);

		if (shiteiNo == null || shiteiNo.isEmpty()) {
			model.addAttribute("showShiteiGassanModal", true);
			model.addAttribute("configForm", new ShoreikinConfigDto());
			return CONFIG_VIEW;
		}

		ShoreikinConfigDto dto = shoreikinConfigService.getShoreikin(shiteiNo, nendo);
		model.addAttribute("configForm", dto);

		return CONFIG_VIEW;
	}

	/**
	 * （編集/照会）モード切替
	 * @author Atsumu Kuboichi
	 * @param mode
	 * @param configForm
	 * @param model
	 * @return 特別徴収事務交付金照会／登録／編集
	 */
	@PostMapping("/config/switch-mode")
	@OpeLog(screenId = SCREEN_ID, operation = "モード切替")
	public String switchMode(@RequestParam("mode") String mode, 
	                         @ModelAttribute("configForm") ShoreikinConfigDto configForm, 
	                         Model model) {
	    accessChecker.checkWriteAccess(SCREEN_ID);

	    // 指定されたモードを設定
	    configForm.setMode(mode);
	    
	    // 送信されたフォームの情報をmodelへ保存
	    model.addAttribute("configForm", configForm);
	    
	    return CONFIG_VIEW;
	}

	@PostMapping("/config/calculate")
	@OpeLog(screenId = SCREEN_ID, operation = "算出")
	public String calculate(@ModelAttribute("configForm") ShoreikinConfigDto configForm, Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		try {
			ShoreikinConfigDto result = shoreikinConfigService.calculateShoreikin(configForm);
			model.addAttribute("configForm", result);
		} catch (IllegalStateException e) {
			log.warn("交付金情報算出エラー: {}", e.getMessage());
			configForm.setKofuRitsu(null);
			model.addAttribute("configForm", configForm);
		} catch (Exception e) {
			// 想定外の事由。原因はログに残し、画面には内部情報を出さない
			log.error("交付金情報算出エラー", e);
			model.addAttribute("configForm", configForm);
			model.addAttribute("errorMessage",
					"交付金情報の算出に失敗しました。時間をおいて再度お試しください。");
		}

		return CONFIG_VIEW;
	}

	@PostMapping("/config/create")
	@OpeLog(screenId = SCREEN_ID, operation = "新規登録")
	public String create(@Valid @ModelAttribute("configForm") ShoreikinConfigDto configForm,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		if (bindingResult.hasErrors()) {
			configForm.setMode("create");
			model.addAttribute("configForm", configForm);
			model.addAttribute("validationErrors", ShoreikinConfigDto.validate(configForm).values());
			return CONFIG_VIEW;
		}

		try {
			shoreikinConfigService.createShoreikin(configForm);
			redirectAttributes.addFlashAttribute("successMessage", "交付金情報を登録しました。");
		} catch (DataIntegrityViolationException e) {
			// 主キー（自治体・指定番号・年度）の重複。SQLの文面は利用者に出さない
			log.warn("交付金情報の登録が重複: shiteiNo={}, nendo={}",
					configForm.getShiteiNo(), configForm.getNendo(), e);
			redirectAttributes.addFlashAttribute("errorMessage",
					"この年度の交付金情報は既に登録されています。一覧から対象を選び直してください。");
		} catch (Exception e) {
			log.error("交付金登録エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage",
					"交付金情報の登録に失敗しました。時間をおいて再度お試しください。");
		}

		return "redirect:/shoreikin/list";
	}

	@PostMapping("/config/update")
	@OpeLog(screenId = SCREEN_ID, operation = "更新")
	public String update(@Valid @ModelAttribute("configForm") ShoreikinConfigDto configForm,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		if (bindingResult.hasErrors()) {
			configForm.setMode("edit");
			model.addAttribute("configForm", configForm);
			model.addAttribute("validationErrors", ShoreikinConfigDto.validate(configForm).values());
			return CONFIG_VIEW;
		}

		try {
			shoreikinConfigService.updateShoreikin(configForm);
			redirectAttributes.addFlashAttribute("successMessage", "交付金情報を更新しました。");
		} catch (OptimisticLockingFailureException e) {
			// 表示中に他の利用者が更新した（version 不一致）。業務上ありうるので warn に留め、
			// 原因は判明しているためスタックトレースは出さない。画面の文言は共通
			log.warn("交付金情報の更新が競合: shiteiNo={}, nendo={}, cause={}",
					configForm.getShiteiNo(), configForm.getNendo(), e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", UPDATE_ERROR_MESSAGE);
		} catch (Exception e) {
			log.error("交付金更新エラー: shiteiNo={}, nendo={}",
					configForm.getShiteiNo(), configForm.getNendo(), e);
			redirectAttributes.addFlashAttribute("errorMessage", UPDATE_ERROR_MESSAGE);
		}

		return "redirect:/shoreikin/list";
	}
}
