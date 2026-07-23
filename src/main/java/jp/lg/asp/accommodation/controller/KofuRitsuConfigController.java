package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;

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
		return "admin/kofuRitsuConfig";
	}

	@PostMapping("/save")
	public String save(@Valid @ModelAttribute("configForm") KofuRitsuConfigDto configForm,
			BindingResult bindingResult, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("validationErrors", KofuRitsuConfigDto.validate(configForm).values());
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
	public String list(Model model) {
		model.addAttribute("historyList", kofuRitsuConfigService.findAll());
		return "admin/kofuRitsuList";
	}

	@GetMapping("/edit/{rno}")
	public String editForm(@PathVariable BigDecimal rno, Model model) {
		KofuRitsu entity = kofuRitsuConfigService.findByRno(rno);
		KofuRitsuConfigDto form = new KofuRitsuConfigDto();
		form.setKofuRitsu(entity.getKofuRitsu());
		form.setTekiyoStYmd(entity.getTekiyoStYmd());
		form.setTekiyoEdYmd(entity.getTekiyoEdYmd());
		model.addAttribute("configForm", form);
		model.addAttribute("rno", rno);
		return "admin/kofuRitsuEdit";
	}

	@PostMapping("/edit/{rno}")
	public String editSave(@PathVariable BigDecimal rno,
			@Valid @ModelAttribute("configForm") KofuRitsuConfigDto configForm,
			BindingResult bindingResult, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("validationErrors", KofuRitsuConfigDto.validate(configForm).values());
			model.addAttribute("rno", rno);
			return "admin/kofuRitsuEdit";
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
}
