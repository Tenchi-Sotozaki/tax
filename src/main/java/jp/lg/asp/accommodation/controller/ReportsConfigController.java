package jp.lg.asp.accommodation.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.dto.ReportsConfigForm;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.service.ReportsConfigService;

@Controller
@RequestMapping("/admin/reports-config")
public class ReportsConfigController {
    
    @Autowired
    private ReportsConfigService reportsConfigService;
    
    @GetMapping
    public String index(Model model) {
        List<ReportsDef> importHistory = reportsConfigService.getImportHistory();
        model.addAttribute("reportsConfigForm", new ReportsConfigForm());
        model.addAttribute("importHistory", importHistory);
        return "admin/reportsConfig";
    }
    
    @PostMapping("/import")
    public String importFile(ReportsConfigForm form, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            if (form.getFile() == null || form.getFile().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "ファイルを選択してください。");
                return "redirect:/admin/reports-config";
            }
            
            // ファイルサイズチェック
            if (form.getFile().getSize() > 10 * 1024 * 1024) { // 10MB
                redirectAttributes.addFlashAttribute("errorMessage", "ファイルサイズが10MBを超えています。");
                return "redirect:/admin/reports-config";
            }
            
            // ファイルタイプチェック
            String contentType = form.getFile().getContentType();
            if (contentType == null || !contentType.equals("image/png")) {
                redirectAttributes.addFlashAttribute("errorMessage", "PNG画像ファイルのみアップロード可能です。");
                return "redirect:/admin/reports-config";
            }
            
            // TODO: 自治体コードの取得方法を実装
            String jichitaiCd = "01202"; // application.ymlから取得するように変更予定
            String userId = authentication.getName();
            
            reportsConfigService.importReportFile(form.getFile(), jichitaiCd, userId);
            redirectAttributes.addFlashAttribute("successMessage", "帳票ファイルの取り込みが完了しました。");
        } catch (Exception e) {
            e.printStackTrace(); // コンソールにエラーを出力
            redirectAttributes.addFlashAttribute("errorMessage", "帳票ファイルの取り込みに失敗しました：" + e.getMessage());
        }
        
        return "redirect:/admin/reports-config";
    }
}