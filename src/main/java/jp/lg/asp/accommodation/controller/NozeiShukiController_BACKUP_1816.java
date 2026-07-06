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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.NozeiShukiDto;
import jp.lg.asp.accommodation.entity.NozeiShuki;
import jp.lg.asp.accommodation.service.NozeiShukiService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/nozei-shuki")
@RequiredArgsConstructor
public class NozeiShukiController {

	private final NozeiShukiService nozeiShukiService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.NOZEI_SHUKI;
	private static final String SCREEN_ID_CONFIG = ScreenManagement.NOZEI_SHUKI_CONFIG;

	@GetMapping("/list")
	@OpeLog(screenId = SCREEN_ID, operation = "一覧表示")
	public String index(Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		List<NozeiShukiDto> nozeiShukiList = nozeiShukiService.findAll();
		model.addAttribute("nozeiShukiList", nozeiShukiList);
		return "admin/nozeiShukiDaicho";
	}

	@GetMapping("/search")
	@OpeLog(screenId = SCREEN_ID, operation = "検索")
	public ResponseEntity<Map<String, Object>> search(@RequestParam(required = false) Integer shuki) {
		accessChecker.checkAccess(SCREEN_ID);
		List<NozeiShukiDto> nozeiShukiList = nozeiShukiService.findByShuki(shuki);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", nozeiShukiList);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/register")
	@OpeLog(screenId = SCREEN_ID, operation = "登録画面表示")
	public String register(Model model) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		model.addAttribute("nozeiShuki", new NozeiShuki());
		model.addAttribute("mode", "register");
		return "admin/nozeiShukiConfig";
	}

	@GetMapping("/edit/{seq}")
	@OpeLog(screenId = SCREEN_ID, operation = "編集画面表示")
	public String edit(@PathVariable BigDecimal seq, Model model, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		NozeiShuki nozeiShuki = nozeiShukiService.findBySeq(seq);
		if (nozeiShuki == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "指定されたデータが見つかりません。");
			return "redirect:/admin/nozei-shuki/list";
		}

		model.addAttribute("nozeiShuki", nozeiShuki);
		model.addAttribute("mode", "edit");
		return "admin/nozeiShukiConfig";
	}

	@PostMapping("/save")
<<<<<<< HEAD
	@OpeLog(screenId = SCREEN_ID, operation = "登録・編集")
	public String save(NozeiShuki nozeiShuki, RedirectAttributes redirectAttributes) {
=======
	public String save(NozeiShuki nozeiShuki, Model model, RedirectAttributes redirectAttributes) {
>>>>>>> shusei_kamei
		accessChecker.checkAccess(SCREEN_ID_CONFIG);

		if (nozeiShuki.getShuki() == null) {
			model.addAttribute("nozeiShuki", nozeiShuki);
			model.addAttribute("mode", nozeiShuki.getSeq() == null ? "register" : "edit");
			model.addAttribute("validationErrors", java.util.List.of("納税周期は必須です"));
			return "admin/nozeiShukiConfig";
		}

		try {
			if (nozeiShuki.getSeq() == null && nozeiShukiService.existsByShuki(nozeiShuki.getShuki())) {
				redirectAttributes.addFlashAttribute("errorMessage", "この周期は既に登録されています。");
				return "redirect:/admin/nozei-shuki/register";
			}
			if (nozeiShuki.getSeq() != null
					&& nozeiShukiService.existsByShukiExcludeSeq(nozeiShuki.getShuki(), nozeiShuki.getSeq())) {
				redirectAttributes.addFlashAttribute("errorMessage", "この周期は既に登録されています。");
				return "redirect:/admin/nozei-shuki/edit/" + nozeiShuki.getSeq();
			}
			nozeiShukiService.save(nozeiShuki);
			String message = nozeiShuki.getSeq() == null ? "納税周期を登録しました。" : "納税周期を更新しました。";
			redirectAttributes.addFlashAttribute("successMessage", message);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
			return nozeiShuki.getSeq() == null ? "redirect:/admin/nozei-shuki/register"
					: "redirect:/admin/nozei-shuki/edit/" + nozeiShuki.getSeq();
		}

		return "redirect:/admin/nozei-shuki/list";
	}

	@PostMapping("/delete/{seq}")
	@OpeLog(screenId = SCREEN_ID, operation = "削除")
	public String delete(@PathVariable BigDecimal seq, RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		try {
			nozeiShukiService.delete(seq);
			redirectAttributes.addFlashAttribute("successMessage", "納税周期を削除しました。");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "削除に失敗しました: " + e.getMessage());
		}

		return "redirect:/admin/nozei-shuki/list";
	}
}
