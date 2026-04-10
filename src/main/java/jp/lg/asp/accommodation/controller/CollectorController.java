package jp.lg.asp.accommodation.controller;

import jp.lg.asp.accommodation.dto.CollectorForm;
import jp.lg.asp.accommodation.dto.CollectorListItem;
import jp.lg.asp.accommodation.dto.CollectorSearchForm;
import jp.lg.asp.accommodation.service.CollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/collector")
public class CollectorController {

    private final CollectorService collectorService;

    // ========== 一覧・検索 ==========

    /** GET /collector/list — 一覧・検索 */
    @GetMapping("/list")
    public String list(@ModelAttribute CollectorSearchForm searchForm, Model model) {
        model.addAttribute("items", collectorService.search(searchForm));
        model.addAttribute("searchForm", searchForm);
        return "collector/special-collector-management";
    }

    // ========== 新規登録 ==========

    /** GET /collector/registration — 新規登録画面 */
    @GetMapping("/registration")
    public String showRegistrationForm(Model model) {
        model.addAttribute("collectorForm", new CollectorForm());
        model.addAttribute("isEdit", false);
        return "collector/collector-registration";
    }

    /** POST /collector/registration — 新規登録実行 */
    @PostMapping("/registration")
    public String register(
            @Validated @ModelAttribute("collectorForm") CollectorForm collectorForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "collector/collector-registration";
        }

        // TODO: DB登録処理
        log.info("特別徴収義務者登録: {}", collectorForm.getObligorName());
        redirectAttributes.addFlashAttribute("successMessage", "登録が完了しました。");
        return "redirect:/collector/list";
    }

    // ========== 編集 ==========

    /** GET /collector/edit/{id} — 編集画面 */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        CollectorForm form = collectorService.getCollectorById(id);
        model.addAttribute("collectorForm", form);
        model.addAttribute("isEdit", true);
        model.addAttribute("editId", id);
        return "collector/collector-registration";
    }

    /** POST /collector/edit/{id} — 更新実行 */
    @PostMapping("/edit/{id}")
    public String update(
            @PathVariable Long id,
            @Validated @ModelAttribute("collectorForm") CollectorForm collectorForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("editId", id);
            return "collector/collector-registration";
        }

        // TODO: DB更新処理
        log.info("特別徴収義務者更新: id={}, name={}", id, collectorForm.getObligorName());
        redirectAttributes.addFlashAttribute("successMessage", "更新が完了しました。");
        return "redirect:/collector/list";
    }

    // ========== 削除 ==========

    /** POST /collector/delete/{id} — 削除実行 */
    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        // TODO: DB削除処理
        log.info("特別徴収義務者削除: id={}", id);
        redirectAttributes.addFlashAttribute("successMessage", "ID:" + id + " のデータを削除しました。");
        return "redirect:/collector/list";
    }
}
