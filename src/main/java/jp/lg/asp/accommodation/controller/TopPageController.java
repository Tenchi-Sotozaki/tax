package jp.lg.asp.accommodation.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.TopPageConfigForm;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.TopPageContent;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.TopPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/top")
@RequiredArgsConstructor
public class TopPageController {

	private final TopPageService topPageService;
	private final JichitaiRepository jichitaiRepository;
	private final ScreenAccessChecker accessChecker;
	private final JichitaiContext jichitaiContext;

	private static final String SCREEN_ID = ScreenManagement.TOP_PAGE;
	private static final String SCREEN_ID_CONFIG = ScreenManagement.TOP_PAGE_CONFIG;

	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(Model model) {
		List<TopPageContent> sharedList = topPageService.findShared();
		model.addAttribute("sharedList", sharedList);
		return "top/topPage";
	}

	@GetMapping("/config")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集画面表示")
	public String config(
			@RequestParam(defaultValue = "0") String kbn,
			@RequestParam(required = false) String jichitaiCd,
			Model model) {
		//accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		if (jichitaiCd == null) {
			jichitaiCd = jichitaiContext.getJichitaiCd();
		}
		List<Jichitai> jichitaiList = jichitaiRepository.findAll();
		model.addAttribute("form", topPageService.loadForm(kbn, jichitaiCd));
		model.addAttribute("jichitaiList", jichitaiList);
		return "top/topPageConfig";
	}

	@PostMapping("/config/preview")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "プレビュー")
	public String preview(@ModelAttribute("form") TopPageConfigForm form, Model model) {
		//accessChecker.checkAccess(SCREEN_ID_CONFIG);
		List<Jichitai> jichitaiList = jichitaiRepository.findAll();
		model.addAttribute("form", form);
		model.addAttribute("jichitaiList", jichitaiList);
		model.addAttribute("preview", true);
		return "top/topPageConfig";
	}

	@PostMapping("/config/save")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "保存")
	public String save(@ModelAttribute("form") TopPageConfigForm form,
			Model model, RedirectAttributes redirectAttributes) {
		//accessChecker.checkWriteAccess(SCREEN_ID_CONFIG);
		try {
			topPageService.save(form);
			redirectAttributes.addFlashAttribute("successMessage", "トップページコンテンツを保存しました。");
		} catch (Exception e) {
			log.error("トップページ保存エラー", e);
			List<Jichitai> jichitaiList = jichitaiRepository.findAll();
			model.addAttribute("form", form);
			model.addAttribute("jichitaiList", jichitaiList);
			model.addAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
			return "top/topPageConfig";
		}
		return "redirect:/top/config?kbn=" + form.getKbn() + "&jichitaiCd=" + form.getJichitaiCd();
	}
}
