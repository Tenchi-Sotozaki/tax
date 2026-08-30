package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.service.impl.ReportsConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReportsConfigServiceImplTest {

    @Mock ReportsDefRepository reportsDefRepository;
    @Mock DataSource dataSource;
    @InjectMocks ReportsConfigServiceImpl service;

    // ===== getImportHistory =====

    // No.1 正常系: レコードが複数件存在する場合、全件リストを返す
    @Test
    void getImportHistory_レコードが複数件存在する場合_全件リストを返す() {
        ReportsDef def1 = new ReportsDef();
        def1.setJichitaiCd("011002");
        def1.setId("RPT0000001");
        ReportsDef def2 = new ReportsDef();
        def2.setJichitaiCd("011002");
        def2.setId("RPT0000002");
        when(reportsDefRepository.findAll()).thenReturn(List.of(def1, def2));

        List<ReportsDef> result = service.getImportHistory();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getJichitaiCd()).isEqualTo("011002");
        assertThat(result.get(0).getId()).isEqualTo("RPT0000001");
        assertThat(result.get(1).getId()).isEqualTo("RPT0000002");
    }

    // No.2 正常系: レコードが0件の場合、空リストを返す
    @Test
    void getImportHistory_レコードが0件の場合_空リストを返す() {
        when(reportsDefRepository.findAll()).thenReturn(List.of());

        List<ReportsDef> result = service.getImportHistory();

        assertThat(result).isEmpty();
    }

    // No.3 正常系: findAllが1回呼ばれる
    @Test
    void getImportHistory_findAllが1回呼ばれる() {
        when(reportsDefRepository.findAll()).thenReturn(List.of());

        service.getImportHistory();

        verify(reportsDefRepository, times(1)).findAll();
    }

    // ===== importReportFile =====

    // No.4 正常系: 正常なファイルの場合、PreparedStatementが実行される
    @Test
    void importReportFile_正常なファイルの場合_PreparedStatementが実行される() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any())).thenReturn(mockStmt);
        when(mockStmt.executeUpdate()).thenReturn(1);

        MultipartFile file = new MockMultipartFile(
                "file", "test.jasper", "application/octet-stream", new byte[100]);

        assertThatNoException().isThrownBy(
                () -> service.importReportFile(file, "011002", "user01"));
        verify(mockStmt).executeUpdate();
    }

    // No.5 正常系: PreparedStatementに渡るパラメータが正しい
    @Test
    void importReportFile_PreparedStatementに渡るパラメータが正しい() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any())).thenReturn(mockStmt);
        when(mockStmt.executeUpdate()).thenReturn(1);

        MultipartFile file = new MockMultipartFile(
                "file", "test.jasper", "application/octet-stream", new byte[]{1, 2, 3});

        service.importReportFile(file, "011002", "user01");

        verify(mockStmt).setString(1, "011002");
        verify(mockStmt).setString(2, "RPT0000001");
        verify(mockStmt).setString(3, "2");
        verify(mockStmt).setString(7, "user01");
        verify(mockStmt).setString(9, "user01");
        verify(mockStmt).setInt(10, 1);
    }

    // No.6 異常系: SQLExceptionが発生した場合、RuntimeExceptionをスロー（messageに"SQLエラー"を含む）
    @Test
    void importReportFile_SQLExceptionが発生した場合_RuntimeExceptionをスロー() throws Exception {
        Connection mockConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any())).thenThrow(new SQLException("DB接続エラー"));

        MultipartFile file = new MockMultipartFile(
                "file", "test.jasper", "application/octet-stream", new byte[10]);

        assertThatThrownBy(() -> service.importReportFile(file, "011002", "user01"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SQLエラー");
    }

    // No.7 異常系: file.getBytes()でIOExceptionが発生した場合、RuntimeExceptionをスロー
    @Test
    void importReportFile_getBytesでIOExceptionが発生した場合_RuntimeExceptionをスロー() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(any())).thenReturn(mockStmt);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("読み込みエラー"));
        when(file.getSize()).thenReturn(10L);

        assertThatThrownBy(() -> service.importReportFile(file, "011002", "user01"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("帳票ファイルの取り込みに失敗しました");
    }

    // No.8 異常系: dataSource.getConnection()でSQLExceptionが発生した場合、RuntimeExceptionをスロー
    @Test
    void importReportFile_getConnectionでSQLExceptionが発生した場合_RuntimeExceptionをスロー() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("接続失敗"));

        MultipartFile file = new MockMultipartFile(
                "file", "test.jasper", "application/octet-stream", new byte[10]);

        assertThatThrownBy(() -> service.importReportFile(file, "011002", "user01"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SQLエラー");
    }
}
