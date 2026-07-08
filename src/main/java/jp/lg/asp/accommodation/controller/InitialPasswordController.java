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
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/password-change")
@Slf4j
public class InitialPasswordController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JichitaiContext jichitaiContext; // ★変更: @Value フィールドを削除し、これを追加

    private static final String FORM_VIEW = "auth/initialPassword";

    /** デフォルトユーザーのID（AdminUserControllerの権限変更不可判定でも使用） */
    public static final String ADMIN_ID = "admin";

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

        String jichitaiCd = jichitaiContext.getJichitaiCd(); // ★変更: セッションから取得（invalidateする前に取得しておく）

        UserId pk = new UserId();
        pk.setJichitaiCd(jichitaiCd);
        pk.setId(authentication.getName());
        User user = userRepository.findById(pk)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

        // 初期パスワード状態でなければ受け付けない（設定済みパスワードの上書き防止）
        if (!"1".equals(user.getInitialPasswordFlg())) {
            model.addAttribute("error", "パスワードは設定済みです。");
            return FORM_VIEW;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setInitialPasswordFlg("0");
        userRepository.save(user);


        log.info("初回パスワード変更が完了しました: userId={}, jichitaiCd={}", ADMIN_ID, jichitaiCd);

        // セッションを破棄すると自治体コードも消えるため、新しいセッションに引き継ぐ
        request.getSession().invalidate();
        request.getSession(true).setAttribute("jichitaiCd", jichitaiCd);
        redirectAttributes.addFlashAttribute("successMessage", "パスワードを設定しました。設定したパスワードでログインしてください。");
        return "redirect:/login";
    }
}