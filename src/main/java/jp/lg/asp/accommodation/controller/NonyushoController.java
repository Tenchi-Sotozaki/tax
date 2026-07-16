package jp.lg.asp.accommodation.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.NonyushoDataResponse;
import jp.lg.asp.accommodation.dto.NonyushoDto;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.service.NonyushoReportsService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.service.TokugimuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 納入書コントローラー
 */
@Slf4j
@Controller
@RequestMapping("/nonyusho")
@RequiredArgsConstructor
public class NonyushoController {

    private final NonyushoReportsService nonyushoReportsService;
    private final TokugimuService tokugimuService;
    private final ReportsCommonService reportsCommonService;

    /**
     * 納入書発行画面表示
     */
    @GetMapping
    public String index(Model model, @RequestParam(required = false) String shiteiNo) {
        log.info("納入書発行画面表示: shiteiNo={}", shiteiNo);
        
        if (shiteiNo != null && !shiteiNo.trim().isEmpty()) {
            try {
                // 指定番号に紐づく特別徴収義務者情報を取得
                TokugimuForm tokugimuForm = tokugimuService.getTokugimuByShiteiNo(shiteiNo);
                model.addAttribute("shiteiNo", shiteiNo);
                model.addAttribute("shisetsuName", tokugimuForm.getFacilityName());
                model.addAttribute("tokuName", tokugimuForm.getName());
                model.addAttribute("tokuJusho", tokugimuForm.getTokugimuAddress());
                model.addAttribute("tokuYubinNo", tokugimuForm.getTokugimuYubinNo());
                model.addAttribute("shisetsuJusho", tokugimuForm.getFacilityAddress());
                model.addAttribute("koin", reportsCommonService.getReportsDefData(ReportsConstants.KOIN));
            } catch (Exception e) {
                log.warn("特別徴収義務者情報の取得に失敗: shiteiNo={}", shiteiNo, e);
                model.addAttribute("shiteiNo", shiteiNo);
            }
        }
        
        return "reports/nonyusho";
    }

    /**
     * 納入書動的データ取得API
     */
    @GetMapping("/data")
    @ResponseBody
    public ResponseEntity<NonyushoDataResponse> getNonyushoData(
            @RequestParam String shiteiNo,
            @RequestParam String nendo,
            @RequestParam(required = false) String shinkokuYm) {
        try {
            log.info("納入書動的データ取得API呼び出し: shiteiNo={}, nendo={}, shinkokuYm={}", shiteiNo, nendo, shinkokuYm);
            
            NonyushoDataResponse response = nonyushoReportsService.getNonyushoData(shiteiNo, nendo, shinkokuYm);
            
            log.info("納入書動的データ取得完了: shiteiNo={}, nendo={}, shinkokuYm={}", shiteiNo, nendo, shinkokuYm);
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("納入書動的データ取得エラー: shiteiNo={}, nendo={}, shinkokuYm={}", shiteiNo, nendo, shinkokuYm, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 納入書PDF生成
     */
    @PostMapping("/pdf")
    public ResponseEntity<byte[]> generatePdf(@RequestBody NonyushoDto dto) {
        try {
            log.info("納入書PDF生成開始: shiteiNo={}", dto.getShiteiNo());
            
            byte[] pdf = nonyushoReportsService.generateNonyushoPdf(dto);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "nonyusho.pdf");
            
            log.info("納入書PDF生成完了: shiteiNo={}", dto.getShiteiNo());
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("納入書PDF生成エラー: shiteiNo={}", dto.getShiteiNo(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}