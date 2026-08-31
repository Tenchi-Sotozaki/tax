package jp.lg.asp.accommodation.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.service.InitialPasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/password-change")
@Slf4j
public class InitialPasswordController {

    private final InitialPasswordService initialPasswordService;
    private final PasswordEncoder passwordEncoder;
    private final JichitaiContext jichitaiContext;

    private static final String FORM_VIEW = "auth/initialPassword";

    public static final String ADMIN_ID = "admin_user";

    @GetMapping
    public String showForm(Authentication authentication, Model model) {
        model.addAttribute("adminId", authentication.getName());
        return FORM_VIEW;
    }

    @PostMapping
    public String changePassword(
            @RequestParam String newPassword,
            @RequestParam String newPasswordConfirm,
            Authentication authentication,
            Model model,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        model.addAttribute("adminId", authentication.getName());

        if (newPassword == null || newPassword.isBlank()) {
            model.addAttribute("error", "新しいパスワードを入力してください");
            return FORM_VIEW;
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            model.addAttribute("error", "パスワードが一致しません");
            return FORM_VIEW;
        }

        String jichitaiCd = jichitaiContext.getJichitaiCd();
        User user = initialPasswordService.findUser(jichitaiCd, authentication.getName());

        if (!"1".equals(user.getInitialPasswordFlg())) {
            model.addAttribute("error", "パスワードは設定済みです。");
            return FORM_VIEW;
        }

        if (passwordEncoder.matches(newPassword, user.getPassword() != null ? user.getPassword().trim() : "")) {
            model.addAttribute("error", "登録済みパスワードと同一のパスワードは登録できません");
            return FORM_VIEW;
        }

        initialPasswordService.changeInitialPassword(user, newPassword);

        log.info("初回パスワード変更が完了しました: userId={}, jichitaiCd={}", ADMIN_ID, jichitaiCd);

        request.getSession().invalidate();
        request.getSession(true).setAttribute("jichitaiCd", jichitaiCd);
        redirectAttributes.addFlashAttribute("successMessage", "パスワードを設定しました。設定したパスワードでログインしてください。");
        return "redirect:/login";
    }
}
