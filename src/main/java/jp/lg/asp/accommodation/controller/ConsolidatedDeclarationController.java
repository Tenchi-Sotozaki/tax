package jp.lg.asp.accommodation.controller;

import jp.lg.asp.accommodation.dto.ConsolidatedDeclarationForm;
import jp.lg.asp.accommodation.dto.FacilityDto;
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

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/consolidated")
public class ConsolidatedDeclarationController {

    // -------------------------------------------------------------------------
    // GET /consolidated/register/{obligorId} — 登録画面表示
    // -------------------------------------------------------------------------
    @GetMapping("/register/{obligorId}")
    public String showForm(@PathVariable String obligorId, Model model) {

        // TODO: DB実装後は obligorId でサービスから取得する
        String obligorName = buildDummyObligorName(obligorId);

        ConsolidatedDeclarationForm form = new ConsolidatedDeclarationForm();
        form.setRegistrationDate(LocalDate.now());
        form.setObligorId(obligorId);
        form.setObligorName(obligorName);

        model.addAttribute("consolidatedDeclarationForm", form);
        model.addAttribute("facilityOptions", buildDummyFacilities(obligorId));
        return "declaration/consolidated-declaration-registration";
    }

    // -------------------------------------------------------------------------
    // POST /consolidated/register — 登録処理
    // -------------------------------------------------------------------------
    @PostMapping("/register")
    public String register(
            @Validated @ModelAttribute("consolidatedDeclarationForm") ConsolidatedDeclarationForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // バリデーションエラー時は施設リストを再セットして再表示
            model.addAttribute("facilityOptions", buildDummyFacilities(form.getObligorId()));
            return "declaration/consolidated-declaration-registration";
        }

        // TODO: DB登録処理
        log.info("合算申告登録: obligorId={}, period={}, facilities={}",
                form.getObligorId(), form.getApplicablePeriod(), form.getFacilities());

        redirectAttributes.addFlashAttribute("successMessage", "合算申告情報を登録しました。");
        return "redirect:/collector/list";
    }

    // -------------------------------------------------------------------------
    // ダミーデータ生成（DB実装後に削除）
    // -------------------------------------------------------------------------

    private String buildDummyObligorName(String obligorId) {
        return switch (obligorId) {
            case "1" -> "グランドホテル東京";
            case "2" -> "温泉旅館やまと";
            case "3" -> "シティイン新宿";
            default  -> "株式会社グランドホテル東京（ID:" + obligorId + "）";
        };
    }

    private List<FacilityDto> buildDummyFacilities(String obligorId) {
        String name = buildDummyObligorName(obligorId);
        return List.of(
            new FacilityDto(obligorId + "-F1", name + " 本館",   "東京都新宿区西新宿1-1-1"),
            new FacilityDto(obligorId + "-F2", name + " 別館",   "東京都新宿区西新宿1-2-1"),
            new FacilityDto(obligorId + "-F3", name + " アネックス", "東京都新宿区西新宿1-3-1"),
            new FacilityDto(obligorId + "-F4", name + " スイート",  "東京都新宿区西新宿1-4-1")
        );
    }
}
