package jp.lg.asp.accommodation.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.RptLogViewDto;
import jp.lg.asp.accommodation.entity.Reports;
import jp.lg.asp.accommodation.service.RptLogViewService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/log/rpt-log")
@RequiredArgsConstructor
public class RptLogViewController {

	private final RptLogViewService rptLogViewService;
	private final ScreenAccessChecker accessChecker;
	private static final String SCREEN_ID = ScreenManagement.RPT_LOG_VIEW;

	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String init(Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		model.addAttribute("form", new RptLogViewDto());
		model.addAttribute("reports", rptLogViewService.findAllReports());
		model.addAttribute("items", List.of());
		return "log/rptLogView";
	}

	@PostMapping("/search")
	@OpeLog(screenId = SCREEN_ID, operation = "検索")
	public String search(@Validated @ModelAttribute("form") RptLogViewDto form, BindingResult result, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		List<Reports> reports = rptLogViewService.findAllReports();
		model.addAttribute("reports", reports);
		if (result.hasErrors()) {
			model.addAttribute("items", List.of());
			return "log/rptLogView";
		}
		List<RptLogViewDto> items = rptLogViewService.search(form);
		model.addAttribute("items", items);
		return "log/rptLogView";
	}
}
