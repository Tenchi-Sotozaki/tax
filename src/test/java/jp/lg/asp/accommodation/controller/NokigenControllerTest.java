package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.service.NokigenService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NokigenControllerTest {

    @Mock NokigenService nokigenService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks NokigenController controller;

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
        when(nokigenService.findAll()).thenReturn(List.of());
    }

    // ===== register =====

    // No.25 正常系: 年度開始月が設定済みの場合、nokigenConfig画面を返す
    @Test
    void register_年度開始月が設定済みの場合_nokigenConfig画面を返す() {
        Jichitai jichitai = new Jichitai();
        jichitai.setNendoStMonth("4");
        when(nokigenService.findJichitai("011002")).thenReturn(jichitai);
        Model model = new ExtendedModelMap();

        String view = controller.register(model);

        assertThat(view).isEqualTo("admin/nokigenConfig");
        assertThat(model.asMap()).containsKey("nokigen");
        assertThat(model.asMap().get("mode")).isEqualTo("register");
        assertThat(model.asMap()).containsKey("kiMonthLabels");
    }

    // No.26 正常系: jichitaiがnullの場合、kiMonthLabelsが設定されずnokigenConfig画面を返す
    @Test
    void register_jichitaiがnullの場合_kiMonthLabelsなしでnokigenConfig画面を返す() {
        when(nokigenService.findJichitai("011002")).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.register(model);

        assertThat(view).isEqualTo("admin/nokigenConfig");
        assertThat(model.asMap()).doesNotContainKey("kiMonthLabels");
    }

    // No.27 正常系: nendoStMonthが未設定の場合、warnMessageを設定してnokigenConfig画面を返す
    @Test
    void register_nendoStMonthが未設定の場合_warnMessageを設定してnokigenConfig画面を返す() {
        Jichitai jichitai = new Jichitai();
        jichitai.setNendoStMonth("");
        when(nokigenService.findJichitai("011002")).thenReturn(jichitai);
        Model model = new ExtendedModelMap();

        String view = controller.register(model);

        assertThat(view).isEqualTo("admin/nokigenConfig");
        assertThat(model.asMap().get("warnMessage")).isEqualTo("年度開始月が未設定です。");
    }

    // No.28 異常系: 書き込み権限なしの場合、例外をスロー
    @Test
    void register_書き込み権限なしの場合_例外をスロー() {
        doThrow(new RuntimeException("権限なし")).when(accessChecker).checkWriteAccess(any());

        assertThatThrownBy(() -> controller.register(new ExtendedModelMap()))
                .isInstanceOf(RuntimeException.class);
    }

    // ===== edit =====

    // No.33 正常系: 指定年度が存在する場合、nokigenConfig画面を返す
    @Test
    void edit_指定年度が存在する場合_nokigenConfig画面を返す() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNokigen1st("20240430");
        nokigen.setNokigen2nd(""); nokigen.setNokigen3rd(""); nokigen.setNokigen4th("");
        nokigen.setNokigen5th(""); nokigen.setNokigen6th(""); nokigen.setNokigen7th("");
        nokigen.setNokigen8th(""); nokigen.setNokigen9th(""); nokigen.setNokigen10th("");
        nokigen.setNokigen11th(""); nokigen.setNokigen12th("");
        when(nokigenService.findByNendo("2024")).thenReturn(nokigen);
        Jichitai jichitai = new Jichitai();
        jichitai.setNendoStMonth("4");
        when(nokigenService.findJichitai("011002")).thenReturn(jichitai);
        Model model = new ExtendedModelMap();

        String view = controller.edit("2024", model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/nokigenConfig");
        assertThat(model.asMap().get("mode")).isEqualTo("edit");
        assertThat(((Nokigen) model.asMap().get("nokigen")).getNokigen1st()).isEqualTo("2024-04-30");
    }

    // No.34 異常系: 指定年度が存在しない場合、list画面へリダイレクト＋errorMessage
    @Test
    void edit_指定年度が存在しない場合_listへリダイレクトとerrorMessage() {
        when(nokigenService.findByNendo("2024")).thenReturn(null);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.edit("2024", new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/nokigen/list");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("指定されたデータが見つかりません。");
    }

    // No.35 異常系: 書き込み権限なしの場合、例外をスロー
    @Test
    void edit_書き込み権限なしの場合_例外をスロー() {
        doThrow(new RuntimeException("権限なし")).when(accessChecker).checkWriteAccess(any());

        assertThatThrownBy(() -> controller.edit("2024", new ExtendedModelMap(), new RedirectAttributesModelMap()))
                .isInstanceOf(RuntimeException.class);
    }

    // ===== exists =====

    // No.36 正常系: 年度が存在する場合、{"exists":true}を返す
    @Test
    void exists_年度が存在する場合_existsTrueを返す() {
        when(nokigenService.existsByNendo("2024")).thenReturn(true);

        var response = controller.exists("2024");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Map.of("exists", true));
    }

    // No.37 正常系: 年度が存在しない場合、{"exists":false}を返す
    @Test
    void exists_年度が存在しない場合_existsFalseを返す() {
        when(nokigenService.existsByNendo("2024")).thenReturn(false);

        var response = controller.exists("2024");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Map.of("exists", false));
    }

    // ===== prevData =====

    // No.38 正常系: 前年度データが存在しshiftMode=noneの場合、変換結果Mapを返す
    @Test
    void prevData_前年度データが存在しshiftModeがnoneの場合_変換結果Mapを返す() {
        Nokigen prev = new Nokigen();
        when(nokigenService.findByNendo("2023")).thenReturn(prev);
        Map<String, String> expected = Map.of("nokigen1st", "2024-04-30");
        when(nokigenService.getPrevDataWithShift(prev, "2024", "none")).thenReturn(expected);

        var response = controller.prevData("2024", "none");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    // No.39 正常系: shiftModeのデフォルト値（指定なし）はnoneとして動作する
    @Test
    void prevData_shiftModeが省略された場合_noneとして動作する() {
        Nokigen prev = new Nokigen();
        when(nokigenService.findByNendo("2023")).thenReturn(prev);
        when(nokigenService.getPrevDataWithShift(any(), any(), any())).thenReturn(Map.of());

        controller.prevData("2024", "none");

        verify(nokigenService).getPrevDataWithShift(prev, "2024", "none");
    }

    // No.40 異常系: 前年度データが存在しない場合、404を返す
    @Test
    void prevData_前年度データが存在しない場合_404を返す() {
        when(nokigenService.findByNendo("2023")).thenReturn(null);

        var response = controller.prevData("2024", "none");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    // No.41 異常系: 権限なしの場合、例外をスロー
    @Test
    void prevData_権限なしの場合_例外をスロー() {
        doThrow(new RuntimeException("権限なし")).when(accessChecker).checkAccess(any());

        assertThatThrownBy(() -> controller.prevData("2024", "none"))
                .isInstanceOf(RuntimeException.class);
    }

    // ===== save =====

    // No.42 正常系: mode=registerで新規登録成功の場合、view画面へリダイレクト＋successMessage
    @Test
    void save_mode_registerで新規登録成功の場合_viewへリダイレクトとsuccessMessage() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNendo("2024");
        when(nokigenService.existsByNendo("2024")).thenReturn(false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(nokigen, "register", new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/nokigen/view/2024");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("納入期限を登録しました。");
    }

    // No.43 正常系: mode=editで更新成功の場合、view画面へリダイレクト＋successMessage
    @Test
    void save_mode_editで更新成功の場合_viewへリダイレクトとsuccessMessage() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNendo("2024");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.save(nokigen, "edit", new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/nokigen/view/2024");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("納入期限を更新しました。");
    }

    // No.44 異常系: nendoがnullの場合、nokigenConfig画面を返す＋validationErrors
    @Test
    void save_nendoがnullの場合_nokigenConfig画面を返すとvalidationErrors() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNendo(null);
        Model model = new ExtendedModelMap();

        String view = controller.save(nokigen, "register", model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/nokigenConfig");
        assertThat(model.asMap()).containsKey("validationErrors");
        assertThat(model.asMap().get("mode")).isEqualTo("register");
    }

    // No.45 異常系: nendoが空文字の場合、nokigenConfig画面を返す＋validationErrors
    @Test
    void save_nendoが空文字の場合_nokigenConfig画面を返すとvalidationErrors() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNendo("");
        Model model = new ExtendedModelMap();

        String view = controller.save(nokigen, "register", model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/nokigenConfig");
        assertThat(model.asMap()).containsKey("validationErrors");
    }

    // No.46 異常系: mode=registerで年度が登録済みの場合、nokigenConfig画面を返す＋errorMessage
    @Test
    void save_mode_registerで年度が登録済みの場合_nokigenConfig画面を返すとerrorMessage() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNendo("2024");
        when(nokigenService.existsByNendo("2024")).thenReturn(true);
        Model model = new ExtendedModelMap();

        String view = controller.save(nokigen, "register", model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/nokigenConfig");
        assertThat(model.asMap().get("errorMessage"))
                .isEqualTo("登録済みの年度です。編集画面から修正してください。");
        assertThat(model.asMap().get("mode")).isEqualTo("register");
    }

    // No.47 異常系: save()で例外が発生した場合、nokigenConfig画面を返す＋errorMessage
    @Test
    void save_saveで例外が発生した場合_nokigenConfig画面を返すとerrorMessage() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNendo("2024");
        when(nokigenService.existsByNendo("2024")).thenReturn(false);
        doThrow(new RuntimeException("DB error")).when(nokigenService).save(any());
        Model model = new ExtendedModelMap();

        String view = controller.save(nokigen, "register", model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("admin/nokigenConfig");
        assertThat(model.asMap().get("errorMessage")).isEqualTo("保存に失敗しました: DB error");
    }

    // No.48 異常系: 書き込み権限なしの場合、例外をスロー
    @Test
    void save_書き込み権限なしの場合_例外をスロー() {
        doThrow(new RuntimeException("権限なし")).when(accessChecker).checkWriteAccess(any());
        Nokigen nokigen = new Nokigen();
        nokigen.setNendo("2024");

        assertThatThrownBy(() -> controller.save(nokigen, "register", new ExtendedModelMap(), new RedirectAttributesModelMap()))
                .isInstanceOf(RuntimeException.class);
    }
}
