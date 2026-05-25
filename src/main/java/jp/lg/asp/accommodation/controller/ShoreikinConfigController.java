package jp.lg.asp.accommodation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;
import jp.lg.asp.accommodation.service.ShoreikinConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 特別徴収事務交付金照会／登録／編集 Controller
 * 仕様書：特別徴収事務交付金照会・登録・編集.csv に基づく実装
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

	/**
	 * 特別徴収事務交付金照会画面表示
	 * @param shiteiNo 指定番号
	 * @param nendo 交付金年度
	 * @param model モデル
	 * @return 画面パス
	 */
	@GetMapping("/config")
	public String config(@RequestParam String shiteiNo,
			@RequestParam(required = false) String nendo,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		ShoreikinConfigDto dto = shoreikinConfigService.getShoreikin(shiteiNo, nendo);
		model.addAttribute("configForm", dto);

		return CONFIG_VIEW;
	}

	/**
	 * 編集モード切り替え
	 * @param configForm フォームデータ
	 * @param model モデル
	 * @return 画面パス
	 */
	@PostMapping("/config/edit")
	public String editMode(@ModelAttribute ShoreikinConfigDto configForm, Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		configForm.setMode("edit");
		model.addAttribute("configForm", configForm);

		return CONFIG_VIEW;
	}

	/**
	 * 交付金算出処理
	 * @param configForm フォームデータ
	 * @param model モデル
	 * @return 画面パス
	 */
	@PostMapping("/config/calculate")
	public String calculate(@ModelAttribute ShoreikinConfigDto configForm, Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		try {
			ShoreikinConfigDto result = shoreikinConfigService.calculateShoreikin(configForm);
			model.addAttribute("configForm", result);
		} catch (Exception e) {
			log.error("交付金情報算出エラー", e);
			model.addAttribute("configForm", configForm);
			model.addAttribute("errorMessage", "交付金情報算出に失敗しました: " + e.getMessage());
		}

		return CONFIG_VIEW;
	}

	/**
	 * 交付金情報登録処理
	 * @param configForm フォームデータ
	 * @param redirectAttributes リダイレクト属性
	 * @return リダイレクト先
	 */
	@PostMapping("/config/create")
	public String create(@ModelAttribute ShoreikinConfigDto configForm,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);

		try {
			shoreikinConfigService.createShoreikin(configForm);
			redirectAttributes.addFlashAttribute("successMessage", "交付金情報を登録しました。");
		} catch (Exception e) {
			log.error("交付金登録エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "交付金情報登録に失敗しました: " + e.getMessage());
		}

		return "redirect:/shoreikin/list";
	}

	/**
	 * 交付金情報更新処理
	 * @param configForm フォームデータ
	 * @param redirectAttributes リダイレクト属性
	 * @return リダイレクト先
	 */
	@PostMapping("/config/update")
	public String update(@ModelAttribute ShoreikinConfigDto configForm,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);

		try {
			shoreikinConfigService.updateShoreikin(configForm);
			redirectAttributes.addFlashAttribute("successMessage", "交付金情報を更新しました。");
		} catch (Exception e) {
			log.error("交付金更新エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "交付金更新に失敗しました: " + e.getMessage());
		}

		return "redirect:/shoreikin/list";
	}
}