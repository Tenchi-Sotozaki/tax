package jp.lg.asp.accommodation.controller;

import org.springframework.beans.factory.annotation.Value;
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

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	private static final String ATENA_DAICHO = ScreenManagement.ATENA_DAICHO;
	private static final String ATENA_INSERT = ScreenManagement.ATENA_INSERT;

	@GetMapping("/list")
	public String list(@ModelAttribute AtenaSearchForm searchForm, Model model) {
		accessChecker.checkAccess(ATENA_DAICHO);
		model.addAttribute("items", atenaRepository.search(
				jichitaiCd,
				emptyToNull(searchForm.getAtenaNo()),
				emptyToNull(searchForm.getName()),
				emptyToNull(searchForm.getNameKana()),
				emptyToNull(searchForm.getYubinNo()),
				emptyToNull(searchForm.getJusho()),
				emptyToNull(searchForm.getTel()),
				hashIfPresent(searchForm.getKojinNo()),
				emptyToNull(searchForm.getHojinNo())));
		model.addAttribute("searchForm", searchForm);
		return "atena/atenaDaicho";
	}

	@GetMapping("/import")
	public String showImport(Model model) {
		accessChecker.checkAccess(ATENA_INSERT);
		model.addAttribute("history", atenaImportService.findHistory(jichitaiCd));
		return "atena/atenaInsert";
	}

	@PostMapping("/import")
	public String importCsv(@RequestParam("file") MultipartFile file,
			RedirectAttributes redirectAttributes) {
		accessChecker.checkAccess(ATENA_INSERT);
		if (file.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "ファイルを選択してください。");
			return "redirect:/atena/import";
		}
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			atenaImportService.importCsv(file, jichitaiCd, auth.getName());
			redirectAttributes.addFlashAttribute("successMessage", "CSVファイルの取込が完了しました。");
		} catch (Exception e) {
			log.error("CSV取込エラー", e);
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/atena/import";
	}

	private String emptyToNull(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}

	private String hashIfPresent(String s) {
		return (s == null || s.isBlank()) ? null : HashUtil.sha256(s);
	}
}
