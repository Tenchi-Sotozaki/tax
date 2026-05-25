package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

	private static final String SCREEN_ID = ScreenManagement.SHOREIKIN;
	private static final String BULK_VIEW = "shoreikin/shoreikinBulk";

	@Value("${app.kofukin.rate}")
	private BigDecimal defaultKofuritsu;

	@GetMapping("/bulk")
	public String bulk(@RequestParam(required = false) String nendo, Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		ShoreikinBulkDto dto = new ShoreikinBulkDto();
		dto.setNendo(nendo);
		dto.setKofuRitsu(defaultKofuritsu);

		model.addAttribute("bulkForm", dto);
		return BULK_VIEW;
	}

	@PostMapping("/bulk/execute")
	public String executeIkkatsu(@ModelAttribute ShoreikinBulkDto bulkForm, Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		try {
			ShoreikinBulkDto result = shoreikinBulkService.executeBulkSanshutsu(bulkForm);
			model.addAttribute("bulkForm", result);

			if (result.getFailureCount() == 0) {
				model.addAttribute("successMessage", result.getResultMessage());
			} else {
				model.addAttribute("warningMessage", result.getResultMessage());
			}

		} catch (Exception e) {
			log.error("一括算出処理エラー", e);
			bulkForm.setResultMessage("一括算出処理中にエラーが発生しました: " + e.getMessage());
			model.addAttribute("errorMessage", bulkForm.getResultMessage());
			model.addAttribute("bulkForm", bulkForm);
		}

		return BULK_VIEW;
	}
}