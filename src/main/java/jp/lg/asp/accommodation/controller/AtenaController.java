package jp.lg.asp.accommodation.controller;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.AtenaImportPreviewDto;
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

	/** 解析結果を確定処理まで保持するセッションキー */
	private static final String IMPORT_PREVIEW_KEY = "atenaImportPreview";

	@GetMapping("/list")
	@OpeLog(screenId = ATENA_DAICHO, operation = "照会")
	public String list(@ModelAttribute AtenaSearchForm searchForm,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int pageSize,
			Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(ATENA_DAICHO);
		searchForm.setPage(page);
		searchForm.setPageSize(pageSize);
		Page<jp.lg.asp.accommodation.entity.Atena> items = atenaRepository.searchPage(
				jichitaiCd,
				toLikePattern(searchForm.getAtenaNo(), "exact"),
				toLikePattern(searchForm.getName(), searchForm.getNameMatchType()),
				toLikePattern(searchForm.getNameKana(), searchForm.getNameKanaMatchType()),
				toLikePattern(searchForm.getYubinNo(), "exact"),
				toLikePattern(searchForm.getJusho(), searchForm.getJushoMatchType()),
				toLikePattern(searchForm.getTel(), "exact"),
				toLikePattern(hashIfPresent(searchForm.getKojinNo()), "exact"),
				toLikePattern(searchForm.getHojinNo(), "exact"),
				PageRequest.of(page, pageSize));
		model.addAttribute("items", items);
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

	/**
	 * CSVを解析し、既存データとの差分を返す。この時点ではDBを更新しない。
	 * 解析結果は確定処理まで（差分確認モーダルの操作中）セッションで保持する。
	 */
	@PostMapping("/import/analyze")
	@ResponseBody
	@OpeLog(screenId = ATENA_INSERT, operation = "取込内容確認")
	public ResponseEntity<?> analyze(@RequestParam("file") MultipartFile file, HttpSession session) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(ATENA_INSERT);

		if (file.isEmpty()) {
			return badRequest("ファイルを選択してください。");
		}
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
			return badRequest("CSVファイルのみ取り込み可能です。");
		}
		String contentType = file.getContentType();
		if (contentType != null
				&& !contentType.equals("text/csv")
				&& !contentType.equals("application/csv")
				&& !contentType.equals("text/plain")) {
			log.warn("不正なContent-Type: {}", contentType);
		}

		try {
			AtenaImportPreviewDto preview = atenaImportService.analyze(file, jichitaiCd);
			session.setAttribute(IMPORT_PREVIEW_KEY, preview);
			return ResponseEntity.ok(preview);
		} catch (RuntimeException e) {
			log.warn("CSV解析ユーザーエラー: {}", e.getMessage());
			return badRequest(e.getMessage());
		} catch (Exception e) {
			log.error("CSV解析システムエラー", e);
			return badRequest("システムエラーが発生しました。管理者にお問い合せください。");
		}
	}

	/**
	 * 差分確認モーダルでの選択結果を受け取り、取込対象のみを登録する。
	 */
	@PostMapping("/import/confirm")
	@ResponseBody
	@OpeLog(screenId = ATENA_INSERT, operation = "取込")
	public ResponseEntity<?> confirmImport(@RequestBody(required = false) List<String> torikomuAtenaNo,
			HttpSession session) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkWriteAccess(ATENA_INSERT);

		AtenaImportPreviewDto preview = (AtenaImportPreviewDto) session.getAttribute(IMPORT_PREVIEW_KEY);
		if (preview == null) {
			return badRequest("取込内容の有効期限が切れました。ファイルを選択し直してください。");
		}

		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			Set<String> selected = torikomuAtenaNo == null ? Set.of() : new HashSet<>(torikomuAtenaNo);
			atenaImportService.confirm(preview, selected, jichitaiCd, auth.getName());
			session.removeAttribute(IMPORT_PREVIEW_KEY);
			return ResponseEntity.ok(Map.of("message", "CSVファイルの取込が完了しました。"));
		} catch (RuntimeException e) {
			log.warn("CSV取込ユーザーエラー: {}", e.getMessage());
			return badRequest(e.getMessage());
		} catch (Exception e) {
			log.error("CSV取込システムエラー", e);
			return badRequest("システムエラーが発生しました。管理者にお問い合せください。");
		}
	}

	/**
	 * 取込結果の明細を取得する。
	 */
	@GetMapping("/import/detail/{seq}")
	@ResponseBody
	public ResponseEntity<?> importDetail(@PathVariable BigDecimal seq) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(ATENA_INSERT);
		return ResponseEntity.ok(atenaImportService.findDetail(jichitaiCd, seq));
	}

	private ResponseEntity<Map<String, String>> badRequest(String message) {
		return ResponseEntity.badRequest().body(Map.of("message", message));
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
