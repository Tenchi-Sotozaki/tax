package jp.lg.asp.accommodation.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.annotation.RptLog;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.service.KanpuMenjoTsuchiReportsService;
import jp.lg.asp.accommodation.service.ReportsCommonService;
import jp.lg.asp.accommodation.service.TokugimuService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/kanpuMenjoTsuchi")
@RequiredArgsConstructor
public class KanpuMenjoTsuchiController {

    private final TokugimuService tokugimuService;
    private final KanpuMenjoTsuchiReportsService kanpuMenjoTsuchiReportsService;
    private final ReportsCommonService reportsCommonService;
    private final JichitaiContext jichitaiContext;

    private static final String SCREEN_ID = ScreenManagement.KANPU_MENJO_TSUCHI;

    @GetMapping
    @OpeLog(screenId = SCREEN_ID, operation = "初期表示")
    public String index(HttpSession session,
                       @AuthenticationPrincipal User userDetails,
                       Model model) {
    	String jichitaiCode = jichitaiContext.getJichitaiCd();
    	ShiteiGassanSearchDto selected = SessionHelper.getShiteiGassan(session);
    	String shiteiNo = SessionHelper.getShiteiNo(session);
    	String gassanShiteiNo = SessionHelper.getGassanShiteiNo(session);

		// 指定番号または合算指定番号が存在しない場合
		if (selected == null || (shiteiNo == null && gassanShiteiNo == null)) {
			model.addAttribute("showShiteiGassanModal", true);
			return "tokugimu/tTokugimuReport";
		}

		String effectiveShiteiNo = shiteiNo != null ? shiteiNo : gassanShiteiNo;

        try {
            log.debug("徴収不能額の還付又は納入義務の免除決定通知書画面表示開始: shiteiNo={}", effectiveShiteiNo);

            TokugimuForm tokugimuForm = tokugimuService.getTokugimuByShiteiNo(effectiveShiteiNo);

            if (tokugimuForm == null) {
                model.addAttribute("errorMessage", "指定された特別徴収義務者が見つかりません。");
                return "error";
            }

            Jichitai jichitai = kanpuMenjoTsuchiReportsService.findJichitai(jichitaiCode);
            String cityName = jichitai.getName();

            KanpuMenjoTsuchiDto dto = new KanpuMenjoTsuchiDto();
            dto.setShiteiNo(effectiveShiteiNo);
            dto.setCityName(cityName);
            dto.setHakkoYmd(LocalDate.now());
            dto.setKoin(reportsCommonService.getReportsDefData(ReportsConstants.KOIN));

			dto.setTokuName(tokugimuForm.getName());
			dto.setTokuYubin("〒" + tokugimuForm.getTokugimuYubinNo());
			dto.setTokuJusho(tokugimuForm.getTokugimuAddress());
			dto.setShisetsuName(tokugimuForm.getFacilityName());

			if (tokugimuForm.getFacilityAddressNo() == null || tokugimuForm.getFacilityAddressNo().isEmpty()) {
				model.addAttribute("errorMessage", "施設情報が見つかりませんでした。");
				return "error";
			}
			dto.setShisetsuYubin("〒" + tokugimuForm.getFacilityAddressNo());
			dto.setShisetsuJusho(tokugimuForm.getFacilityAddress());

            model.addAttribute("dto", dto);

            log.debug("徴収不能額の還付又は納入義務の免除決定通知書画面表示成功");
            return "reports/kanpuMenjoTsuchi";

        } catch (Exception e) {
            log.error("徴収不能額の還付又は納入義務の免除決定通知書画面表示エラー", e);
            model.addAttribute("errorMessage", "画面表示中にエラーが発生しました。");
            return "error";
        }
    }

    @PostMapping("/generatePdf")
    @OpeLog(screenId = SCREEN_ID, operation = "PDF")
	@RptLog(rptId = ReportsConstants.KANPU_MENJO_TSUCHI, operation = ReportsConstants.SOUSA_PDF, shiteiNo = "#dto.shiteiNo")
    public ResponseEntity<byte[]> generatePdf(@ModelAttribute KanpuMenjoTsuchiDto dto,
                                             @AuthenticationPrincipal User userDetails) {
        try {
            log.debug("PDF生成開始: shiteiNo={}, hakkoYmd={}", dto.getShiteiNo(), dto.getHakkoYmd());

            byte[] pdfData = kanpuMenjoTsuchiReportsService.generateTsuchiPdf(dto);

            String filename = "kanpu_menjo_tsuchi_" + dto.getShiteiNo() + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            return ResponseEntity.ok().headers(headers).body(pdfData);

        } catch (Exception e) {
            log.error("PDF生成エラー", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/preview")
    @OpeLog(screenId = SCREEN_ID, operation = "プレビュー")
	@RptLog(rptId = ReportsConstants.KANPU_MENJO_TSUCHI, operation = ReportsConstants.SOUSA_PREVIEW, shiteiNo = "#dto.shiteiNo")
    public ResponseEntity<byte[]> preview(@ModelAttribute KanpuMenjoTsuchiDto dto,
                                         @AuthenticationPrincipal User userDetails) {
        try {
            byte[] pdfData = kanpuMenjoTsuchiReportsService.generateTsuchiPdf(dto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline");
            headers.setContentLength(pdfData.length);

            return ResponseEntity.ok().headers(headers).body(pdfData);

        } catch (Exception e) {
            log.error("プレビューエラー", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/print")
    @OpeLog(screenId = SCREEN_ID, operation = "印刷")
	@RptLog(rptId = ReportsConstants.KANPU_MENJO_TSUCHI, operation = ReportsConstants.SOUSA_PRINT, shiteiNo = "#dto.shiteiNo")
    public ResponseEntity<byte[]> print(@ModelAttribute KanpuMenjoTsuchiDto dto,
                                       @AuthenticationPrincipal User userDetails) {
        try {
            byte[] pdfData = kanpuMenjoTsuchiReportsService.generateTsuchiPdf(dto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline");
            headers.setContentLength(pdfData.length);

            return ResponseEntity.ok().headers(headers).body(pdfData);

        } catch (Exception e) {
            log.error("印刷エラー", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
