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
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.AtenaSearchForm;
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

    @Test
    void list_一覧画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.list(new AtenaSearchForm(), model);

        assertThat(view).isEqualTo("atena/atenaDaicho");
    }

    @Test
    void showImport_取込画面を返す() {
        when(atenaImportService.findHistory("011002")).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.showImport(model);

        assertThat(view).isEqualTo("atena/atenaRenkei");
    }

    @Test
    void importCsv_空ファイルはエラー() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        String view = controller.importCsv(emptyFile, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/atena/import");
    }

    @Test
    void importCsv_CSV以外はエラー() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        String view = controller.importCsv(file, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/atena/import");
    }

    @Test
    void importCsv_正常取込() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        when(atenaImportService.importCsv(any(), any(), any())).thenReturn(null);

        String view = controller.importCsv(file, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/atena/import");
    }
}
