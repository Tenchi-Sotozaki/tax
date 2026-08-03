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

    @Test
    void index_初期表示() {
        Model model = new ExtendedModelMap();

        String view = controller.index(model);

        assertThat(view).isEqualTo("admin/reportsConfig");
        assertThat(model.asMap()).containsKeys("reportsConfigForm", "importHistory");
    }

    @Test
    void importFile_ファイル未選択はエラー() {
        ReportsConfigForm form = new ReportsConfigForm();
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");

        String view = controller.importFile(form, new RedirectAttributesModelMap(), auth);

        assertThat(view).isEqualTo("redirect:/admin/reports-config");
    }

    @Test
    void importFile_PNG以外はエラー() {
        ReportsConfigForm form = new ReportsConfigForm();
        form.setFile(new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes()));
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");

        String view = controller.importFile(form, new RedirectAttributesModelMap(), auth);

        assertThat(view).isEqualTo("redirect:/admin/reports-config");
    }

    @Test
    void importFile_正常取込() throws Exception {
        ReportsConfigForm form = new ReportsConfigForm();
        form.setFile(new MockMultipartFile("file", "koin.png", "image/png", "pngdata".getBytes()));
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");

        String view = controller.importFile(form, new RedirectAttributesModelMap(), auth);

        assertThat(view).isEqualTo("redirect:/admin/reports-config");
        verify(koinTorikomiService).importReportFile(any(), eq("011002"), eq("admin"));
    }
}
