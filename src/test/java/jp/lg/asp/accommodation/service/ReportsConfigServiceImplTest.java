package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.service.impl.ReportsConfigServiceImpl;

/**
 * 帳票設定（ACCOMMODATION_TAX-381）の Service 単体テスト。
 *
 * このクラスは JPA ではなく DataSource から直接 Connection を取り、
 * m_reports_def へ UPSERT する作りになっている。
 * そのため DataSource / Connection / PreparedStatement をモックにして、
 * バインドされる値と例外の変換だけを検証する。
 */
@ExtendWith(MockitoExtension.class)
class ReportsConfigServiceImplTest {

    @Mock ReportsDefRepository reportsDefRepository;
    @Mock DataSource dataSource;
    @Mock Connection connection;
    @Mock PreparedStatement statement;

    @InjectMocks ReportsConfigServiceImpl service;

    private static final String JICHITAI_CD = "01100";
    private static final String USER_ID = "user01";

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.prepareStatement(anyString())).thenReturn(statement);
    }

    private MultipartFile jrxmlFile() {
        return new MockMultipartFile("file", "nonyusho.jrxml", "application/xml",
                "<jasperReport/>".getBytes());
    }

    // ===================================================================
    // getImportHistory — 取込履歴
    // ===================================================================

    @Test
    void getImportHistory_リポジトリの全件をそのまま返す() {
        ReportsDef def = new ReportsDef();
        def.setId("RPT0000001");
        when(reportsDefRepository.findAll()).thenReturn(List.of(def));

        assertThat(service.getImportHistory()).containsExactly(def);
    }

    // ===================================================================
    // importReportFile — 取込
    // ===================================================================

    @Test
    void importReportFile_UPSERTのSQLが実行される() throws Exception {
        service.importReportFile(jrxmlFile(), JICHITAI_CD, USER_ID);

        verify(connection).prepareStatement(contains("INSERT INTO m_reports_def"));
        verify(connection).prepareStatement(contains("ON CONFLICT (jichitai_cd, id) DO UPDATE"));
        verify(statement).executeUpdate();
    }

    @Test
    void importReportFile_自治体コードと利用者IDがバインドされる() throws Exception {
        service.importReportFile(jrxmlFile(), JICHITAI_CD, USER_ID);

        verify(statement).setString(1, JICHITAI_CD);
        verify(statement).setString(7, USER_ID);
        verify(statement).setString(9, USER_ID);
    }

    /**
     * 帳票IDは "RPT0000001"、区分は "2"（バイナリ）で固定されている。
     * 取り込んだファイルに関わらず同じ1行を上書きする挙動なので、
     * ここでは現状の実装をそのまま固定している。
     */
    @Test
    void importReportFile_帳票IDと区分は固定値が入る() throws Exception {
        service.importReportFile(jrxmlFile(), JICHITAI_CD, USER_ID);

        verify(statement).setString(2, "RPT0000001");
        verify(statement).setString(3, "2");
        verify(statement).setString(4, "");
        verify(statement).setInt(10, 1);
    }

    @Test
    void importReportFile_ファイル本体がバイナリとして渡される() throws Exception {
        MultipartFile file = jrxmlFile();

        service.importReportFile(file, JICHITAI_CD, USER_ID);

        ArgumentCaptor<InputStream> captor = ArgumentCaptor.forClass(InputStream.class);
        verify(statement).setBinaryStream(eq(5), captor.capture(), eq((int) file.getSize()));
        assertThat(captor.getValue().readAllBytes()).isEqualTo(file.getBytes());
    }

    @Test
    void importReportFile_接続を閉じる() throws Exception {
        service.importReportFile(jrxmlFile(), JICHITAI_CD, USER_ID);

        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void importReportFile_SQLExceptionはSQLエラーとして送出される() throws Exception {
        when(statement.executeUpdate()).thenThrow(new SQLException("重複キー"));

        assertThatThrownBy(() -> service.importReportFile(jrxmlFile(), JICHITAI_CD, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SQLエラー")
                .hasMessageContaining("重複キー");
    }

    @Test
    void importReportFile_接続の取得に失敗した場合もSQLエラーとして送出される() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("接続失敗"));

        assertThatThrownBy(() -> service.importReportFile(jrxmlFile(), JICHITAI_CD, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SQLエラー")
                .hasMessageContaining("接続失敗");
    }

    @Test
    void importReportFile_その他の例外は取込失敗として送出される() throws Exception {
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.getBytes()).thenThrow(new IOException("読み込み失敗"));

        assertThatThrownBy(() -> service.importReportFile(broken, JICHITAI_CD, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("帳票ファイルの取り込みに失敗しました");
    }
}
