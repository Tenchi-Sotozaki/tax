package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.MenuDto;
<<<<<<< HEAD
import jp.lg.asp.accommodation.service.GlobalModelService;
=======
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Menu;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.MenuRepository;
import jp.lg.asp.accommodation.repository.RoleRepository;
import jp.lg.asp.accommodation.repository.UserRepository;
>>>>>>> master

@ExtendWith(MockitoExtension.class)
class GlobalModelAdviceTest {

<<<<<<< HEAD
	@Mock GlobalModelService globalModelService;
	@Mock JichitaiContext jichitaiContext;
=======
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock MenuRepository menuRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock JichitaiContext jichitaiContext;
>>>>>>> master

	@InjectMocks GlobalModelAdvice advice;

<<<<<<< HEAD
	@Test
	void sideMenuTree_空のメニューは空リストを返す() {
		when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
		when(globalModelService.buildSideMenuTree(eq("00001"), any())).thenReturn(List.of());

		assertThat(advice.sideMenuTree()).isEmpty();
	}
=======
    private Method isAccessible;
    private Method isDspKbnVisible;

    @BeforeEach
    void setUp() throws Exception {
        isAccessible = GlobalModelAdvice.class.getDeclaredMethod("isAccessible", MenuDto.class, Set.class);
        isAccessible.setAccessible(true);
        isDspKbnVisible = GlobalModelAdvice.class.getDeclaredMethod("isDspKbnVisible", String.class, boolean.class, boolean.class);
        isDspKbnVisible.setAccessible(true);
    }
>>>>>>> master

	@Test
	void sideMenuTree_サービスが返したリストをそのまま返す() {
		MenuDto dto = new MenuDto();
		dto.setMenuId("lv1");
		when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
		when(globalModelService.buildSideMenuTree(eq("00001"), any())).thenReturn(List.of(dto));

<<<<<<< HEAD
		List<MenuDto> result = advice.sideMenuTree();
=======
    private boolean invokeIsDspKbnVisible(String dspKbn, boolean isOperator, boolean isMonthly) throws Exception {
        return (boolean) isDspKbnVisible.invoke(advice, dspKbn, isOperator, isMonthly);
    }

    // --- isAccessible ---
>>>>>>> master

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMenuId()).isEqualTo("lv1");
	}

	@Test
	void accessibleScreens_未認証は空セットを返す() {
		when(jichitaiContext.getJichitaiCd()).thenReturn("00001");

		Set<String> result = advice.accessibleScreens();

<<<<<<< HEAD
		assertThat(result).isEmpty();
		verifyNoInteractions(globalModelService);
	}
=======
    @Test
    void isAccessible_screensに含まれない場合false() throws Exception {
        assertThat(invokeIsAccessible(menuDto("m1", "sc00000001"), Set.of("sc00000002"))).isFalse();
    }

    @Test
    void isAccessible_screenIdに空白がある場合stripして判定() throws Exception {
        assertThat(invokeIsAccessible(menuDto("m1", "sc00000001  "), Set.of("sc00000001"))).isTrue();
    }

    // --- isDspKbnVisible ---

    @Test
    void isDspKbnVisible_1は常にtrue() throws Exception {
        assertThat(invokeIsDspKbnVisible("1", false, false)).isTrue();
        assertThat(invokeIsDspKbnVisible("1", true, true)).isTrue();
    }

    @Test
    void isDspKbnVisible_2はisMonthlyがtrueの場合のみtrue() throws Exception {
        assertThat(invokeIsDspKbnVisible("2", false, true)).isTrue();
        assertThat(invokeIsDspKbnVisible("2", false, false)).isFalse();
        assertThat(invokeIsDspKbnVisible("2", true, false)).isFalse();
    }

    @Test
    void isDspKbnVisible_3はisOperatorがtrueの場合のみtrue() throws Exception {
        assertThat(invokeIsDspKbnVisible("3", true, false)).isTrue();
        assertThat(invokeIsDspKbnVisible("3", false, false)).isFalse();
        assertThat(invokeIsDspKbnVisible("3", false, true)).isFalse();
    }

    @Test
    void isDspKbnVisible_不正値はfalse() throws Exception {
        assertThat(invokeIsDspKbnVisible("9", false, false)).isFalse();
        assertThat(invokeIsDspKbnVisible(null, true, true)).isFalse();
    }

    // --- sideMenuTree ---

    @Test
    void sideMenuTree_空のメニューは空リストを返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
        when(jichitaiRepository.findById("00001")).thenReturn(Optional.of(jichitai("00001", null)));
        when(menuRepository.findAllOrderByDspOdr()).thenReturn(List.of());
        assertThat(advice.sideMenuTree()).isEmpty();
    }

    @Test
    void sideMenuTree_ツリー構造が正しく構築される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
        when(jichitaiRepository.findById("00001")).thenReturn(Optional.of(jichitai("00001", null)));
        when(menuRepository.findAllOrderByDspOdr()).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1, "1"),
            menu("lv2", 2, "lv1", null, null, 2, "1"),
            menu("lv3", 3, "lv2", null, "/path", 3, "1")
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
        when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
        when(jichitaiRepository.findById("00001")).thenReturn(Optional.of(jichitai("00001", null)));
        when(menuRepository.findAllOrderByDspOdr()).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1, "1"),
            menu("lv2", 2, "lv1", null, null, 2, "1"),
            menu("lv3", 3, "lv2", "sc00000001", "/path", 3, "1")
        ));
        // 認証なし→screensがemptySet→lv3除去→lv2の子が空→lv2除去→lv1の子が空→lv1除去
        assertThat(advice.sideMenuTree()).isEmpty();
    }

    @Test
    void sideMenuTree_screenIdがnullのlv2直接リンクは残る() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
        when(jichitaiRepository.findById("00001")).thenReturn(Optional.of(jichitai("00001", null)));
        when(menuRepository.findAllOrderByDspOdr()).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1, "1"),
            menu("lv2", 2, "lv1", null, "/path", 2, "1")
        ));
        List<MenuDto> result = advice.sideMenuTree();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getLink()).isEqualTo("/path");
    }

    @Test
    void sideMenuTree_screenIdがnullのlv3は認証なしでも残る() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
        when(jichitaiRepository.findById("00001")).thenReturn(Optional.of(jichitai("00001", null)));
        when(menuRepository.findAllOrderByDspOdr()).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1, "1"),
            menu("lv2", 2, "lv1", null, null, 2, "1"),
            menu("lv3", 3, "lv2", null, "/path", 3, "1")
        ));
        List<MenuDto> result = advice.sideMenuTree();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getChildren()).hasSize(1);
    }

    @Test
    void sideMenuTree_アクセス可能なlv3が1件でもあればlv2とlv1は残る() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
        when(jichitaiRepository.findById("00001")).thenReturn(Optional.of(jichitai("00001", null)));
        when(menuRepository.findAllOrderByDspOdr()).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1, "1"),
            menu("lv2", 2, "lv1", null, null, 2, "1"),
            menu("lv3a", 3, "lv2", "sc00000001", "/path1", 3, "1"),
            menu("lv3b", 3, "lv2", null, "/path2", 4, "1")
        ));
        // lv3bはscreenIdがnull→残る→lv2もlv1も残る
        List<MenuDto> result = advice.sideMenuTree();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).getChildren().get(0).getMenuId()).isEqualTo("lv3b");
    }

    @Test
    void sideMenuTree_dspKbn2は納税周期1の自治体のみ表示() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
        when(jichitaiRepository.findById("00001")).thenReturn(Optional.of(jichitai("00001", "1")));
        when(menuRepository.findAllOrderByDspOdr()).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1, "1"),
            menu("lv2", 2, "lv1", null, "/path", 2, "2")
        ));
        List<MenuDto> result = advice.sideMenuTree();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren()).hasSize(1);
    }

    @Test
    void sideMenuTree_dspKbn2は納税周期1以外の自治体では非表示() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
        when(jichitaiRepository.findById("00001")).thenReturn(Optional.of(jichitai("00001", "3")));
        when(menuRepository.findAllOrderByDspOdr()).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1, "1"),
            menu("lv2", 2, "lv1", null, "/path", 2, "2")
        ));
        assertThat(advice.sideMenuTree()).isEmpty();
    }

    @Test
    void sideMenuTree_dspKbn3は運用者アカウントのみ表示() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("99999");
        when(jichitaiRepository.findById("99999")).thenReturn(Optional.of(jichitai("99999", null)));
        when(menuRepository.findAllOrderByDspOdr()).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1, "1"),
            menu("lv2", 2, "lv1", "sc00000001", "/path", 2, "3")
        ));
        // 運用者はscreens=*なのでscreenIdがあっても除去されない
        List<MenuDto> result = advice.sideMenuTree();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren()).hasSize(1);
    }

    @Test
    void sideMenuTree_dspKbn3は一般自治体では非表示() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
        when(jichitaiRepository.findById("00001")).thenReturn(Optional.of(jichitai("00001", null)));
        when(menuRepository.findAllOrderByDspOdr()).thenReturn(List.of(
            menu("lv1", 1, "lv1", null, null, 1, "1"),
            menu("lv2", 2, "lv1", null, "/path", 2, "3")
        ));
        assertThat(advice.sideMenuTree()).isEmpty();
    }

    // --- helpers ---

    private MenuDto menuDto(String menuId, String screenId) {
        MenuDto dto = new MenuDto();
        dto.setMenuId(menuId);
        dto.setScreenId(screenId);
        return dto;
    }

    private Menu menu(String menuId, int level, String pMenuId, String screenId, String link, int dspOdr, String dspKbn) {
        Menu m = new Menu();
        m.setMenuId(menuId);
        m.setLevel(level);
        m.setPMenuId(pMenuId);
        m.setName(menuId);
        m.setScreenId(screenId);
        m.setLink(link);
        m.setDspOdr(dspOdr);
        m.setDspKbn(dspKbn);
        return m;
    }

    private Jichitai jichitai(String jichitaiCd, String nozeiShuki) {
        Jichitai j = new Jichitai();
        j.setJichitaiCd(jichitaiCd);
        j.setNozeiShuki(nozeiShuki);
        return j;
    }
>>>>>>> master
}
