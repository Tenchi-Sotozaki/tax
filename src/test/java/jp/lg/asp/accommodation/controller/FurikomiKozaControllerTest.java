package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.FurikomiKozaDto;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.service.FurikomiKozaService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
class FurikomiKozaControllerTest {

    @Mock FurikomiKozaService furikomiKozaService;
    @Mock ScreenAccessChecker accessChecker;
    @InjectMocks FurikomiKozaController controller;

    private MockHttpSession sessionWith(String shiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }

    // ===== view =====

    // No.17 正常系: セッションに指定番号がある、照会画面を返す
    @Test
    void view_セッションに指定番号がある場合_照会画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        FurikomiKozaDto dto = new FurikomiKozaDto();
        when(furikomiKozaService.getFurikomiKoza("00100001")).thenReturn(dto);
        Model model = new ExtendedModelMap();

        String view = controller.view(session, model);

        assertThat(view).isEqualTo("shoreikin/furikomiKoza");
        assertThat(model.asMap()).containsKey("kozaForm");
        assertThat(model.asMap().get("kozaForm")).isEqualTo(dto);
    }

    // No.18 正常系: セッションに指定番号がない、モーダル表示フラグをセット
    @Test
    void view_セッションに指定番号がない場合_モーダル表示フラグをセット() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.view(session, model);

        assertThat(view).isEqualTo("shoreikin/furikomiKoza");
        assertThat(model.asMap().get("showShiteiGassanModal")).isEqualTo(true);
        assertThat(model.asMap()).containsKey("kozaForm");
    }

    // No.19 境界値: セッションの指定番号が空文字の場合、モーダル表示フラグをtrueでセット
    @Test
    void view_セッションの指定番号が空文字の場合_モーダル表示フラグをセット() {
        MockHttpSession session = sessionWith("");
        Model model = new ExtendedModelMap();

        String view = controller.view(session, model);

        assertThat(view).isEqualTo("shoreikin/furikomiKoza");
        assertThat(model.asMap().get("showShiteiGassanModal")).isEqualTo(true);
    }

    // No.20 異常系: アクセス権限がない場合、例外をスロー
    @Test
    void view_アクセス権限がない場合_例外をスロー() {
        doThrow(new RuntimeException("アクセス権限がありません")).when(accessChecker).checkAccess(any());
        MockHttpSession session = sessionWith("00100001");
        Model model = new ExtendedModelMap();

        assertThatThrownBy(() -> controller.view(session, model))
                .isInstanceOf(RuntimeException.class);
    }

    // ===== editMode =====

    // No.21 正常系: フォームのmodeをeditに変更して同画面を返す
    @Test
    void editMode_フォームのmodeをeditに変更して同画面を返す() {
        FurikomiKozaDto form = new FurikomiKozaDto();
        form.setMode("view");
        Model model = new ExtendedModelMap();

        String view = controller.editMode(form, model);

        assertThat(view).isEqualTo("shoreikin/furikomiKoza");
        assertThat(form.getMode()).isEqualTo("edit");
        assertThat(model.asMap().get("kozaForm")).isEqualTo(form);
    }

    // No.22 異常系: 書き込みアクセス権限がない場合、例外をスロー
    @Test
    void editMode_書き込みアクセス権限がない場合_例外をスロー() {
        doThrow(new RuntimeException("書き込み権限がありません")).when(accessChecker).checkWriteAccess(any());
        FurikomiKozaDto form = new FurikomiKozaDto();
        Model model = new ExtendedModelMap();

        assertThatThrownBy(() -> controller.editMode(form, model))
                .isInstanceOf(RuntimeException.class);
    }

    // ===== create =====

    // No.23 正常系: バリデーションに問題なし、サービス呼び出し・リダイレクト・successMessage設定
    @Test
    void create_バリデーション正常_サービス呼び出しとリダイレクトとsuccessMessage設定() {
        FurikomiKozaDto form = new FurikomiKozaDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "kozaForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.create(form, bindingResult, model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/shoreikin/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsEntry("successMessage", "振込先口座情報を登録しました。");
        verify(furikomiKozaService).createFurikomiKoza(form);
    }

    // No.24 異常系: バリデーションがエラー、同画面を返す・modeをcreateに設定
    @Test
    void create_バリデーションエラー_同画面を返しmodeをcreateに設定() {
        FurikomiKozaDto form = new FurikomiKozaDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "kozaForm");
        bindingResult.rejectValue("bankCd", "NotBlank", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.create(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("shoreikin/furikomiKoza");
        assertThat(form.getMode()).isEqualTo("create");
        assertThat(model.asMap().get("kozaForm")).isEqualTo(form);
    }

    // No.25 異常系: サービスが例外をスロー、リダイレクト・errorMessage設定
    @Test
    void create_サービスが例外をスロー_リダイレクトとerrorMessage設定() {
        doThrow(new RuntimeException("DB error")).when(furikomiKozaService).createFurikomiKoza(any());
        FurikomiKozaDto form = new FurikomiKozaDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "kozaForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.create(form, bindingResult, model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/shoreikin/list");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .contains("振込先口座情報登録に失敗しました");
    }

    // No.26 異常系: 書き込みアクセス権限なし、例外をスロー
    @Test
    void create_書き込みアクセス権限なし_例外をスロー() {
        doThrow(new RuntimeException("書き込み権限がありません")).when(accessChecker).checkWriteAccess(any());
        FurikomiKozaDto form = new FurikomiKozaDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "kozaForm");
        Model model = new ExtendedModelMap();

        assertThatThrownBy(() -> controller.create(form, bindingResult, model, new RedirectAttributesModelMap()))
                .isInstanceOf(RuntimeException.class);
    }

    // ===== update =====

    // No.27 正常系: バリデーションに問題なし、サービス呼び出し・リダイレクト・successMessage設定
    @Test
    void update_バリデーション正常_サービス呼び出しとリダイレクトとsuccessMessage設定() {
        FurikomiKozaDto form = new FurikomiKozaDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "kozaForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.update(form, bindingResult, model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/shoreikin/list");
        assertThat(redirectAttributes.getFlashAttributes()).containsEntry("successMessage", "振込先口座情報を更新しました。");
        verify(furikomiKozaService).updateFurikomiKoza(form);
    }

    // No.28 異常系: バリデーションがエラー、同画面を返す・modeをeditに設定
    @Test
    void update_バリデーションエラー_同画面を返しmodeをeditに設定() {
        FurikomiKozaDto form = new FurikomiKozaDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "kozaForm");
        bindingResult.rejectValue("bankCd", "NotBlank", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.update(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("shoreikin/furikomiKoza");
        assertThat(form.getMode()).isEqualTo("edit");
        assertThat(model.asMap().get("kozaForm")).isEqualTo(form);
    }

    // No.29 異常系: サービスが例外をスロー、リダイレクト・errorMessage設定
    @Test
    void update_サービスが例外をスロー_リダイレクトとerrorMessage設定() {
        doThrow(new RuntimeException("version mismatch")).when(furikomiKozaService).updateFurikomiKoza(any());
        FurikomiKozaDto form = new FurikomiKozaDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "kozaForm");
        Model model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.update(form, bindingResult, model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/shoreikin/list");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString())
                .contains("振込先口座更新に失敗しました");
    }

    // No.30 異常系: 書き込みアクセス権限がなし、例外をスロー
    @Test
    void update_書き込みアクセス権限なし_例外をスロー() {
        doThrow(new RuntimeException("書き込み権限がありません")).when(accessChecker).checkWriteAccess(any());
        FurikomiKozaDto form = new FurikomiKozaDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "kozaForm");
        Model model = new ExtendedModelMap();

        assertThatThrownBy(() -> controller.update(form, bindingResult, model, new RedirectAttributesModelMap()))
                .isInstanceOf(RuntimeException.class);
    }
}
