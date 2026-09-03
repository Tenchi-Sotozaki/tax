package jp.lg.asp.accommodation.controller;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.service.UserPasswordChangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/user-password-change")
@Slf4j
public class UserPasswordChangeController {

	private final UserPasswordChangeService userPasswordChangeService;
	private final PasswordEncoder passwordEncoder;
	private final JichitaiContext jichitaiContext;

	private static final String VIEW = "auth/changePassword";
	private static final String SCREEN_ID = ScreenManagement.USER_PASSWORD_CHANGE;
	private static final String SESSION_KEY_FROM = "passwordChangeFrom";
	private static final String DEFAULT_BACK = "/top";

	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "画面表示")
	public String showForm(
			@RequestParam(required = false) String from,
			Authentication authentication,
			HttpSession session,
			Model model) {

		if (from != null && !from.isBlank()) {
			session.setAttribute(SESSION_KEY_FROM, from);
		}
		String backUrl = (String) session.getAttribute(SESSION_KEY_FROM);
		model.addAttribute("adminId", authentication.getName());
		model.addAttribute("backUrl", backUrl != null ? backUrl : DEFAULT_BACK);
		return VIEW;
	}

	@PostMapping
	@OpeLog(screenId = SCREEN_ID, operation = "パスワード変更")
	public String changePassword(
			@RequestParam String nowPassword,
			@RequestParam String newPassword,
			@RequestParam String newPasswordConfirm,
			Authentication authentication,
			HttpSession session,
			HttpServletResponse response,
			Model model) throws IOException {

		model.addAttribute("adminId", authentication.getName());
		String backUrl = (String) session.getAttribute(SESSION_KEY_FROM);
		model.addAttribute("backUrl", backUrl != null ? backUrl : DEFAULT_BACK);

		String jichitaiCd = jichitaiContext.getJichitaiCd();
		User user = userPasswordChangeService.findUser(jichitaiCd, authentication.getName());

		if (!passwordEncoder.matches(nowPassword, user.getPassword() != null ? user.getPassword().trim() : "")) {
			model.addAttribute("error", "現在のパスワードが正しくありません");
			return VIEW;
		}
		if (newPassword == null || newPassword.isBlank()) {
			model.addAttribute("error", "新しいパスワードを入力してください");
			return VIEW;
		}
		if (!newPassword.equals(newPasswordConfirm)) {
			model.addAttribute("error", "新しいパスワードが一致しません");
			return VIEW;
		}
		if (passwordEncoder.matches(newPassword, user.getPassword() != null ? user.getPassword().trim() : "")) {
			model.addAttribute("error", "登録済みパスワードと同一のパスワードは登録できません");
			return VIEW;
		}

		userPasswordChangeService.changePassword(user, newPassword);

		log.info("パスワード変更が完了しました: userId={}, jichitaiCd={}", authentication.getName(), jichitaiCd);

		session.removeAttribute(SESSION_KEY_FROM);
		session.setAttribute("flashSuccessMessage", "パスワードを変更しました。");
		response.sendRedirect(backUrl != null ? backUrl : DEFAULT_BACK);
		return null;
	}
}
