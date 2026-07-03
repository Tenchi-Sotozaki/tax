package jp.lg.asp.accommodation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jp.lg.asp.accommodation.dto.JichitaiConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/jichitai-config")
public class JichitaiConfigController {

	private final JichitaiRepository jichitaiRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@GetMapping
	public String index(Model model) {
		Jichitai jichitai = jichitaiRepository.findById(jichitaiCd).orElseThrow();
		JichitaiConfigDto form = new JichitaiConfigDto();
		form.setStartMonth(jichitai.getStartMonth());
		model.addAttribute("configForm", form);
		model.addAttribute("jichitai", jichitai);
		return "admin/jichitaiConfig";
	}

	@PostMapping("/save")
	public String save(@Valid @ModelAttribute("configForm") JichitaiConfigDto configForm,
			BindingResult bindingResult, Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			Jichitai jichitai = jichitaiRepository.findById(jichitaiCd).orElseThrow();
			model.addAttribute("jichitai", jichitai);
			return "admin/jichitaiConfig";
		}
		try {
			Jichitai jichitai = jichitaiRepository.findById(jichitaiCd).orElseThrow();
			jichitai.setStartMonth(configForm.getStartMonth());
			jichitaiRepository.save(jichitai);
			redirectAttributes.addFlashAttribute("successMessage", "年度開始月を保存しました。");
		} catch (Exception e) {
			log.error("年度開始月保存エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", "年度開始月の保存に失敗しました: " + e.getMessage());
		}
		return "redirect:/admin/jichitai-config";
	}
}
