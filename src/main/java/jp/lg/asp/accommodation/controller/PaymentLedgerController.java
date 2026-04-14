package jp.lg.asp.accommodation.controller;

import jp.lg.asp.accommodation.dto.PaymentLedgerSearchForm;
import jp.lg.asp.accommodation.dto.PaymentRecordDto;
import jp.lg.asp.accommodation.service.DeclarationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PaymentLedgerController {

    private final DeclarationService declarationService;

    /**
     * GET /declaration/payment-ledger/{obligorId}
     * 納入金額管理台帳の表示
     */
    @GetMapping("/declaration/payment-ledger/{obligorId}")
    public String show(
            @PathVariable String obligorId,
            @ModelAttribute PaymentLedgerSearchForm searchForm,
            Model model) {

        List<PaymentRecordDto> records = declarationService.searchRecords(obligorId, searchForm);

        model.addAttribute("taxCycleInfo",  declarationService.getTaxCycleInfo(obligorId));
        model.addAttribute("records",       records);
        model.addAttribute("totalAmount",   declarationService.calcTotalAmount(records));
        model.addAttribute("obligorId",     obligorId);
        model.addAttribute("searchForm",    searchForm);
        return "declaration/payment-ledger";
    }
}
