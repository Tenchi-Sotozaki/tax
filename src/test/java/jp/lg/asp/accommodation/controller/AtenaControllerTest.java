package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.AtenaImportPreviewDto;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.service.AtenaImportService;
import jp.lg.asp.accommodation.util.HashUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtenaControllerTest {

    @Mock AtenaRepository atenaRepository;
    @Mock AtenaImportService atenaImportService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock HashUtil hashUtil;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks AtenaController controller;

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
        when(atenaRepository.search(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

//    @Test
//    void list_一覧画面を返す() {
//        Model model = new ExtendedModelMap();
//
//        String view = controller.list(new AtenaSearchForm(), model);
//
//        assertThat(view).isEqualTo("atena/atenaDaicho");
//    }

    @Test
    void showImport_取込画面を返す() {
        when(atenaImportService.findHistory("011002")).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.showImport(model);

        assertThat(view).isEqualTo("atena/atenaRenkei");
    }

    @Test
    void analyze_空ファイルはエラー() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        ResponseEntity<?> res = controller.analyze(emptyFile, new MockHttpSession());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void analyze_CSV以外はエラー() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        ResponseEntity<?> res = controller.analyze(file, new MockHttpSession());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void analyze_解析結果をセッションに保持する() {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        AtenaImportPreviewDto preview = new AtenaImportPreviewDto();
        when(atenaImportService.analyze(any(), any())).thenReturn(preview);
        MockHttpSession session = new MockHttpSession();

        ResponseEntity<?> res = controller.analyze(file, session);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(session.getAttribute("atenaImportPreview")).isSameAs(preview);
    }

    @Test
    void confirmImport_解析結果が無い場合はエラー() {
        ResponseEntity<?> res = controller.confirmImport(List.of("1"), new MockHttpSession());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void confirmImport_選択された宛名のみ取り込む() {
        AtenaImportPreviewDto preview = new AtenaImportPreviewDto();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("atenaImportPreview", preview);

        ResponseEntity<?> res = controller.confirmImport(List.of("100", "200"), session);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(atenaImportService).confirm(eq(preview), eq(Set.of("100", "200")), eq("011002"), eq("testuser"));
        // 二重登録を防ぐため、確定後はセッションから解析結果を破棄する
        assertThat(session.getAttribute("atenaImportPreview")).isNull();
    }
}
