package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.MenuDto;
import jp.lg.asp.accommodation.entity.Menu;
import jp.lg.asp.accommodation.repository.MenuRepository;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class GlobalModelAdviceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock MenuRepository menuRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks GlobalModelAdvice advice;

    private Method isAccessible;

    @BeforeEach
    void setUp() throws Exception {
        when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
        isAccessible = GlobalModelAdvice.class.getDeclaredMethod("isAccessible", MenuDto.class, Set.class);
        isAccessible.setAccessible(true);
    }

    private boolean invokeIsAccessible(MenuDto menu, Set<String> screens) throws Exception {
        return (boolean) isAccessible.invoke(advice, menu, screens);
    }

    // --- isAccessible ---

    @Test
    void isAccessible_ワイルドカードは常にtrue() throws Exception {
        assertThat(invokeIsAccessible(menuDto("m1", null), Set.of("*"))).isTrue();
    }

    @Test
    void isAccessible_screenIdがヌルは常にtrue() throws Exception {
        assertThat(invokeIsAccessible(menuDto("m1", null), Set.of("sc00000001"))).isTrue();
    }

    @Test
    void isAccessible_screensに含まれる場合true() throws Exception {
        assertThat(invokeIsAccessible(menuDto("m1", "sc00000001"), Set.of("sc00000001"))).isTrue();
    }

    @Test
    void isAccessible_screensに含まれない場合false() throws Exception {
        assertThat(invokeIsAccessible(menuDto("m1", "sc00000001"), Set.of("sc00000002"))).isFalse();
    }

    @Test
    void isAccessible_screenIdに空白がある場合stripして判定() throws Exception {
        assertThat(invokeIsAccessible(menuDto("m1", "sc00000001  "), Set.of("sc00000001"))).isTrue();
    }

    // --- sideMenuTree ---

    @Test
    void sideMenuTree_空のメニューは空リストを返す() {
        when(menuRepository.findByJichitaiCdOrderByDspOdr("00001")).thenReturn(List.of());
        assertThat(advice.sideMenuTree()).isEmpty();
    }

    @Test
    void sideMenuTree_ツリー構造が正しく構築される() {
        when(menuRepository.findByJichitaiCdOrderByDspOdr("00001")).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1),
            menu("lv2", 2, "lv1", null, null, 2),
            menu("lv3", 3, "lv2", null, "/path", 3)
        ));
        List<MenuDto> result = advice.sideMenuTree();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMenuId()).isEqualTo("lv1");
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getChildren().get(0).getLink()).isEqualTo("/path");
    }

    @Test
    void sideMenuTree_lv3が全除去されたlv2はlv1も連鎖除去される() {
        when(menuRepository.findByJichitaiCdOrderByDspOdr("00001")).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1),
            menu("lv2", 2, "lv1", null, null, 2),
            menu("lv3", 3, "lv2", "sc00000001", "/path", 3)
        ));
        // 認証なし→screensがemptySet→lv3除去→lv2の子が空→lv2除去→lv1の子が空→lv1除去
        assertThat(advice.sideMenuTree()).isEmpty();
    }

    @Test
    void sideMenuTree_screenIdがnullのlv2直接リンクは残る() {
        when(menuRepository.findByJichitaiCdOrderByDspOdr("00001")).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1),
            menu("lv2", 2, "lv1", null, "/path", 2)
        ));
        List<MenuDto> result = advice.sideMenuTree();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getLink()).isEqualTo("/path");
    }

    @Test
    void sideMenuTree_screenIdがnullのlv3は認証なしでも残る() {
        when(menuRepository.findByJichitaiCdOrderByDspOdr("00001")).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1),
            menu("lv2", 2, "lv1", null, null, 2),
            menu("lv3", 3, "lv2", null, "/path", 3)
        ));
        List<MenuDto> result = advice.sideMenuTree();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getChildren()).hasSize(1);
    }

    @Test
    void sideMenuTree_アクセス可能なlv3が1件でもあればlv2とlv1は残る() {
        when(menuRepository.findByJichitaiCdOrderByDspOdr("00001")).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1),
            menu("lv2", 2, "lv1", null, null, 2),
            menu("lv3a", 3, "lv2", "sc00000001", "/path1", 3),
            menu("lv3b", 3, "lv2", null, "/path2", 4)
        ));
        // lv3bはscreenIdがnull→残る→lv2もlv1も残る
        List<MenuDto> result = advice.sideMenuTree();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getChildren().get(0).getMenuId()).isEqualTo("lv3b");
    }

    // --- helpers ---

    private MenuDto menuDto(String menuId, String screenId) {
        MenuDto dto = new MenuDto();
        dto.setMenuId(menuId);
        dto.setScreenId(screenId);
        return dto;
    }

    private Menu menu(String menuId, int level, String pMenuId, String screenId, String link, int dspOdr) {
        Menu m = new Menu();
        m.setMenuId(menuId);
        m.setLevel(level);
        m.setPMenuId(pMenuId);
        m.setName(menuId);
        m.setScreenId(screenId);
        m.setLink(link);
        m.setDspOdr(dspOdr);
        return m;
    }
}
