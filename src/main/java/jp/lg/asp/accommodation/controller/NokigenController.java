package jp.lg.asp.accommodation.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.service.NokigenService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/nokigen")
@RequiredArgsConstructor
public class NokigenController {

	private final NokigenService nokigenService;
	private final ScreenAccessChecker accessChecker;
	private final JichitaiContext jichitaiContext;

	private static final String SCREEN_ID = ScreenManagement.NOKIGEN;
	private static final String SCREEN_ID_CONFIG = ScreenManagement.NOKIGEN_CONFIG;

	private void addKiMonthLabels(Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		Jichitai jichitai = nokigenService.findJichitai(jichitaiCd);
		if (jichitai == null) return;
		if (jichitai.getNendoStMonth() == null || jichitai.getNendoStMonth().trim().isEmpty()) {
			model.addAttribute("warnMessage", "年度開始月が未設定です。");
			return;
		}
		int st = Integer.parseInt(jichitai.getNendoStMonth().trim());
		java.util.List<String> labels = new java.util.ArrayList<>();
		for (int i = 0; i < 12; i++) {
			int month = (st + i - 1) % 12 + 1;
			labels.add(month + "月");
		}
		model.addAttribute("kiMonthLabels", labels);
	}

	@GetMapping("/list")
	public String list(RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);
		redirectAttributes.addFlashAttribute("nendoList", nokigenService.findAll().stream().map(Nokigen::getNendo).toList());
		Nokigen latest = nokigenService.findAll().stream().findFirst().orElse(null);
		if (latest == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "登録された納入期限がありません。");
			return "redirect:/admin/nokigen/register";
		}
		return "redirect:/admin/nokigen/view/" + latest.getNendo();
	}

	@GetMapping("/register")
	public String register(Model model) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		model.addAttribute("nokigen", new Nokigen());
		model.addAttribute("mode", "register");
		model.addAttribute("nendoList", nokigenService.findAll().stream().map(Nokigen::getNendo).toList());
		addKiMonthLabels(model);
		return "admin/nokigenConfig";
	}

	@GetMapping("/view/{nendo}")
	public String view(@PathVariable String nendo, Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);

		model.addAttribute("nendoList", nokigenService.findAll().stream().map(Nokigen::getNendo).toList());

		Nokigen nokigen = nokigenService.findByNendo(nendo);
		if (nokigen == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "指定されたデータが見つかりません。");
			return "redirect:/admin/nokigen/register";
		}
		toHtmlDate(nokigen);
		model.addAttribute("nokigen", nokigen);
		model.addAttribute("mode", "view");
		addKiMonthLabels(model);
		return "admin/nokigenConfig";
	}

	@GetMapping("/edit/{nendo}")
	public String edit(@PathVariable String nendo, Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		Nokigen nokigen = nokigenService.findByNendo(nendo);
		if (nokigen == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "指定されたデータが見つかりません。");
			return "redirect:/admin/nokigen/list";
		}
		toHtmlDate(nokigen);
		model.addAttribute("nokigen", nokigen);
		model.addAttribute("mode", "edit");
		addKiMonthLabels(model);
		return "admin/nokigenConfig";
	}

	@GetMapping("/exists/{nendo}")
	@ResponseBody
	public ResponseEntity<Map<String, Boolean>> exists(@PathVariable String nendo) {
		return ResponseEntity.ok(Map.of("exists", nokigenService.existsByNendo(nendo)));
	}

	@GetMapping("/prev-data/{nendo}")
	@ResponseBody
	public ResponseEntity<Map<String, String>> prevData(
			@PathVariable String nendo,
			@RequestParam(defaultValue = "none") String shiftMode) {

		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		int prevNendo = Integer.parseInt(nendo) - 1;
		Nokigen prev = nokigenService.findByNendo(String.valueOf(prevNendo));
		if (prev == null) {
			return ResponseEntity.notFound().build();
		}

		Map<String, String> result = nokigenService.getPrevDataWithShift(prev, nendo, shiftMode);

		return ResponseEntity.ok(result);
	}

	@PostMapping("/save")
	public String save(Nokigen nokigen,
			@org.springframework.web.bind.annotation.RequestParam(defaultValue = "register") String mode, Model model,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);

		List<String> nendoList = nokigenService.findAll().stream().map(Nokigen::getNendo).toList();

		if (nokigen.getNendo() == null || nokigen.getNendo().isBlank()) {
			model.addAttribute("nokigen", nokigen);
			model.addAttribute("mode", mode);
			model.addAttribute("validationErrors", java.util.List.of("年度は必須です"));
			model.addAttribute("nendoList", nendoList);
			addKiMonthLabels(model);
			return "admin/nokigenConfig";
		}

		try {
			if ("register".equals(mode) && nokigenService.existsByNendo(nokigen.getNendo())) {
				model.addAttribute("nokigen", nokigen);
				model.addAttribute("mode", "register");
				model.addAttribute("errorMessage", "登録済みの年度です。編集画面から修正してください。");
				model.addAttribute("nendoList", nendoList);
				addKiMonthLabels(model);
				return "admin/nokigenConfig";
			}
			nokigenService.save(nokigen);
			String message = "register".equals(mode) ? "納入期限を登録しました。" : "納入期限を更新しました。";
			redirectAttributes.addFlashAttribute("successMessage", message);
		} catch (Exception e) {
			model.addAttribute("nokigen", nokigen);
			model.addAttribute("mode", mode);
			model.addAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
			model.addAttribute("nendoList", nendoList);
			addKiMonthLabels(model);
			return "admin/nokigenConfig";
		}
		return "redirect:/admin/nokigen/view/" + nokigen.getNendo();
	}

	private void toHtmlDate(Nokigen n) {
		n.setNokigen1st(toHtml(n.getNokigen1st()));
		n.setNokigen2nd(toHtml(n.getNokigen2nd()));
		n.setNokigen3rd(toHtml(n.getNokigen3rd()));
		n.setNokigen4th(toHtml(n.getNokigen4th()));
		n.setNokigen5th(toHtml(n.getNokigen5th()));
		n.setNokigen6th(toHtml(n.getNokigen6th()));
		n.setNokigen7th(toHtml(n.getNokigen7th()));
		n.setNokigen8th(toHtml(n.getNokigen8th()));
		n.setNokigen9th(toHtml(n.getNokigen9th()));
		n.setNokigen10th(toHtml(n.getNokigen10th()));
		n.setNokigen11th(toHtml(n.getNokigen11th()));
		n.setNokigen12th(toHtml(n.getNokigen12th()));
	}

	private String toHtml(String value) {
		if (value == null || value.isBlank() || value.trim().length() != 8)
			return "";
		String v = value.trim();
		return v.substring(0, 4) + "-" + v.substring(4, 6) + "-" + v.substring(6, 8);
	}
}
