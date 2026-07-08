package jp.lg.asp.accommodation.controller;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.exception.AccessDeniedException;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final JichitaiContext jichitaiContext;

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("selectedShiteiGassan")
    public ShiteiGassanSearchDto selectedShiteiGassan(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        return (ShiteiGassanSearchDto) session.getAttribute(ShiteiGassanSearchApiController.SESSION_KEY);
    }

    /**
     * ログインユーザーがアクセス可能な screen_id のセットをモデルに追加する。
     * サイドバーの表示制御に使用する。
     * DBにユーザーが存在しない場合（モックユーザー等）は全画面を許可する。
     */
    @ModelAttribute("accessibleScreens")
    public Set<String> accessibleScreens() {
    	String jichitaiCd = jichitaiContext.getJichitaiCd();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Collections.emptySet();
            }

            UserId pk = new UserId();
            pk.setJichitaiCd(jichitaiCd);
            pk.setId(auth.getName());

            User user = userRepository.findById(pk).orElse(null);

            // DBにユーザーが存在しない場合（モックユーザー）は全画面許可
            if (user == null || user.getRoleId() == null) {
                return Set.of("*");
            }

            return roleRepository.findByIdWithDetails(jichitaiCd, user.getRoleId().longValue())
                    .map(role -> role.getRoleDetails() == null ? Collections.<String>emptySet()
                            : role.getRoleDetails().stream()
                                    .filter(rd -> rd.getPermission() != null && rd.getPermission().compareTo("1") >= 0)
                                    .map(rd -> rd.getScreenId().strip())
                                    .collect(Collectors.toSet()))
                    .orElse(Collections.emptySet());
        } catch (Exception e) {
            log.warn("accessibleScreens取得エラー: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex) {
        log.warn("アクセス拒否: screenId={}, userId={}", ex.getScreenId(), ex.getUserId());
        return "error/403";
    }
}
