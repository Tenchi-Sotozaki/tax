package jp.lg.asp.accommodation.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.service.NokigenService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/nokigen")
@RequiredArgsConstructor
public class NokigenController {

	private final NokigenService nokigenService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.NOKIGEN;
	private static final String SCREEN_ID_CONFIG = ScreenManagement.NOKIGEN_CONFIG;

	@GetMapping("/list")
	public String list(RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);
		Nokigen latest = nokigenService.findAll().stream().findFirst().orElse(null);
		if (latest == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "登録された納入期限がありません。");
			return "redirect:/admin/nokigen/register";
		}
		return "redirect:/admin/nokigen/view/" + latest.getNendo();
	}

	@GetMapping("/register")
	public String register(Model model) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		model.addAttribute("nokigen", new Nokigen());
		model.addAttribute("mode", "register");
		return "admin/nokigenConfig";
	}

	@GetMapping("/view/{nendo}")
	public String view(@PathVariable String nendo, Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID);
		Nokigen nokigen = nokigenService.findByNendo(nendo);
		if (nokigen == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "指定されたデータが見つかりません。");
			return "redirect:/admin/nokigen/register";
		}
		toHtmlDate(nokigen);
		model.addAttribute("nokigen", nokigen);
		model.addAttribute("mode", "view");
		model.addAttribute("nendoList", nokigenService.findAll().stream().map(Nokigen::getNendo).toList());
		return "admin/nokigenConfig";
	}

	@GetMapping("/edit/{nendo}")
	public String edit(@PathVariable String nendo, Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		Nokigen nokigen = nokigenService.findByNendo(nendo);
		if (nokigen == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "指定されたデータが見つかりません。");
			return "redirect:/admin/nokigen/list";
		}
		toHtmlDate(nokigen);
		model.addAttribute("nokigen", nokigen);
		model.addAttribute("mode", "edit");
		return "admin/nokigenConfig";
	}

	/** 前年度データをJSONで返す（画面への複写用） */
	@GetMapping("/prev-data/{nendo}")
	@ResponseBody
	public ResponseEntity<Map<String, String>> prevData(@PathVariable String nendo) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		int prevNendo = Integer.parseInt(nendo) - 1;
		Nokigen prev = nokigenService.findByNendo(String.valueOf(prevNendo));
		if (prev == null) {
			return ResponseEntity.notFound().build();
		}
		Map<String, String> result = new java.util.LinkedHashMap<>();
		result.put("nokigen1st",  toHtml(prev.getNokigen1st()));
		result.put("nokigen2nd",  toHtml(prev.getNokigen2nd()));
		result.put("nokigen3rd",  toHtml(prev.getNokigen3rd()));
		result.put("nokigen4th",  toHtml(prev.getNokigen4th()));
		result.put("nokigen5th",  toHtml(prev.getNokigen5th()));
		result.put("nokigen6th",  toHtml(prev.getNokigen6th()));
		result.put("nokigen7th",  toHtml(prev.getNokigen7th()));
		result.put("nokigen8th",  toHtml(prev.getNokigen8th()));
		result.put("nokigen9th",  toHtml(prev.getNokigen9th()));
		result.put("nokigen10th", toHtml(prev.getNokigen10th()));
		result.put("nokigen11th", toHtml(prev.getNokigen11th()));
		result.put("nokigen12th", toHtml(prev.getNokigen12th()));
		return ResponseEntity.ok(result);
	}

	@PostMapping("/save")
	public String save(Nokigen nokigen, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		try {
			boolean isNew = !nokigenService.existsByNendo(nokigen.getNendo());
			if (isNew && nokigenService.existsByNendo(nokigen.getNendo())) {
				redirectAttributes.addFlashAttribute("errorMessage", "この年度は既に登録されています。");
				return "redirect:/admin/nokigen/register";
			}
			nokigenService.save(nokigen);
			String message = isNew ? "納入期限を登録しました。" : "納入期限を更新しました。";
			redirectAttributes.addFlashAttribute("successMessage", message);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
			return "redirect:/admin/nokigen/register";
		}
		return "redirect:/admin/nokigen/register";
	}

	/** yyyyMMdd → yyyy-MM-dd に変換してHTML date inputに対応 */
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
