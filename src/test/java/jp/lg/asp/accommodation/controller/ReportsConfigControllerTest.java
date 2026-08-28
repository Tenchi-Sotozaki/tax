package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ReportsConfigForm;
import jp.lg.asp.accommodation.entity.KoinTorikomi;
import jp.lg.asp.accommodation.service.KoinTorikomiService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportsConfigControllerTest {

    @Mock KoinTorikomiService koinTorikomiService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks ReportsConfigController controller;

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
        when(koinTorikomiService.getImportHistory()).thenReturn(List.of());
    }

    // ===== index =====

    // No.9 正常系: 取込履歴が存在する場合、初期表示画面を返す
    @Test
    void index_取込履歴が存在する場合_初期表示画面を返す() {
        KoinTorikomi k1 = new KoinTorikomi();
        KoinTorikomi k2 = new KoinTorikomi();
        when(koinTorikomiService.getImportHistory()).thenReturn(List.of(k1, k2));
        Model model = new ExtendedModelMap();

        String view = controller.index(model);

        assertThat(view).isEqualTo("admin/reportsConfig");
        assertThat(model.asMap()).containsKey("reportsConfigForm");
        assertThat((List<?>) model.asMap().get("importHistory")).hasSize(2);
    }

    // No.10 正常系: 取込履歴が0件の場合、空リストで初期表示画面を返す
    @Test
    void index_取込履歴が0件の場合_空リストで初期表示画面を返す() {
        when(koinTorikomiService.getImportHistory()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.index(model);

        assertThat(view).isEqualTo("admin/reportsConfig");
        assertThat((List<?>) model.asMap().get("importHistory")).isEmpty();
    }

    // No.11 異常系: 書き込みアクセス権限なしの場合、例外をスロー
    @Test
    void index_書き込みアクセス権限なしの場合_例外をスロー() {
        doThrow(new RuntimeException("権限なし")).when(accessChecker).checkWriteAccess(any());
        Model model = new ExtendedModelMap();

        assertThatThrownBy(() -> controller.index(model))
                .isInstanceOf(RuntimeException.class);
    }

    // ===== importFile =====

    // No.12 正常系: 正常なPNGファイルの場合、サービス呼び出し・リダイレクト・successMessage設定
    @Test
    void importFile_正常なPNGファイルの場合_サービス呼び出しとリダイレクトとsuccessMessage設定() {
        ReportsConfigForm form = new ReportsConfigForm();
        form.setFile(new MockMultipartFile("file", "koin.png", "image/png", new byte[1024]));
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user01");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.importFile(form, redirectAttributes, auth);

        assertThat(view).isEqualTo("redirect:/admin/reports-config");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("帳票ファイルの取り込みが完了しました。");
        verify(koinTorikomiService).importReportFile(form.getFile(), "011002", "user01");
    }

    // No.13 異常系: fileがnullの場合、リダイレクト・errorMessage設定
    @Test
    void importFile_fileがnullの場合_リダイレクトとerrorMessage設定() {
        ReportsConfigForm form = new ReportsConfigForm();
        Authentication auth = mock(Authentication.class);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.importFile(form, redirectAttributes, auth);

        assertThat(view).isEqualTo("redirect:/admin/reports-config");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("ファイルを選択してください。");
        verify(koinTorikomiService, never()).importReportFile(any(), any(), any());
    }

    // No.14 異常系: fileが空（isEmpty=true）の場合、リダイレクト・errorMessage設定
    @Test
    void importFile_fileが空の場合_リダイレクトとerrorMessage設定() {
        ReportsConfigForm form = new ReportsConfigForm();
        form.setFile(new MockMultipartFile("file", new byte[0]));
        Authentication auth = mock(Authentication.class);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.importFile(form, redirectAttributes, auth);

        assertThat(view).isEqualTo("redirect:/admin/reports-config");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("ファイルを選択してください。");
        verify(koinTorikomiService, never()).importReportFile(any(), any(), any());
    }

    // No.15 境界値: ファイルサイズが10MB（10×1024×1024byte）の場合、正常に取り込まれる
    @Test
    void importFile_ファイルサイズが10MBの場合_正常に取り込まれる() {
        ReportsConfigForm form = new ReportsConfigForm();
        form.setFile(new MockMultipartFile("file", "koin.png", "image/png",
                new byte[10 * 1024 * 1024]));
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user01");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.importFile(form, redirectAttributes, auth);

        assertThat(view).isEqualTo("redirect:/admin/reports-config");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("帳票ファイルの取り込みが完了しました。");
    }

    // No.16 境界値: ファイルサイズが10MB超（10×1024×1024+1byte）の場合、リダイレクト・errorMessage設定
    @Test
    void importFile_ファイルサイズが10MB超の場合_リダイレクトとerrorMessage設定() {
        ReportsConfigForm form = new ReportsConfigForm();
        form.setFile(new MockMultipartFile("file", "koin.png", "image/png",
                new byte[10 * 1024 * 1024 + 1]));
        Authentication auth = mock(Authentication.class);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.importFile(form, redirectAttributes, auth);

        assertThat(view).isEqualTo("redirect:/admin/reports-config");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("ファイルサイズが10MBを超えています。");
        verify(koinTorikomiService, never()).importReportFile(any(), any(), any());
    }

    // No.17 異常系: contentTypeがnullの場合、リダイレクト・errorMessage設定
    @Test
    void importFile_contentTypeがnullの場合_リダイレクトとerrorMessage設定() {
        ReportsConfigForm form = new ReportsConfigForm();
        form.setFile(new MockMultipartFile("file", "koin.png", null, new byte[1024]));
        Authentication auth = mock(Authentication.class);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.importFile(form, redirectAttributes, auth);

        assertThat(view).isEqualTo("redirect:/admin/reports-config");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("PNG画像ファイルのみアップロード可能です。");
    }

    // No.18 異常系: contentTypeがPNG以外（"application/pdf"）の場合、リダイレクト・errorMessage設定
    @Test
    void importFile_contentTypeがPNG以外の場合_リダイレクトとerrorMessage設定() {
        ReportsConfigForm form = new ReportsConfigForm();
        form.setFile(new MockMultipartFile("file", "koin.pdf", "application/pdf", new byte[1024]));
        Authentication auth = mock(Authentication.class);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.importFile(form, redirectAttributes, auth);

        assertThat(view).isEqualTo("redirect:/admin/reports-config");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("PNG画像ファイルのみアップロード可能です。");
    }

    // No.19 異常系: サービスが例外をスローした場合、リダイレクト・errorMessage設定
    @Test
    void importFile_サービスが例外をスローした場合_リダイレクトとerrorMessage設定() {
        doThrow(new RuntimeException("DB error"))
                .when(koinTorikomiService).importReportFile(any(), any(), any());
        ReportsConfigForm form = new ReportsConfigForm();
        form.setFile(new MockMultipartFile("file", "koin.png", "image/png", new byte[1024]));
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user01");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.importFile(form, redirectAttributes, auth);

        assertThat(view).isEqualTo("redirect:/admin/reports-config");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .contains("帳票ファイルの取り込みに失敗しました");
    }

    // No.20 異常系: 書き込みアクセス権限なしの場合、例外をスロー
    @Test
    void importFile_書き込みアクセス権限なしの場合_例外をスロー() {
        doThrow(new RuntimeException("権限なし")).when(accessChecker).checkWriteAccess(any());
        ReportsConfigForm form = new ReportsConfigForm();
        Authentication auth = mock(Authentication.class);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        assertThatThrownBy(() -> controller.importFile(form, redirectAttributes, auth))
                .isInstanceOf(RuntimeException.class);
    }
}
