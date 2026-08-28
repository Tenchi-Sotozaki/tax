package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.AtenaDaichoItem;
import jp.lg.asp.accommodation.dto.AtenaSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.service.AtenaConfigService;
import jp.lg.asp.accommodation.service.AtenaImportService;
import jp.lg.asp.accommodation.service.AtenaService;

@ExtendWith(MockitoExtension.class)
class AtenaControllerTest {

    @Mock
    private AtenaImportService atenaImportService;

    @Mock
    private AtenaConfigService atenaConfigService;

    @Mock
    private AtenaService atenaService;

    @Mock
    private ScreenAccessChecker accessChecker;

    @Mock
    private JichitaiContext jichitaiContext;

    @InjectMocks
    private AtenaController atenaController;

    private static final String JICHITAI_CD = "123456";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    @Nested
    @DisplayName("list メソッドのテスト")
    class ListTest {

        @Test
        @DisplayName("正常系：検索条件と searched=true を指定して一覧画面を表示すること")
        void list_searchedTrue_returnsViewWithItems() {
            AtenaSearchForm searchForm = new AtenaSearchForm();
            Model model = new ConcurrentModel();

            Atena atena = new Atena();
            atena.setAtenaNo(BigDecimal.ONE);
            List<AtenaDaichoItem> expectedItems = List.of(new AtenaDaichoItem(atena, BigDecimal.valueOf(100)));

            when(atenaService.searchDaicho(JICHITAI_CD, searchForm, true)).thenReturn(expectedItems);

            String viewName = atenaController.list(searchForm, true, model);

            assertThat(viewName).isEqualTo("atena/atenaDaicho");
            assertThat(model.getAttribute("items")).isEqualTo(expectedItems);
            assertThat(model.getAttribute("searchForm")).isEqualTo(searchForm);
            assertThat(model.getAttribute("isSearched")).isEqualTo(true);
            verify(accessChecker).checkAccess(ScreenManagement.ATENA_DAICHO);
        }

        @Test
        @DisplayName("境界値：検索結果が 0件（該当データなし）の場合、空のリストが設定されて一覧画面を表示すること")
        void list_emptyResult_returnsViewWithEmptyList() {
            AtenaSearchForm searchForm = new AtenaSearchForm();
            Model model = new ConcurrentModel();

            when(atenaService.searchDaicho(JICHITAI_CD, searchForm, true)).thenReturn(List.of());

            String viewName = atenaController.list(searchForm, true, model);

            assertThat(viewName).isEqualTo("atena/atenaDaicho");
            @SuppressWarnings("unchecked")
            List<AtenaDaichoItem> items = (List<AtenaDaichoItem>) model.getAttribute("items");
            assertThat(items).isEmpty();
            assertThat(model.getAttribute("isSearched")).isEqualTo(true);
            verify(accessChecker).checkAccess(ScreenManagement.ATENA_DAICHO);
        }

        @Test
        @DisplayName("異常系：画面アクセス権限がない場合に例外がスローされること")
        void list_accessDenied_throwsException() {
            AtenaSearchForm searchForm = new AtenaSearchForm();
            Model model = new ConcurrentModel();

            org.mockito.Mockito.doThrow(new AccessDeniedException("Access Denied"))
                    .when(accessChecker).checkAccess(ScreenManagement.ATENA_DAICHO);

            assertThatThrownBy(() -> atenaController.list(searchForm, false, model))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}