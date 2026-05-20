package jp.lg.asp.accommodation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.GassanForm;
import jp.lg.asp.accommodation.service.GassanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/gassan")
public class GassanController {

    private final GassanService gassanService;
    private final ScreenAccessChecker accessChecker;

    private static final String SCREEN_ID_CONFIG = ScreenManagement.GASSAN_CONFIG;
    private static final String FORM_VIEW = "gassan/tGassanConfig";

    // ========== 新規登録 ==========

    @GetMapping("/registration/{shiteiNo}")
    public String showRegistrationForm(@PathVariable("shiteiNo") String shiteiNo, Model model) {
        accessChecker.checkAccess(SCREEN_ID_CONFIG);
        model.addAttribute("GassanForm", gassanService.buildFormByShiteiNo(shiteiNo));
        model.addAttribute("isEdit", false);
        model.addAttribute("isView", false);
        return FORM_VIEW;
    }

    @PostMapping("/registration")
    public String register(
            @Validated @ModelAttribute("GassanForm") GassanForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        accessChecker.checkAccess(SCREEN_ID_CONFIG);

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("isView", false);
            return FORM_VIEW;
        }
        try {
            gassanService.register(form);
        } catch (Exception e) {
            log.error("合算申告登録エラー", e);
            model.addAttribute("isEdit", false);
            model.addAttribute("isView", false);
            model.addAttribute("errorMessage", e.getMessage());
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "登録が完了しました。");
        return "redirect:/tokugimu/list";
    }

    // ========== 照会 ==========

    @GetMapping("/view-by-shitei/{shiteiNo}")
    public String showViewByShiteiNo(
            @PathVariable("shiteiNo") String shiteiNo,
            @RequestParam(required = false) String gassanShiteiNo,
            Model model) {
        accessChecker.checkAccess(SCREEN_ID_CONFIG);
        GassanForm form = (gassanShiteiNo != null && !gassanShiteiNo.isBlank())
                ? gassanService.getViewFormByShiteiNo(shiteiNo, gassanShiteiNo)
                : gassanService.getLatestByShiteiNo(shiteiNo);
        model.addAttribute("GassanForm", form);
        model.addAttribute("isView", true);
        model.addAttribute("isEdit", false);
        model.addAttribute("editId", form.getGassanShiteiNo());
        return FORM_VIEW;
    }

    // ========== 編集 ==========

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") String id, Model model) {
        accessChecker.checkAccess(SCREEN_ID_CONFIG);
        model.addAttribute("GassanForm", gassanService.getByGassanShiteiNo(id));
        model.addAttribute("isView", false);
        model.addAttribute("isEdit", true);
        model.addAttribute("editId", id);
        return FORM_VIEW;
    }

    @PostMapping("/edit/{id}")
    public String update(
            @PathVariable("id") String id,
            @Validated @ModelAttribute("GassanForm") GassanForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        accessChecker.checkAccess(SCREEN_ID_CONFIG);

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("isView", false);
            model.addAttribute("editId", id);
            return FORM_VIEW;
        }
        try {
            gassanService.updateByGassanShiteiNo(id, form);
        } catch (Exception e) {
            log.error("合算申告更新エラー", e);
            model.addAttribute("isEdit", true);
            model.addAttribute("isView", false);
            model.addAttribute("editId", id);
            model.addAttribute("errorMessage", e.getMessage());
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "更新が完了しました。");
        return "redirect:/tokugimu/list";
    }

    // ========== 削除 ==========

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        accessChecker.checkAccess(SCREEN_ID_CONFIG);
        gassanService.deleteByGassanShiteiNo(id);
        redirectAttributes.addFlashAttribute("successMessage", "合算指定番号:" + id + " のデータを削除しました。");
        return "redirect:/tokugimu/list";
    }
}
