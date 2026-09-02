package jp.lg.asp.accommodation.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.JichitaiListSearchForm;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.service.JichitaiListService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/jichitai-list")
public class JichitaiListController {

    private final JichitaiListService jichitaiListService;
    private final ScreenAccessChecker accessChecker;

    private static final String SCREEN_ID = ScreenManagement.JICHITAI_LIST;

    @GetMapping
    @OpeLog(screenId = SCREEN_ID, operation = "照会")
    public String list(
            @ModelAttribute JichitaiListSearchForm searchForm,
            @RequestParam(defaultValue = "false") boolean searched,
            Model model) {

        accessChecker.checkAccess(SCREEN_ID);

        List<Jichitai> items = searched
                ? jichitaiListService.search(searchForm)
                : List.of();

        model.addAttribute("items", items);
        model.addAttribute("searched", searched);

        boolean canWrite;
        try {
            accessChecker.checkWriteAccess(ScreenManagement.JICHITAI_CONFIG);
            canWrite = true;
        } catch (Exception e) {
            canWrite = false;
        }
        model.addAttribute("canWrite", canWrite);

        return "admin/jichitaiList";
    }
}
