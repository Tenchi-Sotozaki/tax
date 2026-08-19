package jp.lg.asp.accommodation.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.MenuDto;
import jp.lg.asp.accommodation.entity.Menu;
import jp.lg.asp.accommodation.entity.User;
import jp.lg.asp.accommodation.entity.UserId;
import jp.lg.asp.accommodation.repository.MenuRepository;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
import jp.lg.asp.accommodation.service.GlobalModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalModelServiceImpl implements GlobalModelService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;

    @Override
    @Transactional(readOnly = true)
    public Set<String> getAccessibleScreens(String jichitaiCd, String userId) {
        try {
            UserId pk = new UserId();
            pk.setJichitaiCd(jichitaiCd);
            pk.setId(userId);

            User user = userRepository.findById(pk).orElse(null);

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
            log.error("accessibleScreens取得エラー: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuDto> buildSideMenuTree(String jichitaiCd, Set<String> screens) {
        try {
            List<Menu> menus = menuRepository.findByJichitaiCdOrderByDspOdr(jichitaiCd);
            Map<String, MenuDto> map = new LinkedHashMap<>();
            for (Menu m : menus) {
                MenuDto dto = new MenuDto();
                dto.setMenuId(m.getMenuId());
                dto.setLevel(m.getLevel());
                dto.setPMenuId(m.getPMenuId());
                dto.setName(m.getName());
                dto.setScreenId(m.getScreenId());
                dto.setIconLink(m.getIconLink());
                dto.setLink(m.getLink());
                map.put(m.getMenuId(), dto);
            }
            List<MenuDto> roots = new ArrayList<>();
            for (MenuDto dto : map.values()) {
                if (dto.getLevel() == 1) {
                    roots.add(dto);
                } else {
                    MenuDto parent = map.get(dto.getPMenuId());
                    if (parent != null) {
                        parent.getChildren().add(dto);
                    }
                }
            }
            for (MenuDto lv1 : roots) {
                for (MenuDto lv2 : lv1.getChildren()) {
                    for (MenuDto lv3 : lv2.getChildren()) {
                        lv3.getChildren().removeIf(lv4 -> !isAccessible(lv4, screens));
                    }
                    lv2.getChildren().removeIf(lv3 ->
                        !isAccessible(lv3, screens) ||
                        (lv3.getLink() == null && lv3.getChildren().isEmpty())
                    );
                }
                lv1.getChildren().removeIf(lv2 ->
                    !isAccessible(lv2, screens) ||
                    (lv2.getLink() == null && lv2.getChildren().isEmpty())
                );
            }
            roots.removeIf(lv1 -> lv1.getChildren().isEmpty());
            return roots;
        } catch (Exception e) {
            log.error("sideMenuTree取得エラー: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean isAccessible(MenuDto menu, Set<String> screens) {
        return screens.contains("*") || menu.getScreenId() == null || screens.contains(menu.getScreenId().strip());
    }
}
