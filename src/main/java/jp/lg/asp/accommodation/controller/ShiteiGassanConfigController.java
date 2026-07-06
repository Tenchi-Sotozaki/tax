package jp.lg.asp.accommodation.controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShiteiGassanConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/shitei-gassan")
public class ShiteiGassanConfigController {

	private final JichitaiRepository jichitaiRepository;
	private final ScreenAccessChecker accessChecker;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	private static final String SCREEN_ID_CONFIG = ScreenManagement.SHITEI_GASSAN_CONFIG;
	private static final String SCREEN_ID = ScreenManagement.SHITEI_GASSAN;
	private static final String VIEW = "admin/shiteiGassanConfig";

	/** 設定メニューからの遷移：登録済みなら照会へ、未登録なら登録画面へ */
	@GetMapping("/register")
	public String register(Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		Jichitai jichitai = jichitaiRepository.findById(jichitaiCd).orElse(null);
		if (jichitai != null && (jichitai.getShiteiStChar() != null || jichitai.getGassanStChar() != null)) {
			redirectAttributes.addFlashAttribute("infoMessage", "既に登録されています。照会画面に遷移しました。");
			return "redirect:/admin/shitei-gassan/view";
		}
		model.addAttribute("configDto", new ShiteiGassanConfigDto());
		model.addAttribute("mode", "register");
		return VIEW;
	}

	/** 照会メニューからの遷移 */
	@GetMapping("/view")
	public String view(Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);
		Jichitai jichitai = jichitaiRepository.findById(jichitaiCd).orElse(null);
		if (jichitai.getShiteiStChar() == null && jichitai.getGassanStChar() == null) {
			redirectAttributes.addFlashAttribute("infoMessage", "登録された情報がありません。登録画面に遷移しました。");
			return "redirect:/admin/shitei-gassan/register";
		}
		ShiteiGassanConfigDto dto = toDto(jichitai);
		model.addAttribute("configDto", dto);
		model.addAttribute("mode", "view");
		return VIEW;
	}

	/** 照会画面の編集ボタンから遷移 */
	@GetMapping("/edit")
	public String edit(Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		Jichitai jichitai = jichitaiRepository.findById(jichitaiCd).orElse(null);
		model.addAttribute("configDto", toDto(jichitai));
		model.addAttribute("mode", "edit");
		return VIEW;
	}

	/** 登録・更新処理 */
	@PostMapping("/save")
	public String save(@Valid @ModelAttribute("configDto") ShiteiGassanConfigDto dto,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		if (bindingResult.hasErrors()) {
			boolean isNew = jichitaiRepository.findById(jichitaiCd)
					.map(j -> j.getShiteiStChar() == null && j.getGassanStChar() == null)
					.orElse(true);
			model.addAttribute("mode", isNew ? "register" : "edit");
			return VIEW;
		}
		Jichitai jichitai = jichitaiRepository.findById(jichitaiCd)
				.orElseThrow(() -> new IllegalStateException("自治体情報が見つかりません"));
		boolean isNew = jichitai.getShiteiStChar() == null && jichitai.getGassanStChar() == null;
		jichitai.setShiteiStChar(dto.getShiteiStChar());
		jichitai.setGassanStChar(dto.getGassanStChar());
		jichitaiRepository.save(jichitai);
		log.info("指定番号・合算指定番号を{}しました。jichitaiCd: {}", isNew ? "登録" : "更新", jichitaiCd);
		redirectAttributes.addFlashAttribute("successMessage",
				"指定番号・合算指定番号を" + (isNew ? "登録" : "更新") + "しました。");
		return "redirect:/admin/shitei-gassan/view";
	}

	private ShiteiGassanConfigDto toDto(Jichitai jichitai) {
		ShiteiGassanConfigDto dto = new ShiteiGassanConfigDto();
		dto.setShiteiStChar(jichitai.getShiteiStChar());
		dto.setGassanStChar(jichitai.getGassanStChar());
		dto.setVersion(jichitai.getVersion());
		return dto;
	}
}
