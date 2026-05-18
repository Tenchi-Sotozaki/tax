package jp.lg.asp.accommodation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.exception.AccessDeniedException;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * 画面アクセス権限判定クラス。
 * 各Controllerのメソッド先頭で checkAccess(screenId) を呼ぶこと。
 *
 * 権限テーブル（m_role_dtl）に対象ユーザーのrole_idと画面IDの組み合わせが
 * permission >= 1 で存在しない場合、AccessDeniedException をスローする。
 */
@Component
@RequiredArgsConstructor
public class ScreenAccessChecker {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    /**
     * ログイン中ユーザーが指定画面にアクセス可能か検証する。
     * アクセス不可の場合は AccessDeniedException をスローする。
     *
     * @param screenId 画面ID（m_screen.screen_id）
     */
    public void checkAccess(String screenId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        UserId pk = new UserId();
        pk.setJichitaiCd(jichitaiCd);
        pk.setId(userId);

        User user = userRepository.findById(pk).orElse(null);

        // DBにユーザーが存在しない場合（モックユーザー等）はチェックをスキップ
        if (user == null || user.getRoleId() == null) {
            return;
        }

        long count = roleRepository.countAccessibleScreen(
                jichitaiCd, user.getRoleId().longValue(), screenId.strip());

        if (count == 0) {
            throw new AccessDeniedException(screenId, userId);
        }
    }
}
