package jp.lg.asp.accommodation.config;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.RoleForm;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import jp.lg.asp.accommodation.service.RoleService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminSeedRunner implements ApplicationRunner {

    // 画面ID一覧（seed_admin_role.sql と同じ値）
    private static final String[] ALL_SCREEN_IDS = {
        "ms00000001", "ms00000008", "mt00000001", "ms00000002", "ms00000005",
        "ms00000004", "ms00000003", "ss00000002", "sc00000004", "ss00000005",
        "sc00000003", "mt00000002", "mt00000003", "mi00000001", "ms00000006",
        "mo00000001", "mo00000003", "sc00000002", "ms00000009", "ms00000010",
        "ms00000011", "ms00000012", "ms00000007", "ms00000013", "ms00000014",
        "ms00000015", "ms00000022", "ms00000016", "ms00000017", "ms00000018",
        "ms00000019", "ms00000020", "ms00000021", "ms00000023", "sc00000005",
        "ms00000024", "sc00000006", "ss00000006", "sc00000007", "ss00000007",
        "ss00000008"
    };

    private static final String ADMIN_ID = "admin";
    private static final String INITIAL_PASSWORD = "ChangeMe0000"; // TODO: 運用ルール確定後に見直す

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        UserId adminUserId = new UserId();
        adminUserId.setJichitaiCd(jichitaiCd);
        adminUserId.setId(ADMIN_ID);
        boolean alreadyExists = userRepository.findById(adminUserId).isPresent();
        if (alreadyExists) {
            return;
        }

        // 1. 全権限ロールを既存のRoleService.saveRole()経由で作成
        RoleForm roleForm = new RoleForm();
        roleForm.setRoleId(null); // null = 新規作成。DBが空ならroleId=1が採番される
        roleForm.setName("管理者");
        roleForm.setScreenPermissions(buildFullPermissionMap());
        roleService.saveRole(roleForm, jichitaiCd, "SYSTEM");

        Long createdRoleId = roleRepository.findMaxRoleIdByJichitaiCd(jichitaiCd);

        // 2. Adminユーザーを作成
        User admin = new User();
        admin.setJichitaiCd(jichitaiCd);
        admin.setId(ADMIN_ID);
        admin.setName("システム管理者");
        admin.setNameKana("しすてむかんりしゃ");
        admin.setBusho("システム管理課");
        admin.setRoleId(BigDecimal.valueOf(createdRoleId));
        admin.setPassword(passwordEncoder.encode(INITIAL_PASSWORD));
        admin.setInitialPasswordFlg("1");
        admin.setAddUser("SYSTEM");
        admin.setUpdUser("SYSTEM");
        userRepository.save(admin);
    }

    private Map<String, Integer> buildFullPermissionMap() {
        Map<String, Integer> permissions = new LinkedHashMap<>();
        for (String screenId : ALL_SCREEN_IDS) {
            permissions.put(screenId, 2); // 2 = 更新権限
        }
        return permissions;
    }
}
