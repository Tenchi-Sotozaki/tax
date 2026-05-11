package jp.lg.asp.accommodation.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenId;
import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.service.FukaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/declaration")
public class FukaController {

	private final FukaService fukaService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenId.FUKADAICHO;
	private static final String DAICHO_VIEW = "fuka/tFukaDaicho";

	@GetMapping("/payment-ledger/{shiteiNo}")
	public String showDaicho(
			@PathVariable String shiteiNo,
			@RequestParam(name = "nendo", required = false) String nendo,
			@RequestParam(required = false) String status,
			Model model) {
		accessChecker.checkAccess(SCREEN_ID);

		if (nendo == null || nendo.isEmpty()) {
			LocalDate now = LocalDate.now();
			int nendoInt = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
			nendo = String.valueOf(nendoInt);
		}

		FukaDaichoForm form = fukaService.getDaichoData(shiteiNo, nendo, status);

		model.addAttribute("fukaDaichoForm", form);
		model.addAttribute("searchForm", form);
		model.addAttribute("items", form.getItems());
		model.addAttribute("totalAmount", form.getTotalAmount());
		model.addAttribute("obligorId", shiteiNo);

		return DAICHO_VIEW;
	}
}
