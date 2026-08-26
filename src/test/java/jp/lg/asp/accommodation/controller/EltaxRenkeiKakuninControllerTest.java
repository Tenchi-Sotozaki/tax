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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto;
import jp.lg.asp.accommodation.service.EltaxRenkeiKakuninService;

@ExtendWith(MockitoExtension.class)
class EltaxRenkeiKakuninControllerTest {

    @Mock EltaxRenkeiKakuninService eltaxRenkeiKakuninService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks EltaxRenkeiKakuninController controller;

    @Test
    void preview_空ファイルはリダイレクト() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        String view = controller.preview(emptyFile, new MockHttpSession(),
                new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/eltax-renkei");
    }

    @Test
    void preview_正常処理は確認画面を返す() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        EltaxRenkeiKakuninDto dto = new EltaxRenkeiKakuninDto(
                null, null, null, null, null, "test.csv", null, null, false, null, null, null, null, null, null, null);
        when(eltaxRenkeiKakuninService.preview(file)).thenReturn(dto);
        Model model = new ExtendedModelMap();

        String view = controller.preview(file, new MockHttpSession(), model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("eltaxRenkei/eltaxRenkeiKakunin");
        assertThat(model.asMap()).containsKey("kakuninDto");
    }

    @Test
    void preview_例外時はリダイレクト() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        when(eltaxRenkeiKakuninService.preview(file)).thenThrow(new RuntimeException("解析エラー"));

        String view = controller.preview(file, new MockHttpSession(),
                new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/eltax-renkei");
    }

    @Test
    void commit_セッションなしはリダイレクト() {
        MockHttpSession session = new MockHttpSession();

        String view = controller.commit(null, null, session, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/eltax-renkei");
    }

    @Test
    void commit_正常処理はリダイレクト() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("eltaxUploadedFile", "data".getBytes());
        session.setAttribute("eltaxUploadedFileName", "test.csv");

        String view = controller.commit(null, null, session, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/eltax-renkei");
        verify(eltaxRenkeiKakuninService).commit(any(), eq("test.csv"), isNull(), isNull());
    }
}
