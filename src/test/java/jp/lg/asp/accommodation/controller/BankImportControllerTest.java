package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.BankImportResultDto;
import jp.lg.asp.accommodation.service.BankImportService;

@ExtendWith(MockitoExtension.class)
class BankImportControllerTest {

    @Mock BankImportService bankImportService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks BankImportController controller;

    @Test
    @DisplayName("#1 index 正常系 初期表示：取込画面を返す")
    void 確認1_初期表示() {
        String view = controller.index();

        assertThat(view).isEqualTo("admin/bankImport");
        verify(accessChecker, times(1)).checkAccess(ScreenManagement.BANK_IMPORT);
        verify(bankImportService, never()).importFromZip(any());
    }

    @Test
    @DisplayName("#2 upload 正常系 取込成功：取込結果と successMessage がモデルに設定される")
    void 確認2_取込成功() {
        MockMultipartFile file = new MockMultipartFile("file", "zengin-code.zip", "application/zip", new byte[]{1, 2, 3});
        BankImportResultDto dto = new BankImportResultDto();
        dto.setBankCount(1500);
        dto.setBranchCount(30000);
        dto.setSkippedBankCount(0);
        dto.setSkippedBranchCount(0);
        dto.setUpdatedAt("2026-08-01");
        when(bankImportService.importFromZip(file)).thenReturn(dto);

        Model model = new ExtendedModelMap();
        String view = controller.upload(file, model);

        assertThat(view).isEqualTo("admin/bankImport");
        assertThat(model.asMap().get("result")).isSameAs(dto);
        assertThat(model.asMap().get("successMessage")).isEqualTo("金融機関コードの取込が完了しました。");
        assertThat(model.asMap()).doesNotContainKey("errorMessage");
        verify(bankImportService, times(1)).importFromZip(file);
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.BANK_IMPORT);
    }

    @Test
    @DisplayName("#3 upload 異常系 IllegalStateException が発生した場合：例外メッセージがそのまま errorMessage に設定される")
    void 確認3_IllegalStateException() {
        MockMultipartFile file = new MockMultipartFile("file", "", "application/zip", new byte[0]);
        when(bankImportService.importFromZip(file)).thenThrow(new IllegalStateException("ファイルを選択してください。"));

        Model model = new ExtendedModelMap();
        String view = controller.upload(file, model);

        assertThat(view).isEqualTo("admin/bankImport");
        assertThat(model.asMap().get("errorMessage")).isEqualTo("ファイルを選択してください。");
        assertThat(model.asMap()).doesNotContainKey("result");
        assertThat(model.asMap()).doesNotContainKey("successMessage");
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.BANK_IMPORT);
    }

    @Test
    @DisplayName("#4 upload 異常系 IllegalStateException 以外の例外が発生した場合：固定文言の errorMessage が設定される")
    void 確認4_RuntimeException() {
        MockMultipartFile file = new MockMultipartFile("file", "zengin-code.zip", "application/zip", new byte[]{1, 2, 3});
        when(bankImportService.importFromZip(file)).thenThrow(new RuntimeException("接続に失敗しました"));

        Model model = new ExtendedModelMap();
        String view = controller.upload(file, model);

        assertThat(view).isEqualTo("admin/bankImport");
        assertThat(model.asMap().get("errorMessage")).isEqualTo("取込処理中にエラーが発生しました。");
        assertThat(model.asMap()).doesNotContainKey("result");
        assertThat(model.asMap()).doesNotContainKey("successMessage");
        verify(accessChecker, times(1)).checkWriteAccess(ScreenManagement.BANK_IMPORT);
    }

    @Test
    @DisplayName("#5 upload 異常系 IllegalStateException のメッセージが null の場合")
    void 確認5_IllegalStateExceptionメッセージnull() {
        MockMultipartFile file = new MockMultipartFile("file", "zengin-code.zip", "application/zip", new byte[]{1, 2, 3});
        when(bankImportService.importFromZip(file)).thenThrow(new IllegalStateException());

        Model model = new ExtendedModelMap();
        String view = controller.upload(file, model);

        assertThat(view).isEqualTo("admin/bankImport");
        assertThat(model.asMap().get("errorMessage")).isNull();
    }
}
