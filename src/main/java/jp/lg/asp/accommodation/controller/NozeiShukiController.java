package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.dto.NozeiShukiDto;
import jp.lg.asp.accommodation.entity.NozeiShuki;
import jp.lg.asp.accommodation.service.NozeiShukiService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/nozei-shuki")
@RequiredArgsConstructor
public class NozeiShukiController {

    private final NozeiShukiService nozeiShukiService;

    /**
     * 納税周期照会画面表示
     */
    @GetMapping
    public String index(Model model) {
        List<NozeiShukiDto> nozeiShukiList = nozeiShukiService.findAll();
        model.addAttribute("nozeiShukiList", nozeiShukiList);
        return "admin/nozeiShukiDaicho";
    }

    /**
     * 納税周期検索
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam(required = false) Integer shuki) {
        List<NozeiShukiDto> nozeiShukiList = nozeiShukiService.findByShuki(shuki);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", nozeiShukiList);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 納税周期登録画面表示
     */
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("nozeiShuki", new NozeiShuki());
        model.addAttribute("mode", "register");
        return "admin/nozeiShukiConfig";
    }

    /**
     * 納税周期編集画面表示
     */
    @GetMapping("/edit/{seq}")
    public String edit(@PathVariable BigDecimal seq, Model model, RedirectAttributes redirectAttributes) {
        NozeiShuki nozeiShuki = nozeiShukiService.findBySeq(seq);
        if (nozeiShuki == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "指定されたデータが見つかりません。");
            return "redirect:/admin/nozei-shuki";
        }
        
        model.addAttribute("nozeiShuki", nozeiShuki);
        model.addAttribute("mode", "edit");
        return "admin/nozeiShukiConfig";
    }

    /**
     * 納税周期保存
     */
    @PostMapping("/save")
    public String save(NozeiShuki nozeiShuki, RedirectAttributes redirectAttributes) {
        try {
            nozeiShukiService.save(nozeiShuki);
            String message = nozeiShuki.getSeq() == null ? "納税周期を登録しました。" : "納税周期を更新しました。";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
            return nozeiShuki.getSeq() == null ? "redirect:/admin/nozei-shuki/register" : "redirect:/admin/nozei-shuki/edit/" + nozeiShuki.getSeq();
        }
        
        return "redirect:/admin/nozei-shuki";
    }

    /**
     * 納税周期削除
     */
    @PostMapping("/delete/{seq}")
    public String delete(@PathVariable BigDecimal seq, RedirectAttributes redirectAttributes) {
        try {
            nozeiShukiService.delete(seq);
            redirectAttributes.addFlashAttribute("successMessage", "納税周期を削除しました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/admin/nozei-shuki";
    }
}