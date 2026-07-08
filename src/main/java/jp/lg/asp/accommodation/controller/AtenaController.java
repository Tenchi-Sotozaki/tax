package jp.lg.asp.accommodation.controller;
import jp.lg.asp.accommodation.config.JichitaiContext;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.AtenaSearchForm;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.service.AtenaImportService;
import jp.lg.asp.accommodation.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/atena")
public class AtenaController {

	private final AtenaRepository atenaRepository;
	private final AtenaImportService atenaImportService;
	private final ScreenAccessChecker accessChecker;
	private final HashUtil hashUtil;

	private final JichitaiContext jichitaiContext;

	private static final String ATENA_DAICHO = ScreenManagement.ATENA_DAICHO;
	private static final String ATENA_INSERT = ScreenManagement.ATENA_INSERT;

	@GetMapping("/list")
	@OpeLog(screenId = ATENA_DAICHO, operation = "照会")
	public String list(@ModelAttribute AtenaSearchForm searchForm, Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(ATENA_DAICHO);
		model.addAttribute("items", atenaRepository.search(
				jichitaiCd,
				toLikePattern(searchForm.getAtenaNo(), "exact"),
				toLikePattern(searchForm.getName(), searchForm.getNameMatchType()),
				toLikePattern(searchForm.getNameKana(), searchForm.getNameKanaMatchType()),
				toLikePattern(searchForm.getYubinNo(), "exact"),
				toLikePattern(searchForm.getJusho(), searchForm.getJushoMatchType()),
				toLikePattern(searchForm.getTel(), "exact"),
				toLikePattern(hashIfPresent(searchForm.getKojinNo()), "exact"),
				toLikePattern(searchForm.getHojinNo(), "exact")));
		model.addAttribute("searchForm", searchForm);
		return "atena/atenaDaicho";
	}

	@GetMapping("/import")
	@OpeLog(screenId = ATENA_INSERT, operation = "取込画面表示")
	public String showImport(Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(ATENA_INSERT);
		model.addAttribute("history", atenaImportService.findHistory(jichitaiCd));
		return "atena/atenaRenkei";
	}

	@PostMapping("/import")
	@OpeLog(screenId = ATENA_INSERT, operation = "取込")
	public String importCsv(@RequestParam("file") MultipartFile file,
			RedirectAttributes redirectAttributes) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(ATENA_INSERT);
		
		// ファイルが選択されているかチェック
		if (file.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "ファイルを選択してください。");
			return "redirect:/atena/import";
		}
		
		// ファイル形式チェック（CSVファイルのみ許可）
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
			redirectAttributes.addFlashAttribute("errorMessage", "CSVファイルのみ取り込み可能です。");
			return "redirect:/atena/import";
		}
		
		// Content-Typeチェック（念のため）
		String contentType = file.getContentType();
		if (contentType != null && 
			!contentType.equals("text/csv") && 
			!contentType.equals("application/csv") && 
			!contentType.equals("text/plain")) {
			log.warn("不正なContent-Type: {}", contentType);
		}
		
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			atenaImportService.importCsv(file, jichitaiCd, auth.getName());
			redirectAttributes.addFlashAttribute("successMessage", "CSVファイルの取込が完了しました。");
		} catch (RuntimeException e) {
			// フォーマットエラーなどのユーザーエラー
			log.warn("CSV取込ユーザーエラー: {}", e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		} catch (Exception e) {
			// 予期しないシステムエラー
			log.error("CSV取込システムエラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", 
					"システムエラーが発生しました。管理者にお問い合せください。");
		}
		return "redirect:/atena/import";
	}

	private String emptyToNull(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}

	private String hashIfPresent(String s) {
		return (s == null || s.isBlank()) ? null : hashUtil.sha256(s);
	}

	private String toLikePattern(String value, String matchType) {
		if (value == null || value.isBlank()) return "%";
		return switch (matchType) {
			case "prefix" -> value + "%";
			case "exact"  -> value;
			default       -> "%" + value + "%"; // partial
		};
	}
}
