package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.lg.asp.accommodation.dto.BankImportResultDto;
import jp.lg.asp.accommodation.service.impl.BankImportServiceImpl;

@ExtendWith(MockitoExtension.class)
class BankImportServiceImplTest {

    @Mock JdbcTemplate jdbcTemplate;

    BankImportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BankImportServiceImpl(jdbcTemplate, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== zip組み立てヘルパー ==========

    /** エントリ名と内容のペアでzipを組み立てる */
    private MockMultipartFile zipOf(Object... nameAndContents) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            for (int i = 0; i < nameAndContents.length; i += 2) {
                String name = (String) nameAndContents[i];
                byte[] content = nameAndContents[i + 1] instanceof String s
                        ? s.getBytes(StandardCharsets.UTF_8)
                        : (byte[]) nameAndContents[i + 1];
                zos.putNextEntry(new ZipEntry(name));
                zos.write(content);
                zos.closeEntry();
            }
        }
        return new MockMultipartFile("file", "zengin-code.zip", "application/zip", baos.toByteArray());
    }

    private static final String BANKS_1 = """
            {"0001":{"code":"0001","name":"みずほ","kana":"ミズホ"}}
            """;
    private static final String BANKS_2 = """
            {"0001":{"code":"0001","name":"みずほ","kana":"ミズホ"},"0005":{"code":"0005","name":"三菱","kana":"ミツビシ"}}
            """;
    private static final String BRANCH_0001_2 = """
            {"001":{"code":"001","name":"東京","kana":"トウキヨウ"},"002":{"code":"002","name":"大阪","kana":"オオサカ"}}
            """;
    private static final String BRANCH_0005_1 = """
            {"001":{"code":"001","name":"本店","kana":"ホンテン"}}
            """;

    // ========== テスト ==========

    @Test
    @DisplayName("#6 importFromZip 正常系 金融機関2件・支店3件のzipを取り込む場合：件数と版が結果に設定される")
    void 確認6_正常取込() throws IOException {
        MockMultipartFile file = zipOf(
                "zengin-code-master/data/banks.json", BANKS_2,
                "zengin-code-master/data/branches/0001.json", BRANCH_0001_2,
                "zengin-code-master/data/branches/0005.json", BRANCH_0005_1,
                "zengin-code-master/data/updated_at", "2026-08-01");

        BankImportResultDto result = service.importFromZip(file);

        assertThat(result.getBankCount()).isEqualTo(2);
        assertThat(result.getBranchCount()).isEqualTo(3);
        assertThat(result.getSkippedBankCount()).isEqualTo(0);
        assertThat(result.getSkippedBranchCount()).isEqualTo(0);
        assertThat(result.getUpdatedAt()).isEqualTo("2026-08-01");
        verify(jdbcTemplate, times(1)).batchUpdate(startsWith("INSERT INTO tmp_bank_work"), any(BatchPreparedStatementSetter.class));
        verify(jdbcTemplate, times(1)).batchUpdate(startsWith("INSERT INTO tmp_branch_work"), any(BatchPreparedStatementSetter.class));
        verify(jdbcTemplate, times(1)).execute("TRUNCATE TABLE m_bank");
        verify(jdbcTemplate, times(1)).execute("TRUNCATE TABLE m_branch");
    }

    @Test
    @DisplayName("#7 importFromZip 異常系 file が null の場合：例外となり DB は更新されない")
    void 確認7_fileがnull() {
        assertThatThrownBy(() -> service.importFromZip(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ファイルを選択してください。");
        verify(jdbcTemplate, never()).execute(anyString());
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("#8 importFromZip 異常系 file が空の場合：例外となり DB は更新されない")
    void 確認8_fileが空() {
        MockMultipartFile file = new MockMultipartFile("file", "", "application/zip", new byte[0]);

        assertThatThrownBy(() -> service.importFromZip(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ファイルを選択してください。");
        verify(jdbcTemplate, never()).execute(anyString());
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("#9 importFromZip 異常系 zip内に banks.json が無い場合：例外となり DB は更新されない")
    void 確認9_banksJsonなし() throws IOException {
        MockMultipartFile file = zipOf("zengin-code-master/data/updated_at", "2026-08-01");

        assertThatThrownBy(() -> service.importFromZip(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("zip内に data/banks.json が見つかりません。ZenginCode のzipファイルを選択してください。");
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    @DisplayName("#10 importFromZip 異常系 zipとして読み取れないファイルの場合：例外となる")
    void 確認10_不正なzip() {
        MockMultipartFile file = new MockMultipartFile("file", "broken.zip", "application/zip",
                "これはzipではありません".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.importFromZip(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("zipファイルの読み取りに失敗しました。ファイルが壊れていないか確認してください。");
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    @DisplayName("#11 importFromZip 異常系 banks.json が不正なJSONの場合：例外となる")
    void 確認11_不正なJSON() throws IOException {
        MockMultipartFile file = zipOf("zengin-code-master/data/banks.json", "{ invalid json");

        assertThatThrownBy(() -> service.importFromZip(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JSONの解析に失敗しました。（data/banks.json）");
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    @DisplayName("#12 importFromZip 異常系 金融機関マスタに存在しない支店ファイルがある場合：スキップされ件数に計上される")
    void 確認12_存在しない金融機関コードの支店はスキップ() throws IOException {
        String branch9999 = """
                {"001":{"code":"001","name":"A","kana":"A"},"002":{"code":"002","name":"B","kana":"B"},"003":{"code":"003","name":"C","kana":"C"}}
                """;
        MockMultipartFile file = zipOf(
                "zengin-code-master/data/banks.json", BANKS_1,
                "zengin-code-master/data/branches/0001.json", BRANCH_0001_2,
                "zengin-code-master/data/branches/9999.json", branch9999);

        BankImportResultDto result = assertDoesNotThrow(() -> service.importFromZip(file));

        assertThat(result.getBankCount()).isEqualTo(1);
        assertThat(result.getBranchCount()).isEqualTo(2);
        assertThat(result.getSkippedBankCount()).isEqualTo(1);
        assertThat(result.getSkippedBranchCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("#13 importFromZip 異常系 zip内に updated_at が無い場合：版は null となる")
    void 確認13_updatedAtなし() throws IOException {
        MockMultipartFile file = zipOf(
                "zengin-code-master/data/banks.json", BANKS_1,
                "zengin-code-master/data/branches/0001.json", BRANCH_0001_2);

        BankImportResultDto result = assertDoesNotThrow(() -> service.importFromZip(file));

        assertThat(result.getUpdatedAt()).isNull();
        assertThat(result.getBankCount()).isEqualTo(1);
        assertThat(result.getBranchCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("#14 importFromZip 正常系 updated_at の前後に改行・空白がある場合：除去される")
    void 確認14_updatedAt前後の空白除去() throws IOException {
        MockMultipartFile file = zipOf(
                "zengin-code-master/data/banks.json", BANKS_1,
                "zengin-code-master/data/updated_at", "\n 2026-08-01 \n");

        BankImportResultDto result = service.importFromZip(file);

        assertThat(result.getUpdatedAt()).isEqualTo("2026-08-01");
    }

    @Test
    @DisplayName("#15 importFromZip 異常系 name・kana が欠落または null の場合：空文字として取り込まれる")
    void 確認15_nameKanaがnull() throws IOException, SQLException {
        String banksJson = "{\"0001\":{\"code\":\"0001\",\"kana\":null}}";
        MockMultipartFile file = zipOf("zengin-code-master/data/banks.json", banksJson);

        ArgumentCaptor<BatchPreparedStatementSetter> captor = ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        BankImportResultDto result = assertDoesNotThrow(() -> service.importFromZip(file));

        assertThat(result.getBankCount()).isEqualTo(1);
        verify(jdbcTemplate).batchUpdate(startsWith("INSERT INTO tmp_bank_work"), captor.capture());

        PreparedStatement ps = mock(PreparedStatement.class);
        captor.getValue().setValues(ps, 0);
        verify(ps).setString(2, "");  // bank_name
        verify(ps).setString(3, "");  // bank_kana
    }

    @Test
    @DisplayName("#16 importFromZip 異常系 支店ファイルが1つも無い場合：支店は0件となり支店の登録は行われない")
    void 確認16_支店ファイルなし() throws IOException {
        MockMultipartFile file = zipOf("zengin-code-master/data/banks.json", BANKS_1);

        BankImportResultDto result = service.importFromZip(file);

        assertThat(result.getBankCount()).isEqualTo(1);
        assertThat(result.getBranchCount()).isEqualTo(0);
        verify(jdbcTemplate, times(1)).batchUpdate(startsWith("INSERT INTO tmp_bank_work"), any(BatchPreparedStatementSetter.class));
        verify(jdbcTemplate, never()).batchUpdate(startsWith("INSERT INTO tmp_branch_work"), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("#17 importFromZip 正常系 zip内のディレクトリと対象外ファイルは無視される")
    void 確認17_対象外ファイルは無視() throws IOException {
        MockMultipartFile file = zipOf(
                "zengin-code-master/data/banks.json", BANKS_1,
                "zengin-code-master/data/branches/0001.json", BRANCH_0001_2,
                "README.md", "readme content");

        BankImportResultDto result = assertDoesNotThrow(() -> service.importFromZip(file));

        assertThat(result.getBankCount()).isEqualTo(1);
        assertThat(result.getBranchCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("#18 importFromZip 正常系 金融機関がバッチサイズを超える場合：分割して登録される")
    void 確認18_バッチサイズ超過() throws IOException {
        // 1001件のbanks.jsonを生成
        StringBuilder sb = new StringBuilder("{");
        for (int i = 1; i <= 1001; i++) {
            String code = String.format("%04d", i);
            if (i > 1) sb.append(",");
            sb.append("\"").append(code).append("\":{\"code\":\"").append(code).append("\",\"name\":\"銀行").append(i).append("\",\"kana\":\"カナ\"}");
        }
        sb.append("}");

        MockMultipartFile file = zipOf("zengin-code-master/data/banks.json", sb.toString());

        ArgumentCaptor<BatchPreparedStatementSetter> captor = ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        BankImportResultDto result = service.importFromZip(file);

        assertThat(result.getBankCount()).isEqualTo(1001);
        verify(jdbcTemplate, times(2)).batchUpdate(startsWith("INSERT INTO tmp_bank_work"), captor.capture());

        assertThat(captor.getAllValues().get(0).getBatchSize()).isEqualTo(1000);
        assertThat(captor.getAllValues().get(1).getBatchSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("#19 importFromZip 正常系 ログイン中ユーザーがいる場合：登録者・更新者にユーザーIDが設定される")
    void 確認19_ログインユーザーが設定される() throws IOException, SQLException {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("U001", "password"));

        MockMultipartFile file = zipOf("zengin-code-master/data/banks.json", BANKS_1);

        ArgumentCaptor<BatchPreparedStatementSetter> captor = ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        service.importFromZip(file);

        verify(jdbcTemplate).batchUpdate(startsWith("INSERT INTO tmp_bank_work"), captor.capture());
        PreparedStatement ps = mock(PreparedStatement.class);
        captor.getValue().setValues(ps, 0);

        verify(ps).setString(5, "U001");  // add_user
        verify(ps).setString(7, "U001");  // upd_user
        verify(ps).setInt(8, 1);          // version
    }

    @Test
    @DisplayName("#20 importFromZip 異常系 ログイン情報が無い場合：登録者・更新者に \"system\" が設定される")
    void 確認20_ログイン情報なしはsystem() throws IOException, SQLException {
        // SecurityContextHolder は設定しない

        MockMultipartFile file = zipOf("zengin-code-master/data/banks.json", BANKS_1);

        ArgumentCaptor<BatchPreparedStatementSetter> captor = ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        service.importFromZip(file);

        verify(jdbcTemplate).batchUpdate(startsWith("INSERT INTO tmp_bank_work"), captor.capture());
        PreparedStatement ps = mock(PreparedStatement.class);
        captor.getValue().setValues(ps, 0);

        verify(ps).setString(5, "system");  // add_user
        verify(ps).setString(7, "system");  // upd_user
    }

    @Test
    @DisplayName("#21 importFromZip 正常系 一時テーブル経由で置き換える一連のSQLが順番に実行されること")
    void 確認21_SQL実行順序() throws IOException {
        MockMultipartFile file = zipOf(
                "zengin-code-master/data/banks.json", BANKS_1,
                "zengin-code-master/data/branches/0001.json", BRANCH_0001_2);

        service.importFromZip(file);

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).execute("DROP TABLE IF EXISTS tmp_bank_work");
        inOrder.verify(jdbcTemplate).execute("DROP TABLE IF EXISTS tmp_branch_work");
        inOrder.verify(jdbcTemplate).execute(startsWith("CREATE TEMP TABLE tmp_bank_work"));
        inOrder.verify(jdbcTemplate).execute(startsWith("CREATE TEMP TABLE tmp_branch_work"));
        inOrder.verify(jdbcTemplate, times(1)).batchUpdate(startsWith("INSERT INTO tmp_bank_work"), any(BatchPreparedStatementSetter.class));
        inOrder.verify(jdbcTemplate, times(1)).batchUpdate(startsWith("INSERT INTO tmp_branch_work"), any(BatchPreparedStatementSetter.class));
        inOrder.verify(jdbcTemplate).execute("TRUNCATE TABLE m_bank");
        inOrder.verify(jdbcTemplate).execute("TRUNCATE TABLE m_branch");
        inOrder.verify(jdbcTemplate).execute("INSERT INTO m_bank SELECT * FROM tmp_bank_work");
        inOrder.verify(jdbcTemplate).execute("INSERT INTO m_branch SELECT * FROM tmp_branch_work");
        inOrder.verify(jdbcTemplate).execute("DROP TABLE tmp_bank_work");
        inOrder.verify(jdbcTemplate).execute("DROP TABLE tmp_branch_work");
    }

    @Test
    @DisplayName("#22 importFromZip 異常系 登録処理でDBエラーが発生した場合：例外がそのまま伝播する")
    void 確認22_DBエラーは伝播する() throws IOException {
        MockMultipartFile file = zipOf("zengin-code-master/data/banks.json", BANKS_1);
        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenThrow(new DataAccessResourceFailureException("DB error"));

        assertThatThrownBy(() -> service.importFromZip(file))
                .isInstanceOf(DataAccessResourceFailureException.class);
        verify(jdbcTemplate, never()).execute("TRUNCATE TABLE m_bank");
        verify(jdbcTemplate, never()).execute("TRUNCATE TABLE m_branch");
    }
}
