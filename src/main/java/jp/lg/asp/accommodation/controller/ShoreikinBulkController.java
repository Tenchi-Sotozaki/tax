package jp.lg.asp.accommodation.controller;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShoreikinBulkDto;
import jp.lg.asp.accommodation.repository.KofuRitsuRepository;
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
	private final KofuRitsuRepository kofuRitsuRepository;

	private static final String SCREEN_ID = ScreenManagement.SHOREIKIN;
	private static final String BULK_VIEW = "shoreikin/shoreikinBulk";

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@GetMapping("/bulk")
	public String bulk(@RequestParam(required = false) String nendo, Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		ShoreikinBulkDto dto = new ShoreikinBulkDto();

		// 決定した年度を設定
		dto.setNendo(nendo);

		// 交付率を取得して設定
		dto.setKofuRitsu(kofuRitsuRepository.findKofuRitsuByJichitaiCd(jichitaiCd, LocalDate.now()));

		model.addAttribute("bulkForm", dto);
		return BULK_VIEW;
	}

	@PostMapping("/bulk/execute")
	public String executeBulk(@Valid @ModelAttribute ShoreikinBulkDto bulkForm,
			BindingResult bindingResult,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		if (bindingResult.hasErrors()) {
			model.addAttribute("bulkForm", bulkForm);
			return BULK_VIEW;
		}

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