package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jp.lg.asp.accommodation.dto.KofuRitsuConfigDto;
import jp.lg.asp.accommodation.entity.KofuRitsu;
import jp.lg.asp.accommodation.service.KofuRitsuConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/kofu-ritsu")
public class KofuRitsuConfigController {

	private final KofuRitsuConfigService kofuRitsuConfigService;

	@GetMapping
	public String register(Model model) {
		model.addAttribute("configForm", new KofuRitsuConfigDto());
		model.addAttribute("mode", "register");
		return "admin/kofuRitsuConfig";
	}

	@PostMapping("/save")
	public String save(@Valid @ModelAttribute("configForm") KofuRitsuConfigDto configForm,
			BindingResult bindingResult, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("validationErrors", KofuRitsuConfigDto.validate(configForm).values());
			model.addAttribute("mode", "register");
			return "admin/kofuRitsuConfig";
		}
		if (kofuRitsuConfigService.existsByTekiyoStNendo(configForm.getTekiyoStNendo())) {
			model.addAttribute("errorMessage", "登録済みの適用開始年度です。");
			model.addAttribute("mode", "register");
			return "admin/kofuRitsuConfig";
		}
		try {
			kofuRitsuConfigService.register(configForm);
			redirectAttributes.addFlashAttribute("successMessage", "交付率を登録しました。");
		} catch (Exception e) {
			log.error("交付率登録エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "交付率の登録に失敗しました: " + e.getMessage());
		}
		return "redirect:/admin/kofu-ritsu/list";
	}

	@GetMapping("/list")
	public String list(Model model, RedirectAttributes redirectAttributes) {
		try {
			model.addAttribute("historyList", kofuRitsuConfigService.findAll());
		} catch (Exception e) {
			log.error("交付率一覧取得エラー", e);
			model.addAttribute("errorMessage", "交付率履歴の取得に失敗しました: " + e.getMessage());
			model.addAttribute("historyList", List.of());
		}
		return "admin/kofuRitsuList";
	}

	@GetMapping("/view/{rno}")
	public String viewForm(@PathVariable BigDecimal rno, Model model, RedirectAttributes redirectAttributes) {
		try {
			KofuRitsu entity = kofuRitsuConfigService.findByRno(rno);
			KofuRitsuConfigDto form = toDto(entity);
			model.addAttribute("configForm", form);
			model.addAttribute("rno", rno);
			model.addAttribute("mode", "view");
		} catch (Exception e) {
			log.error("交付率取得エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "交付率の取得に失敗しました: " + e.getMessage());
			return "redirect:/admin/kofu-ritsu/list";
		}
		return "admin/kofuRitsuConfig";
	}

	@GetMapping("/edit/{rno}")
	public String editForm(@PathVariable BigDecimal rno, Model model, RedirectAttributes redirectAttributes) {
		try {
			KofuRitsu entity = kofuRitsuConfigService.findByRno(rno);
			KofuRitsuConfigDto form = toDto(entity);
			model.addAttribute("configForm", form);
			model.addAttribute("rno", rno);
			model.addAttribute("mode", "edit");
		} catch (Exception e) {
			log.error("交付率取得エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "交付率の取得に失敗しました: " + e.getMessage());
			return "redirect:/admin/kofu-ritsu/list";
		}
		return "admin/kofuRitsuConfig";
	}

	@PostMapping("/edit/{rno}")
	public String editSave(@PathVariable BigDecimal rno,
			@Valid @ModelAttribute("configForm") KofuRitsuConfigDto configForm,
			BindingResult bindingResult, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("validationErrors", KofuRitsuConfigDto.validate(configForm).values());
			model.addAttribute("rno", rno);
			model.addAttribute("mode", "edit");
			return "admin/kofuRitsuConfig";
		}
		try {
			kofuRitsuConfigService.update(rno, configForm);
			redirectAttributes.addFlashAttribute("successMessage", "交付率を更新しました。");
		} catch (Exception e) {
			log.error("交付率更新エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "交付率の更新に失敗しました: " + e.getMessage());
		}
		return "redirect:/admin/kofu-ritsu/list";
	}

	private KofuRitsuConfigDto toDto(KofuRitsu entity) {
		KofuRitsuConfigDto form = new KofuRitsuConfigDto();
		form.setKofuRitsu(entity.getKofuRitsu());
		form.setSanshutsu(entity.getSanshutsu());
		form.setKbn(entity.getKbn());
		form.setSaiteigaku(entity.getSaiteigaku());
		form.setTekiyoStNendo(entity.getTekiyoStNendo());
		return form;
	}
}