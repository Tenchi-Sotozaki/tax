package jp.lg.asp.accommodation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainViewController {

    // ========== 認証 ==========

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    // ========== メイン（リダイレクト） ==========

    @GetMapping({"/", "/declarations/form"})
    public String root() {
        return "redirect:/collector/list";
    }

    // ========== 特別徴収義務者管理 ==========

    @GetMapping("/collector/list")
    public String collectorList() {
        return "collector/special-collector-management";
    }

    @GetMapping("/collector/tax-manager-registration")
    public String taxManagerRegistration() {
        return "collector/tax-manager-registration";
    }

    // ========== 申告・納付管理 ==========

    @GetMapping("/declaration/consolidated-declaration-registration")
    public String consolidatedDeclarationRegistration() {
        return "declaration/consolidated-declaration-registration";
    }

    @GetMapping("/declaration/payment-management")
    public String paymentManagement() {
        return "declaration/payment-management";
    }

    @GetMapping("/declaration/payment-registration")
    public String paymentRegistration() {
        return "declaration/payment-registration";
    }

    // ========== システム管理 ==========

    @GetMapping("/admin/user-search")
    public String userSearch() {
        return "admin/user-search";
    }

    @GetMapping("/admin/user-registration")
    public String userRegistration() {
        return "admin/user-registration";
    }

    @GetMapping("/admin/tax-system-inquiry")
    public String taxSystemInquiry() {
        return "admin/tax-system-inquiry";
    }

    @GetMapping("/admin/tax-system-registration")
    public String taxSystemRegistration() {
        return "admin/tax-system-registration";
    }

    @GetMapping("/admin/tax-cycle-inquiry")
    public String taxCycleInquiry() {
        return "admin/tax-cycle-inquiry";
    }

    @GetMapping("/admin/tax-cycle-registration")
    public String taxCycleRegistration() {
        return "admin/tax-cycle-registration";
    }
}
