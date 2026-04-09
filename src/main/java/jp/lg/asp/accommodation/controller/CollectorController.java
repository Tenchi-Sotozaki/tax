package jp.lg.asp.accommodation.controller;

import jp.lg.asp.accommodation.dto.CollectorForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/collector")
public class CollectorController {

    /**
     * 特別徴収義務者 新規登録画面表示
     * GET /collector/collector-registration
     */
    @GetMapping("/collector-registration")
    public String showRegistrationForm(Model model) {
        model.addAttribute("collectorForm", new CollectorForm());
        return "collector/collector-registration";
    }

    /**
     * 特別徴収義務者 登録実行
     * POST /collector/collector-registration
     */
    @PostMapping("/collector-registration")
    public String register(
            @Validated @ModelAttribute("collectorForm") CollectorForm collectorForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.debug("バリデーションエラー: {} 件", bindingResult.getErrorCount());
            return "collector/collector-registration";
        }

        // TODO: DB登録処理（Service呼び出し）
        log.info("特別徴収義務者登録: {}", collectorForm.getObligorName());

        redirectAttributes.addFlashAttribute("successMessage", "登録が完了しました。");
        return "redirect:/collector/list";
    }
}
