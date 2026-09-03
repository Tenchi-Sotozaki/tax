package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto;
import jp.lg.asp.accommodation.exception.EltaxRenkeiKakuninValidationException;
import jp.lg.asp.accommodation.service.EltaxRenkeiKakuninService;

@ExtendWith(MockitoExtension.class)
class EltaxRenkeiKakuninControllerTest {

    @Mock EltaxRenkeiKakuninService eltaxRenkeiKakuninService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks EltaxRenkeiKakuninController controller;

    private static final String SESSION_KEY_FILE = "eltaxUploadedFile";
    private static final String SESSION_KEY_FILE_NAME = "eltaxUploadedFileName";

    private EltaxRenkeiKakuninDto emptyDto(String fileName) {
        return new EltaxRenkeiKakuninDto(
                null, null, null, null, null, fileName, null, null, false, null, null, null, null, null, null, null);
    }

    // -------------------------------------------------------------------------
    // preview
    // -------------------------------------------------------------------------

    // No.1: 正常なファイルを選択してプレビューを表示する
    @Test
    void preview_正常なファイルを選択してプレビューを表示する() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        EltaxRenkeiKakuninDto dto = emptyDto("test.csv");
        when(eltaxRenkeiKakuninService.preview(file)).thenReturn(dto);
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.preview(file, session, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("eltaxRenkei/eltaxRenkeiKakunin");
        assertThat(model.asMap()).containsKey("kakuninDto");
        assertThat(session.getAttribute(SESSION_KEY_FILE)).isNotNull();
        assertThat(session.getAttribute(SESSION_KEY_FILE_NAME)).isEqualTo("test.csv");
    }

    // No.2: サービス側から単一エラーメッセージが返却された場合のプレビュー表示
    @Test
    void preview_エラーメッセージ付きDtoが返却された場合にModelへ設定する() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        EltaxRenkeiKakuninDto dto = emptyDto("test.csv");
        dto.setErrorMessage("エラーが発生しました。");
        when(eltaxRenkeiKakuninService.preview(file)).thenReturn(dto);
        Model model = new ExtendedModelMap();

        String view = controller.preview(file, new MockHttpSession(), model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("eltaxRenkei/eltaxRenkeiKakunin");
        assertThat(model.asMap()).containsKey("kakuninDto");
        assertThat(model.asMap()).containsKey("errorMessage");
    }

    // No.3: ファイル未選択（空ファイル）でリクエストを送信する
    @Test
    void preview_空ファイルの場合はリダイレクトしエラーメッセージを設定する() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.preview(emptyFile, new MockHttpSession(),
                new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/eltax-renkei");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("ファイルを選択してください。");
    }

    // No.4: バリデーション例外が発生する
    @Test
    void preview_バリデーション例外が発生した場合に確認画面を返す() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        EltaxRenkeiKakuninDto dto = emptyDto("test.csv");
        EltaxRenkeiKakuninValidationException ex =
                new EltaxRenkeiKakuninValidationException(java.util.List.of("エラー1"), dto);
        when(eltaxRenkeiKakuninService.preview(file)).thenThrow(ex);
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.preview(file, session, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("eltaxRenkei/eltaxRenkeiKakunin");
        assertThat(model.asMap()).containsKey("kakuninDto");
        assertThat(model.asMap()).containsKey("errorMessages");
        assertThat(session.getAttribute(SESSION_KEY_FILE_NAME)).isEqualTo("test.csv");
    }

    // No.5: ファイル処理中に想定外の汎用例外が発生する
    @Test
    void preview_汎用例外が発生した場合はリダイレクトしエラーメッセージを設定する() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        when(eltaxRenkeiKakuninService.preview(file)).thenThrow(new RuntimeException("解析エラー"));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.preview(file, new MockHttpSession(),
                new ExtendedModelMap(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/eltax-renkei");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isEqualTo("解析エラー");
    }

    // No.6: アクセス権限エラー時に処理を停止する
    @Test
    void preview_アクセス権限エラー時に処理を停止する() throws Exception {
        doThrow(new RuntimeException("AccessDenied")).when(accessChecker).checkAccess(any());
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());

        assertThatThrownBy(() -> controller.preview(file, new MockHttpSession(),
                new ExtendedModelMap(), new RedirectAttributesModelMap()))
                .isInstanceOf(RuntimeException.class);
        verify(eltaxRenkeiKakuninService, never()).preview(any());
    }

    // -------------------------------------------------------------------------
    // repreview
    // -------------------------------------------------------------------------

    // No.7: セッションにファイルが存在する状態で指定番号を上書き再プレビューする
    @Test
    void repreview_セッションにファイルが存在する場合は200を返す() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY_FILE, "data".getBytes());
        EltaxRenkeiKakuninDto dto = emptyDto(null);
        when(eltaxRenkeiKakuninService.repreview(any(), eq("shi00001"))).thenReturn(dto);

        ResponseEntity<?> response = controller.repreview(Map.of("shiteiNo", "shi00001"), session);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(dto);
    }

    // No.8: セッションが切れた状態でファイルデータなしで再プレビューする
    @Test
    void repreview_セッションが切れた場合は400を返す() {
        MockHttpSession session = new MockHttpSession();

        ResponseEntity<?> response = controller.repreview(Map.of("shiteiNo", "shi00001"), session);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("セッションが切れました。再度ファイルを選択してください。");
    }

    // No.9: 再プレビュー処理中に例外が発生する
    @Test
    void repreview_例外が発生した場合は400を返す() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY_FILE, "data".getBytes());
        when(eltaxRenkeiKakuninService.repreview(any(), any())).thenThrow(new RuntimeException("エラー"));

        ResponseEntity<?> response = controller.repreview(Map.of("shiteiNo", "shi00001"), session);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("エラー");
    }

    // -------------------------------------------------------------------------
    // commit
    // -------------------------------------------------------------------------

    // No.10: 宛名番号・指定番号の両方を指定して取込を確定する
    @Test
    void commit_正常に取込を確定してリダイレクトする() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY_FILE, "data".getBytes());
        session.setAttribute(SESSION_KEY_FILE_NAME, "test.csv");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.commit("5000000000000011", "shi00001", session, redirectAttributes);

        verify(eltaxRenkeiKakuninService).commit(any(), eq("test.csv"),
                eq(new BigDecimal("5000000000000011")), eq("shi00001"));
        assertThat(view).isEqualTo("redirect:/eltax-renkei");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("ファイルを取り込みました。");
        assertThat(session.getAttribute(SESSION_KEY_FILE)).isNull();
    }

    // No.11: 宛名番号・指定番号が空で取込を確定する
    @Test
    void commit_宛名番号が空の場合はnullでサービスを呼び出す() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY_FILE, "data".getBytes());
        session.setAttribute(SESSION_KEY_FILE_NAME, "test.csv");

        controller.commit("", "", session, new RedirectAttributesModelMap());

        verify(eltaxRenkeiKakuninService).commit(any(), eq("test.csv"), isNull(), eq(""));
    }

    // No.12: 宛名番号に数値以外の文字列が入力されて取込を確定する
    @Test
    void commit_宛名番号が数値以外の場合はnullでサービスを呼び出す() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY_FILE, "data".getBytes());
        session.setAttribute(SESSION_KEY_FILE_NAME, "test.csv");

        controller.commit("ABC", "shi00001", session, new RedirectAttributesModelMap());

        verify(eltaxRenkeiKakuninService).commit(any(), eq("test.csv"), isNull(), eq("shi00001"));
    }

    // No.13: セッションが切れた状態で取込を実行する
    @Test
    void commit_セッションが切れた場合はリダイレクトしエラーメッセージを設定する() {
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.commit("5000000000000011", "shi00001", session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/eltax-renkei");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("セッションが切れました。再度ファイルを選択してください。");
        verify(eltaxRenkeiKakuninService, never()).commit(any(), any(), any(), any());
    }

    // No.14: 取込実行時にサービス例外が発生する
    @Test
    void commit_サービス例外が発生した場合はリダイレクトしエラーメッセージを設定しセッションを破棄する() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY_FILE, "data".getBytes());
        session.setAttribute(SESSION_KEY_FILE_NAME, "test.csv");
        doThrow(new RuntimeException("取込エラー")).when(eltaxRenkeiKakuninService)
                .commit(any(), any(), any(), any());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.commit("5000000000000011", "shi00001", session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/eltax-renkei");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isEqualTo("取込エラー");
        assertThat(session.getAttribute(SESSION_KEY_FILE)).isNull();
    }

    // No.15: 書込権限エラー時に取込処理を停止する
    @Test
    void commit_書込権限エラー時に取込処理を停止する() {
        doThrow(new RuntimeException("AccessDenied")).when(accessChecker).checkWriteAccess(any());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SESSION_KEY_FILE, "data".getBytes());

        assertThatThrownBy(() -> controller.commit("5000000000000011", "shi00001",
                session, new RedirectAttributesModelMap()))
                .isInstanceOf(RuntimeException.class);
        verify(eltaxRenkeiKakuninService, never()).commit(any(), any(), any(), any());
    }
}
