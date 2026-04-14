package jp.lg.asp.accommodation.controller;

import jp.lg.asp.accommodation.dto.TaxDeclarationForm;
import jp.lg.asp.accommodation.service.DeclarationService;
import jp.lg.asp.accommodation.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/declaration")
public class TaxDeclarationController {

    private final DeclarationService declarationService;
    private final ReportService reportService;

    private static final String VIEW = "declaration/tax-declaration-registration";

    /**
     * GET /declaration/register/{obligorId}
     * 宿泊税情報登録画面の表示
     */
    @GetMapping("/register/{obligorId}")
    public String showForm(@PathVariable String obligorId, Model model) {
        model.addAttribute("taxDeclarationForm", declarationService.buildInitialDeclarationForm(obligorId));
        model.addAttribute("obligorName",        declarationService.getObligorName(obligorId));
        return VIEW;
    }

    /**
     * POST /declaration/register
     * 宿泊税情報の登録処理
     */
    @PostMapping("/register")
    public String register(
            @Validated @ModelAttribute("taxDeclarationForm") TaxDeclarationForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("obligorName", declarationService.getObligorName(form.getObligorId()));
            return VIEW;
        }

        declarationService.registerDeclaration(form);
        redirectAttributes.addFlashAttribute("successMessage", "宿泊税情報を登録しました。");
        return "redirect:/declaration/payment-ledger/" + form.getObligorId();
    }

    /**
     * GET /declaration/pdf/{id}
     * 宿泊税申告書をPDF形式で出力する。
     */
    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> exportPdf(@PathVariable String id) {
        byte[] pdf = reportService.generateDeclarationPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "tax-declaration-" + id + ".pdf");
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
