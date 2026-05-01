package jp.lg.asp.accommodation.controller;

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
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.dto.TaxDeclarationForm;
import jp.lg.asp.accommodation.service.DeclarationService;
import jp.lg.asp.accommodation.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/declaration")
@SessionAttributes("taxDeclarationForm")   // フォームをセッションに保持してPDF出力時に再利用
public class TaxDeclarationController {

    private final DeclarationService declarationService;
    private final ReportService      reportService;

    private static final String VIEW = "declaration/tax-declaration-registration";

    /** セッション初期化用（Spring MVC が @SessionAttributes で使用） */
    @ModelAttribute("taxDeclarationForm")
    public TaxDeclarationForm initForm() {
        return new TaxDeclarationForm();
    }

    // -------------------------------------------------------------------------
    // GET: 登録画面表示
    // -------------------------------------------------------------------------

    @GetMapping("/old-register/{obligorId}") 
    public String showForm(@PathVariable String obligorId, Model model) {
        model.addAttribute("taxDeclarationForm",
                declarationService.buildInitialDeclarationForm(obligorId));
        model.addAttribute("obligorName",
                declarationService.getObligorName(obligorId));
        return VIEW;
    }

    // -------------------------------------------------------------------------
    // POST: 登録処理
    // -------------------------------------------------------------------------

    @PostMapping("/register")
    public String register(
            @Validated @ModelAttribute("taxDeclarationForm") TaxDeclarationForm form,
            BindingResult bindingResult,
            Model model,
            SessionStatus sessionStatus,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("obligorName",
                    declarationService.getObligorName(form.getObligorId()));
            return VIEW;
        }

        declarationService.registerDeclaration(form);
        sessionStatus.setComplete();   // 登録完了後にセッションをクリア

        redirectAttributes.addFlashAttribute("successMessage", "宿泊税情報を登録しました。");
        return "redirect:/declaration/payment-ledger/" + form.getObligorId();
    }

    // -------------------------------------------------------------------------
    // GET: PDF出力（セッションのフォームデータを使用）
    // -------------------------------------------------------------------------

    /**
     * GET /declaration/pdf/{id}
     * セッションに保持されている TaxDeclarationForm を使って PDF を生成する。
     * フォームが未入力の場合は ID のみで生成（後方互換）。
     */
    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable String id,
            @ModelAttribute("taxDeclarationForm") TaxDeclarationForm form) {

        // フォームに obligorId がセットされていない場合は id を補完
        if (form.getObligorId() == null) {
            form.setObligorId(id);
        }

        byte[] pdf = reportService.generateDeclarationPdf(form);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"tax-declaration-" + id + ".pdf\"");
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
