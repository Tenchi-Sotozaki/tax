package jp.lg.asp.accommodation.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.FurikomiKozaDto;
import jp.lg.asp.accommodation.service.FurikomiKozaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 振込先口座照会／登録／編集 Controller
 * 仕様書：振込先口座照会・登録・編集.csv に基づく実装
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/shoreikin/furikomiKoza")
public class FurikomiKozaController {

	private final FurikomiKozaService furikomiKozaService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.FURIKOMI_KOZA;
	private static final String KOZA_VIEW = "shoreikin/furikomiKoza";

	/**
	 * 振込先口座照会画面表示
	 * @param shiteiNo 指定番号
	 * @param model モデル
	 * @return 画面パス
	 */
	@GetMapping
	public String view(@RequestParam String shiteiNo, Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		FurikomiKozaDto dto = furikomiKozaService.getFurikomiKoza(shiteiNo);
		model.addAttribute("kozaForm", dto);

		return KOZA_VIEW;
	}

	/**
	 * 編集モード切り替え
	 * @param kozaForm フォームデータ
	 * @param model モデル
	 * @return 画面パス
	 */
	@PostMapping("/edit")
	public String editMode(@ModelAttribute FurikomiKozaDto kozaForm, Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		kozaForm.setMode("edit");
		model.addAttribute("kozaForm", kozaForm);

		return KOZA_VIEW;
	}

	/**
	 * 振込先口座情報登録処理
	 * @param kozaForm フォームデータ
	 * @param bindingResult バリデーション結果
	 * @param model モデル
	 * @param redirectAttributes リダイレクト属性
	 * @return リダイレクト先または画面パス
	 */
	@PostMapping("/create")
	public String create(@Valid @ModelAttribute FurikomiKozaDto kozaForm,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);

		if (bindingResult.hasErrors()) {
			kozaForm.setMode("create");
			model.addAttribute("kozaForm", kozaForm);
			return KOZA_VIEW;
		}

		try {
			furikomiKozaService.createFurikomiKoza(kozaForm);
			redirectAttributes.addFlashAttribute("successMessage", "振込先口座情報を登録しました。");
		} catch (Exception e) {
			log.error("振込先口座登録エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "振込先口座情報登録に失敗しました: " + e.getMessage());
		}

		return "redirect:/shoreikin/list";
	}

	/**
	 * 振込先口座情報更新処理
	 * @param kozaForm フォームデータ
	 * @param bindingResult バリデーション結果
	 * @param model モデル
	 * @param redirectAttributes リダイレクト属性
	 * @return リダイレクト先または画面パス
	 */
	@PostMapping("/update")
	public String update(@Valid @ModelAttribute FurikomiKozaDto kozaForm,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);

		if (bindingResult.hasErrors()) {
			kozaForm.setMode("edit");
			model.addAttribute("kozaForm", kozaForm);
			return KOZA_VIEW;
		}

		try {
			furikomiKozaService.updateFurikomiKoza(kozaForm);
			redirectAttributes.addFlashAttribute("successMessage", "振込先口座情報を更新しました。");
		} catch (Exception e) {
			log.error("振込先口座更新エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "振込先口座更新に失敗しました: " + e.getMessage());
		}

		return "redirect:/shoreikin/list";
	}
}