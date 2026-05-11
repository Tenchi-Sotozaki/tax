package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.service.EltaxRenkeiService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/eltax-renkei")
@RequiredArgsConstructor
public class EltaxRenkeiController {

    private final EltaxRenkeiService eltaxRenkeiService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("eltaxRenkeiList", eltaxRenkeiService.findAll());
        return "eltaxRenkei/eltaxRenkei";
    }

    @PostMapping("/import")
    public String importFile(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "ファイルを選択してください。");
            return "redirect:/eltax-renkei";
        }
        try {
            eltaxRenkeiService.importFile(file);
            redirectAttributes.addFlashAttribute("successMessage", "ファイルを取り込みました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/eltax-renkei";
    }

    @GetMapping("/download/{seq}")
    public ResponseEntity<byte[]> download(@PathVariable BigDecimal seq) {
        EltaxRenkei entity = eltaxRenkeiService.findBySeq(seq);
        if (entity == null || entity.getLog() == null) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(entity.getFileName()).build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().headers(headers).body(entity.getLog());
    }
}
