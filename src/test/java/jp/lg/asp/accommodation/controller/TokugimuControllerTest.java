package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuListItem;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;
import jp.lg.asp.accommodation.service.NozeiShukiService;
import jp.lg.asp.accommodation.service.TokugimuService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
class TokugimuControllerTest {

    @Mock
    private TokugimuService tokugimuService;

    @Mock
    private NozeiShukiService nozeiShukiService;

    @Mock
    private ScreenAccessChecker accessChecker;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    @InjectMocks
    private TokugimuController tokugimuController;

    private MockedStatic<SessionHelper> sessionHelperMock;

    @BeforeEach
    void setUp() {
        sessionHelperMock = mockStatic(SessionHelper.class);
    }

    @AfterEach
    void tearDown() {
        sessionHelperMock.close();
    }

    @Nested
    @DisplayName("list メソッドのテスト")
    class ListTest {

        @Test
        @DisplayName("正常系：検索フラグが true のとき、検索サービスが呼び出され、取得された一覧がモデルに設定されて一覧画面が返却されること")
        void success_searchedTrue() {
            TokugimuSearchForm searchForm = new TokugimuSearchForm();
            boolean searched = true;

            TokugimuListItem item = new TokugimuListItem();
            item.setShiteiNo("00000001");

            when(tokugimuService.searchAll(searchForm)).thenReturn(List.of(item));

            String viewName = tokugimuController.list(searchForm, searched, model);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuDaicho");
            verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_DAICHO);
            verify(tokugimuService).searchAll(searchForm);
            verify(model).addAttribute("items", List.of(item));
            verify(model).addAttribute("searchForm", searchForm);
            verify(model).addAttribute("isSearched", true);
        }

        @Test
        @DisplayName("境界値：初期表示時は検索結果一覧を表示しないため、検索が行われず空のリストがモデルに設定されて一覧画面が返却されること")
        void boundary_searchedFalse() {
            TokugimuSearchForm searchForm = new TokugimuSearchForm();
            boolean searched = false;

            String viewName = tokugimuController.list(searchForm, searched, model);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuDaicho");
            verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_DAICHO);
            verify(tokugimuService, never()).searchAll(any());
            verify(model).addAttribute("items", List.of());
            verify(model).addAttribute("searchForm", searchForm);
            verify(model).addAttribute("isSearched", false);
        }
        
        @Test
        @DisplayName("正常系：個人番号を含む検索フォームが渡されたとき、そのままサービス層の searchAll へ伝達されること")
        void success_withKojinNoSearch() {
            TokugimuSearchForm searchForm = new TokugimuSearchForm();
            searchForm.setKojinNo("search_target_kojin_no"); // ダミーの検索値
            boolean searched = true;

            TokugimuListItem item = new TokugimuListItem();
            item.setShiteiNo("00000001");

            when(tokugimuService.searchAll(eq(searchForm))).thenReturn(List.of(item));

            String viewName = tokugimuController.list(searchForm, searched, model);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuDaicho");
            verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_DAICHO);
            // 検索フォーム（個人番号等）が正しくサービスへ渡されていることを検証
            verify(tokugimuService).searchAll(searchForm);
            assertThat(searchForm.getKojinNo()).isEqualTo("search_target_kojin_no");
            
            verify(model).addAttribute("items", List.of(item));
            verify(model).addAttribute("searchForm", searchForm);
            verify(model).addAttribute("isSearched", true);
        }

        @Test
        @DisplayName("正常系：各種検索条件（指定番号・法人番号等）を含む検索フォームが正しくサービス層へ伝達されること")
        void success_withVariousSearchConditions() {
            TokugimuSearchForm searchForm = new TokugimuSearchForm();
            searchForm.setShiteiNo("00000001");
            searchForm.setHojinNo("search_target_hojin_no");
            boolean searched = true;

            when(tokugimuService.searchAll(eq(searchForm))).thenReturn(List.of());

            String viewName = tokugimuController.list(searchForm, searched, model);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuDaicho");
            verify(tokugimuService).searchAll(searchForm);
            verify(model).addAttribute("isSearched", true);
        }
    }

    @Nested
    @DisplayName("showView メソッドのテスト")
    class ShowViewTest {

        @Test
        @DisplayName("正常系：セッションに指定番号が存在し、rno が指定されている場合に、履歴番号を指定してデータが取得・モデルに設定されること")
        void success_withRno() {
            Integer rno = 2;
            String shiteiNo = "00000001";
            TokugimuForm form = new TokugimuForm();
            form.setShiteiNo(shiteiNo);
            form.setName("テスト事業者");
            form.setFacilityName("テスト施設");
            form.setAtenaNo(1L);

            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(shiteiNo);
            sessionHelperMock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(null);
            sessionHelperMock.when(() -> SessionHelper.saveShiteiGassan(eq(session), any())).thenAnswer(inv -> null);

            when(tokugimuService.getTokugimuByShiteiNoAndRno(shiteiNo, rno)).thenReturn(form);

            String viewName = tokugimuController.showView(session, rno, model);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuConfig");
            verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_CONFIG);
            verify(tokugimuService).getTokugimuByShiteiNoAndRno(shiteiNo, rno);
            verify(model).addAttribute(eq("TokugimuForm"), eq(form));
            verify(model).addAttribute("isView", true);
            verify(model).addAttribute("isEdit", false);
            verify(model).addAttribute("editId", shiteiNo);
        }

        @Test
        @DisplayName("正常系：セッションに指定番号が存在し、rno が未指定の場合に、最新データが取得・モデルに設定されること")
        void success_withoutRno() {
            Integer rno = null;
            String shiteiNo = "00000001";
            TokugimuForm form = new TokugimuForm();
            form.setShiteiNo(shiteiNo);
            form.setName("テスト事業者");
            form.setFacilityName("テスト施設");
            form.setAtenaNo(1L);

            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(shiteiNo);
            sessionHelperMock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(null);
            sessionHelperMock.when(() -> SessionHelper.saveShiteiGassan(eq(session), any())).thenAnswer(inv -> null);

            when(tokugimuService.getTokugimuByShiteiNo(shiteiNo)).thenReturn(form);

            String viewName = tokugimuController.showView(session, rno, model);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuConfig");
            verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_CONFIG);
            verify(tokugimuService).getTokugimuByShiteiNo(shiteiNo);
            verify(model).addAttribute(eq("TokugimuForm"), eq(form));
            verify(model).addAttribute("isView", true);
            verify(model).addAttribute("isEdit", false);
            verify(model).addAttribute("editId", shiteiNo);
        }
    }
}