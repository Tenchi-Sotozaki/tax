package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.AppUserDetails;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.controller.InitialPasswordController;
import jp.lg.asp.accommodation.dto.UserForm;
import jp.lg.asp.accommodation.dto.UserSearchForm;
import jp.lg.asp.accommodation.entity.Role;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import jp.lg.asp.accommodation.service.AdminUserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JichitaiContext jichitaiContext;

    @Override
    public java.util.List<User> searchAll(UserSearchForm form) {

        String jichitaiCd = jichitaiContext.getJichitaiCd();

        return userRepository.searchAll(
                jichitaiCd,
                emptyToNull(form.getId()),
                toLikePattern(form.getName(), form.getNameMatchType()),
                toLikePattern(form.getNameKana(), form.getNameKanaMatchType()),
                toLikePattern(form.getBusho(), form.getBushoMatchType()),
                form.getRoleId());
    }

    @Override
    public List<Role> selectableRoles(String jichitaiCd, BigDecimal currentRoleId) {

        Long current = currentRoleId != null ? currentRoleId.longValue() : null;

        return roleRepository.findByJichitaiCdOrderByRoleId(jichitaiCd)
                .stream()
                .filter(r ->
                        r.getRoleId() == null
                        || r.getRoleId().longValue() != UserRepository.DEFAULT_USER_ROLE_ID
                        || r.getRoleId().equals(current))
                .toList();
    }

    @Override
    public User findById(String id) {

        return userRepository.findById(buildUserId(id))
                .orElseThrow(() ->
                        new RuntimeException("ユーザーが見つかりません: " + id));
    }

    @Override
    public boolean existsActiveUser(String id) {

        return userRepository.findById(buildUserId(id))
                .filter(u -> "0".equals(u.getDelFlg()))
                .isPresent();
    }

    @Override
    public void register(UserForm form) {

        User user = userRepository.findById(buildUserId(form.getId()))
                .orElse(null);

        if (user == null) {
            user = new User();
            user.setJichitaiCd(jichitaiContext.getJichitaiCd());
            user.setId(form.getId());
        } else {
            user.setDelFlg("0");
        }

        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setName(form.getName());
        user.setNameKana(form.getNameKana());
        user.setBusho(form.getBusho());
        user.setRoleId(form.getRoleId());
        user.setInitialPasswordFlg("1");

        userRepository.save(user);
    }

    @Override
    public void update(String id, UserForm form) {

        User user = findById(id);

        boolean isDefaultUser =
                InitialPasswordController.ADMIN_ID.equals(user.getId());

        user.setName(form.getName());
        user.setNameKana(form.getNameKana());
        user.setBusho(form.getBusho());

        if (!isDefaultUser) {
            user.setRoleId(form.getRoleId());
        }

        if (form.getPassword() != null
                && !form.getPassword().isBlank()) {

            user.setPassword(
                    passwordEncoder.encode(form.getPassword()));

            user.setInitialPasswordFlg("1");
        }

        userRepository.save(user);

        if (isLoginUser(id)) {
            updateSessionAuthentication(user);
        }
    }

    @Override
    public void delete(String id) {

        User user = findById(id);

        user.setDelFlg("1");

        userRepository.save(user);
    }

    @Override
    public boolean isLoginUser(String id) {

        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName()
                .equals(id);
    }

    private void updateSessionAuthentication(User user) {

        Authentication currentAuth =
                SecurityContextHolder.getContext().getAuthentication();

        AppUserDetails details =
                new AppUserDetails(
                        user.getId(),
                        user.getPassword(),
                        currentAuth.getAuthorities(),
                        "1".equals(user.getInitialPasswordFlg()));

        details.setDisplayName(user.getName());

        Authentication newAuth =
                new UsernamePasswordAuthenticationToken(
                        details,
                        details.getPassword(),
                        details.getAuthorities());

        var ctx = SecurityContextHolder.getContext();
        ctx.setAuthentication(newAuth);
        SecurityContextHolder.setContext(ctx);
    }

    private UserId buildUserId(String id) {

        UserId pk = new UserId();
        pk.setJichitaiCd(jichitaiContext.getJichitaiCd());
        pk.setId(id);

        return pk;
    }

    private String emptyToNull(String value) {

        return value == null || value.isBlank()
                ? null
                : value;
    }

    private String toLikePattern(
            String value,
            String matchType) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (matchType) {
            case "prefix" -> value + "%";
            case "exact" -> value;
            default -> "%" + value + "%";
        };
    }
}