package jp.lg.asp.accommodation.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.RptStatusListItem;
import jp.lg.asp.accommodation.dto.RptStatusSearchForm;
import jp.lg.asp.accommodation.service.RptStatusService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/tokugimu/reports-status")
@RequiredArgsConstructor
public class RptStatusController {

    private final RptStatusService rptStatusService;
    private final ScreenAccessChecker accessChecker;
    private static final String SCREEN_ID = ScreenManagement.TOKUGIMU_STATUS_VIEW;
    private static final String VIEW = "tokugimu/tTokugimuReportsStatus";

    @GetMapping
    @OpeLog(screenId = SCREEN_ID, operation = "初期表示")
    public String init(Model model) {
        accessChecker.checkAccess(SCREEN_ID);
        model.addAttribute("searchForm", new RptStatusSearchForm());
        model.addAttribute("reports", rptStatusService.findAllReports());
        model.addAttribute("items", List.of());
        model.addAttribute("isSearched", false);
        return VIEW;
    }

    @PostMapping("/search")
    @OpeLog(screenId = SCREEN_ID, operation = "検索")
    public String search(@ModelAttribute("searchForm") RptStatusSearchForm form, Model model) {
        accessChecker.checkAccess(SCREEN_ID);
        List<RptStatusListItem> items = rptStatusService.search(form);
        model.addAttribute("searchForm", form);
        model.addAttribute("reports", rptStatusService.findAllReports());
        model.addAttribute("items", items);
        model.addAttribute("isSearched", true);
        return VIEW;
    }
}
