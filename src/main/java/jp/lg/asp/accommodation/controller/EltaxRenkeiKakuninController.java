package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto;
import jp.lg.asp.accommodation.service.EltaxRenkeiKakuninService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/eltax-renkei/kakunin")
@RequiredArgsConstructor
public class EltaxRenkeiKakuninController {

	private final EltaxRenkeiKakuninService eltaxRenkeiKakuninService;
	private final ScreenAccessChecker accessChecker;

	private static final String SCREEN_ID = ScreenManagement.ELTAX_RENKEI_KAKUNIN;
	private static final String SESSION_KEY_FILE = "eltaxUploadedFile";
	private static final String SESSION_KEY_FILE_NAME = "eltaxUploadedFileName";

	/**
	 * ファイルを受け取り、確認画面を表示する（DB未登録）
	 */
	@PostMapping("/preview")
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String preview(
			@RequestParam("file") MultipartFile file,
			HttpSession session,
			Model model,
			RedirectAttributes redirectAttributes) {

		accessChecker.checkAccess(SCREEN_ID);
		if (file.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "ファイルを選択してください。");
			return "redirect:/eltax-renkei";
		}
		try {
			EltaxRenkeiKakuninDto dto = eltaxRenkeiKakuninService.preview(file);
			session.setAttribute(SESSION_KEY_FILE, file.getBytes());
			session.setAttribute(SESSION_KEY_FILE_NAME, file.getOriginalFilename());
			model.addAttribute("kakuninDto", dto);
			return "eltaxRenkei/eltaxRenkeiKakunin";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/eltax-renkei";
		}
	}

	/**
	 * 確認後、セッションのファイルをDBへ登録する
	 */
	@PostMapping("/commit")
	@OpeLog(screenId = SCREEN_ID, operation = "取込")
	public String commit(
			@RequestParam(required = false) String atenaNo,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		accessChecker.checkAccess(SCREEN_ID);
		byte[] fileBytes = (byte[]) session.getAttribute(SESSION_KEY_FILE);
		String fileName = (String) session.getAttribute(SESSION_KEY_FILE_NAME);
		if (fileBytes == null || fileBytes.length == 0) {
			redirectAttributes.addFlashAttribute("errorMessage", "セッションが切れました。再度ファイルを選択してください。");
			return "redirect:/eltax-renkei";
		}
		try {
			BigDecimal atenaNoDecimal = null;
			if (atenaNo != null && !atenaNo.isBlank()) {
				try { atenaNoDecimal = new BigDecimal(atenaNo); } catch (NumberFormatException ignored) {}
			}
			eltaxRenkeiKakuninService.commit(fileBytes, fileName, atenaNoDecimal);
			redirectAttributes.addFlashAttribute("successMessage", "ファイルを取り込みました。");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		} finally {
			session.removeAttribute(SESSION_KEY_FILE);
			session.removeAttribute(SESSION_KEY_FILE_NAME);
		}
		return "redirect:/eltax-renkei";
	}
}
