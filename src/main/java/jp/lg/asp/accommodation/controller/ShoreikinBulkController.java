package jp.lg.asp.accommodation.controller;

import jp.lg.asp.accommodation.config.JichitaiContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShoreikinBulkDto;
import jp.lg.asp.accommodation.service.ShoreikinBulkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/shoreikin")
public class ShoreikinBulkController {

	private final ShoreikinBulkService shoreikinBulkService;
	private final ScreenAccessChecker accessChecker;
	private final JichitaiContext jichitaiContext;

	private static final String SCREEN_ID = ScreenManagement.SHOREIKIN_BULK;
	private static final String BULK_VIEW = "shoreikin/shoreikinBulk";

	@GetMapping("/bulk")
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String bulk(@RequestParam(required = false) String nendo, Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(SCREEN_ID);

		ShoreikinBulkDto dto = new ShoreikinBulkDto();
		dto.setNendo(nendo);

		List<BigDecimal> kofuRitsuList = shoreikinBulkService.findKofuRitsuList(jichitaiCd, LocalDate.now().getYear());
		if (kofuRitsuList.isEmpty()) {
			model.addAttribute("errorMessage", "交付率のシステム設定値が登録されていません。システム設定から交付率を設定してください。");
		} else {
			dto.setKofuRitsu(kofuRitsuList.get(0));
		}

		model.addAttribute("bulkForm", dto);
		return BULK_VIEW;
	}

	@PostMapping("/bulk/execute")
	@OpeLog(screenId = SCREEN_ID, operation = "一括算出")
	public String executeBulk(@Valid @ModelAttribute ShoreikinBulkDto bulkForm,
			BindingResult bindingResult,
			Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID);

		if (bindingResult.hasErrors()) {
			model.addAttribute("bulkForm", bulkForm);
			return BULK_VIEW;
		}

		try {
			ShoreikinBulkDto result = shoreikinBulkService.executeBulkSanshutsu(bulkForm);
			model.addAttribute("bulkForm", result);

		} catch (Exception e) {
			log.error("一括算出処理エラー", e);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			model.addAttribute("errorMessage", "一括算出処理中にエラーが発生しました。システム管理者にお問い合わせください。");
			model.addAttribute("errorDetail", sw.toString());
			model.addAttribute("bulkForm", bulkForm);
		}

		return BULK_VIEW;
	}
}
