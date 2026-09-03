package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.ReportsConstants.ReportsOutputField;
import jp.lg.asp.accommodation.service.ReportsOutputConfigService;

@ExtendWith(MockitoExtension.class)
class ReportsOutputConfigControllerTest {

    private static final String JICHITAI_CD = "01100";
    private static final String SCREEN_ID = ScreenManagement.REPORTS_OUTPUT_CONFIG;

    @InjectMocks ReportsOutputConfigController controller;

    @Mock ReportsOutputConfigService reportsOutputConfigService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;
    @Mock Authentication authentication;

    // ─── view ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#1 view 正常系 照会画面表示：定義テキストのマップと mode=\"view\" がモデルに設定される")
    void 確認1_view_正常系() {
        LinkedHashMap<ReportsOutputField, String> defTextMap = new LinkedHashMap<>();
        defTextMap.put(ReportsOutputField.TOKUGIMU_SHITEI_JOREI, "第1条");
        defTextMap.put(ReportsOutputField.TOKUGIMU_JURI_JOREI, "");

        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(reportsOutputConfigService.getDefTextMap(JICHITAI_CD)).thenReturn(defTextMap);

        ExtendedModelMap model = new ExtendedModelMap();
        String result = controller.view(model);

        assertThat(result).isEqualTo("admin/reportsOutputConfig");
        assertThat(model.get("defTextMap")).isSameAs(defTextMap);
        assertThat(model.get("mode")).isEqualTo("view");
        verify(reportsOutputConfigService, times(1)).getDefTextMap(JICHITAI_CD);
        verify(accessChecker, times(1)).checkAccess(SCREEN_ID);
    }

    // ─── edit ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#2 edit 正常系 編集画面表示：定義テキストのマップと mode=\"edit\" がモデルに設定される")
    void 確認2_edit_正常系() {
        LinkedHashMap<ReportsOutputField, String> defTextMap = new LinkedHashMap<>();
        defTextMap.put(ReportsOutputField.TOKUGIMU_SHITEI_JOREI, "第1条");
        defTextMap.put(ReportsOutputField.TOKUGIMU_JURI_JOREI, "");

        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(reportsOutputConfigService.getDefTextMap(JICHITAI_CD)).thenReturn(defTextMap);

        ExtendedModelMap model = new ExtendedModelMap();
        String result = controller.edit(model);

        assertThat(result).isEqualTo("admin/reportsOutputConfig");
        assertThat(model.get("defTextMap")).isSameAs(defTextMap);
        assertThat(model.get("mode")).isEqualTo("edit");
        verify(reportsOutputConfigService, times(1)).getDefTextMap(JICHITAI_CD);
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID);
    }

    // ─── save ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("#3 save 正常系 登録成功：successMessage を積んで照会画面へリダイレクトする")
    void 確認3_save_正常系() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(authentication.getName()).thenReturn("U001");
        doNothing().when(reportsOutputConfigService).saveDefText(any(), any(), any());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("RPT0000002", "第1条");
        params.put("RPT0000003", "第2条");

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = controller.save(params, authentication, redirectAttributes);

        assertThat(result).isEqualTo("redirect:/admin/reports-output-config/view");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("帳票出力項目を登録しました。");
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("errorMessage");
        verify(reportsOutputConfigService, times(1)).saveDefText(JICHITAI_CD, "U001", params);
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#4 save 正常系 params が空の場合：空の Map がそのままサービスに渡る")
    void 確認4_save_paramsが空() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(authentication.getName()).thenReturn("U001");
        doNothing().when(reportsOutputConfigService).saveDefText(any(), any(), any());

        Map<String, String> params = new LinkedHashMap<>();

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = controller.save(params, authentication, redirectAttributes);

        assertThat(result).isEqualTo("redirect:/admin/reports-output-config/view");
        verify(reportsOutputConfigService, times(1)).saveDefText(JICHITAI_CD, "U001", params);
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("帳票出力項目を登録しました。");
    }

    @Test
    @DisplayName("#5 save 異常系 サービスが例外をスローした場合：errorMessage を積んで編集画面へリダイレクトする")
    void 確認5_save_サービス例外() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(authentication.getName()).thenReturn("U001");
        doThrow(new RuntimeException("DB error"))
                .when(reportsOutputConfigService).saveDefText(any(), any(), any());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("RPT0000002", "第1条");

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = controller.save(params, authentication, redirectAttributes);

        assertThat(result).isEqualTo("redirect:/admin/reports-output-config/edit");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("登録に失敗しました: DB error");
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("successMessage");
        verify(accessChecker, times(1)).checkWriteAccess(SCREEN_ID);
    }

    @Test
    @DisplayName("#6 save 異常系 いずれかの定義テキストが未入力（空文字・null）の場合：バリデーションエラーとなり編集画面に戻る")
    void 確認6_save_バリデーションエラー() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(authentication.getName()).thenReturn("U001");
        doNothing().when(reportsOutputConfigService).saveDefText(any(), any(), any());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("TOKUGIMU_SHITEI_JOREI", "");

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        // コントローラにバリデーション処理がある場合は編集画面に戻ること
        // 現状の実装ではサービスに委譲するため、チェックリスト通りに記述
        String result = controller.save(params, authentication, redirectAttributes);

        // チェックリスト期待値：バリデーションエラーで編集画面に戻る
        // 実装にバリデーションがない場合はこのテストは失敗する（意図的）
        assertThat(result).isEqualTo("admin/reportsOutputConfig");
        verify(reportsOutputConfigService, never()).saveDefText(any(), any(), any());
    }
}
