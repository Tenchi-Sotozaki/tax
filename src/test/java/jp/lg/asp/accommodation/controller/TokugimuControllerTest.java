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
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
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
    
    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

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
    @DisplayName("showRegistrationForm メソッドのテスト")
    class ShowRegistrationFormTest {

        @Test
        @DisplayName("正常系：書き込み権限がある場合に、空のフォームと編集フラグ（false）がモデルに設定され、登録画面が返却されること")
        void success() {
            String viewName = tokugimuController.showRegistrationForm(model);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuConfig");
            verify(accessChecker).checkWriteAccess(ScreenManagement.TOKUGIMU_CONFIG);
            verify(model).addAttribute(eq("TokugimuForm"), any(TokugimuForm.class));
            verify(model).addAttribute("isEdit", false);
        }
    }

    @Nested
    @DisplayName("register メソッドのテスト")
    class RegisterTest {

        @Test
        @DisplayName("正常系：入力エラーがなく登録処理が成功した場合に、メッセージが設定され一覧画面へリダイレクトされること")
        void success() {
            TokugimuForm form = new TokugimuForm();
            when(bindingResult.hasErrors()).thenReturn(false);

            String viewName = tokugimuController.register(form, bindingResult, session, model, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/tokugimu/list");
            verify(accessChecker).checkWriteAccess(ScreenManagement.TOKUGIMU_CONFIG);
            verify(tokugimuService).register(form);
            verify(redirectAttributes).addFlashAttribute("successMessage", "登録が完了しました。");
        }

        @Test
        @DisplayName("境界値：入力値にバリデーションエラーが存在する場合に、エラーメッセージが構築されて登録画面が再表示されること")
        void boundary_validationError() {
            TokugimuForm form = new TokugimuForm();
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getFieldErrors()).thenReturn(List.of());

            String viewName = tokugimuController.register(form, bindingResult, session, model, redirectAttributes);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuConfig");
            verify(accessChecker).checkWriteAccess(ScreenManagement.TOKUGIMU_CONFIG);
            verify(model).addAttribute("isEdit", false);
            verify(model).addAttribute(eq("validationErrors"), anyList());
            verify(tokugimuService, never()).register(any());
        }

        @Test
        @DisplayName("異常系：サービス層の登録処理で例外が発生した場合に、エラーログが出力され、エラーメッセージと共に登録画面が再表示されること")
        void exception_serviceError() {
            TokugimuForm form = new TokugimuForm();
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("登録失敗")).when(tokugimuService).register(form);
            when(nozeiShukiService.findAll()).thenReturn(List.of());

            String viewName = tokugimuController.register(form, bindingResult, session, model, redirectAttributes);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuConfig");
            verify(accessChecker).checkWriteAccess(ScreenManagement.TOKUGIMU_CONFIG);
            verify(model).addAttribute("isEdit", false);
            verify(model).addAttribute("errorMessage", "登録失敗");
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
    
    @Nested
    @DisplayName("showEditForm メソッドのテスト")
    class ShowEditFormTest {

        @Test
        @DisplayName("正常系：書き込み権限があり、セッションに指定番号が存在する場合に、対象データが取得され編集画面が返却されること")
        void success() {
            String shiteiNo = "00000001";
            TokugimuForm form = new TokugimuForm();
            form.setShiteiNo(shiteiNo);

            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(shiteiNo);
            sessionHelperMock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(null);
            when(tokugimuService.getTokugimuByShiteiNo(shiteiNo)).thenReturn(form);

            String viewName = tokugimuController.showEditForm(session, model);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuConfig");
            verify(accessChecker).checkWriteAccess(ScreenManagement.TOKUGIMU_CONFIG);
            verify(model).addAttribute("isEdit", true);
            verify(model).addAttribute("editId", shiteiNo);
        }

        @Test
        @DisplayName("境界値：セッションに指定番号が存在しない場合に、指定番号選択モーダルを開いた状態で画面が返却されること")
        void boundary_noShiteiNo() {
            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(null);

            String viewName = tokugimuController.showEditForm(session, model);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuConfig");
            verify(model).addAttribute("showShiteiGassanModal", true);
        }
    }

    @Nested
    @DisplayName("update メソッドのテスト")
    class UpdateTest {

        @Test
        @DisplayName("正常系：セッションに指定番号が存在し、バリデーションエラーがない場合に更新処理が成功し、一覧画面へリダイレクトされること")
        void success() {
            String shiteiNo = "00000001";
            TokugimuForm form = new TokugimuForm();

            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(shiteiNo);
            when(bindingResult.hasErrors()).thenReturn(false);

            String viewName = tokugimuController.update(session, form, bindingResult, model, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/tokugimu/list");
            verify(accessChecker).checkWriteAccess(ScreenManagement.TOKUGIMU_CONFIG);
            verify(tokugimuService).updateByShiteiNo(shiteiNo, form);
            verify(redirectAttributes).addFlashAttribute("successMessage", "更新が完了しました。");
        }

        @Test
        @DisplayName("境界値：バリデーションエラーがある場合に、エラーメッセージが構築されて編集画面が再表示されること")
        void boundary_validationError() {
            String shiteiNo = "00000001";
            TokugimuForm form = new TokugimuForm();

            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(shiteiNo);
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getFieldErrors()).thenReturn(List.of());

            String viewName = tokugimuController.update(session, form, bindingResult, model, redirectAttributes);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuConfig");
            verify(accessChecker).checkWriteAccess(ScreenManagement.TOKUGIMU_CONFIG);
            verify(model).addAttribute("isEdit", true);
            verify(model).addAttribute("editId", shiteiNo);
            verify(tokugimuService, never()).updateByShiteiNo(any(), any());
        }

        @Test
        @DisplayName("異常系：更新時にセッションの指定番号がロストしている場合に、選択モーダル付きの画面が返却されること")
        void exception_noShiteiNo() {
            TokugimuForm form = new TokugimuForm();
            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(null);

            String viewName = tokugimuController.update(session, form, bindingResult, model, redirectAttributes);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuConfig");
            verify(model).addAttribute("showShiteiGassanModal", true);
        }
    }

    @Nested
    @DisplayName("showReport メソッドのテスト")
    class ShowReportTest {

        @Test
        @DisplayName("正常系：セッションに指定番号が存在する場合に、該当データが取得されて帳票出力画面が返却されること")
        void success() {
            String shiteiNo = "00000001";
            TokugimuForm form = new TokugimuForm();
            form.setName("テスト名");
            form.setFacilityName("テスト施設");

            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(shiteiNo);
            sessionHelperMock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(null);
            when(tokugimuService.getTokugimuByShiteiNo(shiteiNo)).thenReturn(form);

            String viewName = tokugimuController.showReport(session, model);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuReport");
            verify(accessChecker).checkAccess(ScreenManagement.TOKUGIMU_REPORT);
            verify(model).addAttribute("shiteiNo", shiteiNo);
        }

        @Test
        @DisplayName("境界値：セッションに指定番号が存在しない場合に、指定番号選択モーダルを開いた状態で帳票画面が返却されること")
        void boundary_noShiteiNo() {
            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(null);

            String viewName = tokugimuController.showReport(session, model);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuReport");
            verify(model).addAttribute("showShiteiGassanModal", true);
        }
    }

    @Nested
    @DisplayName("showGassanReport メソッドのテスト")
    class ShowGassanReportTest {

        @Test
        @DisplayName("正常系：セッションに有効な合算指定番号の情報が存在する場合に、合算通知書のパスへリダイレクトされること")
        void success() {
            ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
            dto.setGassanShiteiNo("00000002");

            sessionHelperMock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(dto);

            String viewName = tokugimuController.showGassanReport(session, model, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/reports/gassanNonyuTsuchi");
        }

        @Test
        @DisplayName("異常系：合算情報が存在しない、または合算指定番号が空の場合に、エラーメッセージが設定されて帳票画面へリダイレクトされること")
        void exception_invalidGassan() {
            sessionHelperMock.when(() -> SessionHelper.getShiteiGassan(session)).thenReturn(null);

            String viewName = tokugimuController.showGassanReport(session, model, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/tokugimu/report");
            verify(redirectAttributes).addFlashAttribute("errorMessage", "合算対象外の特別徴収義務者です");
        }
    }

    @Nested
    @DisplayName("delete メソッドのテスト")
    class DeleteTest {

        @Test
        @DisplayName("正常系：削除対象の最新履歴を削除した際、過去の履歴が残る場合に、最新履歴の照会画面へリダイレクトされること")
        void success_historyRemains() {
            String shiteiNo = "00000001";
            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(shiteiNo);
            when(tokugimuService.deleteByShiteiNo(shiteiNo)).thenReturn(true);

            String viewName = tokugimuController.delete(session, model, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/tokugimu/view");
            verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("最新履歴を削除しました"));
        }

        @Test
        @DisplayName("境界値：すべての履歴が削除され履歴が残らない場合に、セッションの合算情報がクリアされ一覧画面へリダイレクトされること")
        void boundary_noHistory() {
            String shiteiNo = "00000001";
            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(shiteiNo);
            when(tokugimuService.deleteByShiteiNo(shiteiNo)).thenReturn(false);

            String viewName = tokugimuController.delete(session, model, redirectAttributes);

            assertThat(viewName).isEqualTo("redirect:/tokugimu/list");
            sessionHelperMock.verify(() -> SessionHelper.saveShiteiGassan(session, null));
        }

        @Test
        @DisplayName("異常系：削除実行時にセッションの指定番号が保持されていない場合に、選択モーダル付きのフォーム画面が返却されること")
        void exception_noShiteiNo() {
            sessionHelperMock.when(() -> SessionHelper.getShiteiNo(session)).thenReturn(null);

            String viewName = tokugimuController.delete(session, model, redirectAttributes);

            assertThat(viewName).isEqualTo("tokugimu/tTokugimuConfig");
            verify(model).addAttribute("showShiteiGassanModal", true);
        }
    }

}