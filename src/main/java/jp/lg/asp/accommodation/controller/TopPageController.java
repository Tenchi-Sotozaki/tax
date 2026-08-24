package jp.lg.asp.accommodation.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.TopPageConfigForm;
import jp.lg.asp.accommodation.entity.TopPageContent;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.MarkdownService;
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
	private static final String LIST_VIEW = "top/topPageConfigDaicho";

	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(Model model) {

	    List<TopPageContent> sharedList = topPageService.findShared();

	    sharedList.forEach(content -> {
	    	content.setTitleHtml(markdownService.toHtml(content.getTitle()));
	    	content.setContentHtml(markdownService.toHtml(content.getHtmlContent()));
	    });

	    model.addAttribute("sharedList", sharedList);

	    return "top/topPage";
	}
	
	/**
	* 一覧表示
	*/
	@GetMapping("/topPageConfigDaicho")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "一覧表示")
	public String list(
	        @RequestParam(defaultValue = "10") int pageSize,
	        Model model) {

	    List<TopPageContent> items =
	            topPageService.findAll();

	    model.addAttribute("items", items);
	    model.addAttribute("pageSize", pageSize);

	    return "top/topPageConfigDaicho";
	}

	@GetMapping("/config")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集画面表示")
	public String config(Model model) {
		accessChecker.checkAccess(SCREEN_ID_CONFIG);
		model.addAttribute("form", topPageService.loadForm());
		return "top/topPageConfig";
	}

	private final MarkdownService markdownService;
	
	@PostMapping("/config/preview")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "プレビュー")
	public String preview(@ModelAttribute("form") TopPageConfigForm form, Model model) {
		String previewTitle = markdownService.toHtml(form.getTitle());
		String previewHtml = markdownService.toHtml(form.getHtmlContent());
	
		model.addAttribute("previewTitle", previewTitle);	
		model.addAttribute("previewHtml", previewHtml);	
		
		model.addAttribute("form", form);	
		model.addAttribute("preview", true);
		return "top/topPageConfig";
	}
	

	@PostMapping("/config/save")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "保存")
	public String save(
	        @Valid @ModelAttribute("form") TopPageConfigForm form,
	        BindingResult bindingResult,
	        Model model,
	        RedirectAttributes redirectAttributes) {

	    // 掲載開始日・終了日の整合性チェック
	    if (form.getPostingStartDate() != null
	            && form.getPostingEndDate() != null
	            && form.getPostingStartDate().isAfter(form.getPostingEndDate())) {

	        bindingResult.reject(
	                "date.reverse",
	                "掲載開始日は掲載終了日以前の日付を入力してください。");
	    }

	    // バリデーションエラー
	    if (bindingResult.hasErrors()) {

	        if (bindingResult.hasFieldErrors("title")) {
	            model.addAttribute(
	                    "errorMessage",
	                    "タイトルを入力してください。");

	        } else if (bindingResult.hasFieldErrors("htmlContent")) {
	            model.addAttribute(
	                    "errorMessage",
	                    "内容を入力してください。");

	        } else {
	            model.addAttribute(
	                    "errorMessage",
	                    bindingResult.getAllErrors()
	                            .get(0)
	                            .getDefaultMessage());
	        }

	        return "top/topPageConfig";
	    }

	    try {
	        topPageService.save(form);

	        redirectAttributes.addFlashAttribute(
	                "successMessage",
	                "トップページコンテンツを保存しました。");

	    } catch (Exception e) {

	        log.error("トップページ保存エラー", e);

	        model.addAttribute(
	                "errorMessage",
	                "保存に失敗しました: " + e.getMessage());

	        return "top/topPageConfig";
	    }

	    return "redirect:/top/config";
	}
	
	@GetMapping("/config/{seq}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "編集画面表示")
	public String edit(
	        @PathVariable Integer seq,
	        Model model) {

	    accessChecker.checkAccess(SCREEN_ID_CONFIG);

	    TopPageContent content =
	            topPageService.findBySeq(seq);

	    TopPageConfigForm form = new TopPageConfigForm();

	    form.setSeq(content.getSeq());
	    form.setTitle(content.getTitle());
	    form.setHtmlContent(content.getHtmlContent());
	    form.setPostingStartDate(content.getPostingStartDate());
	    form.setPostingEndDate(content.getPostingEndDate());

	    model.addAttribute("form", form);

	    return "top/topPageConfig";
	}
	
	@PostMapping("/config/delete/{seq}")
	@OpeLog(screenId = SCREEN_ID_CONFIG, operation = "削除")
	public String delete(
	        @PathVariable Integer seq,
	        RedirectAttributes redirectAttributes) {

	    accessChecker.checkAccess(SCREEN_ID_CONFIG);

	    topPageService.delete(seq);

	    redirectAttributes.addFlashAttribute(
	            "successMessage",
	            "削除しました。");

	    return "redirect:/top/topPageConfigDaicho";
	}
}
