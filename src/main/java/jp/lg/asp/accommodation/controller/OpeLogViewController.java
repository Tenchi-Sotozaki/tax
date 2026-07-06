package jp.lg.asp.accommodation.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jp.lg.asp.accommodation.dto.OpeLogViewDto;
import jp.lg.asp.accommodation.entity.Screen;
import jp.lg.asp.accommodation.service.OpeLogViewService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/log/ope-log")
@RequiredArgsConstructor
public class OpeLogViewController {

	private final OpeLogViewService opeLogViewService;

	@GetMapping
	public String init(Model model) {
		model.addAttribute("form", new OpeLogViewDto());
		model.addAttribute("screens", opeLogViewService.findAllScreens());
		model.addAttribute("items", List.of());
		return "log/opeLogView";
	}

	@PostMapping("/search")
	public String search(@ModelAttribute("form") OpeLogViewDto form, Model model) {
		List<Screen> screens = opeLogViewService.findAllScreens();
		List<OpeLogViewDto> items = opeLogViewService.search(form);
		model.addAttribute("screens", screens);
		model.addAttribute("items", items);
		return "log/opeLogView";
	}
}
