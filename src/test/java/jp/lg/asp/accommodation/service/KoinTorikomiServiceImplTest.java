package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.entity.KoinTorikomi;
import jp.lg.asp.accommodation.repository.KoinTorikomiRepository;
import jp.lg.asp.accommodation.service.impl.KoinTorikomiServiceImpl;

/**
 * 帳票設定 公印取込（ACCOMMODATION_TAX-381）の Service 単体テスト。
 *
 * NamedParameterJdbcTemplate をモックにして、
 * t_koin_torikomi へ渡すパラメータの中身を検証する。
 * 取込日時などの現在時刻は値の一致では見ず、設定されていることだけを確認している。
 */
@ExtendWith(MockitoExtension.class)
class KoinTorikomiServiceImplTest {

    @Mock KoinTorikomiRepository koinTorikomiRepository;
    @Mock NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @InjectMocks KoinTorikomiServiceImpl service;

    private static final String JICHITAI_CD = "01100";
    private static final String USER_ID = "user01";

    private MultipartFile koinFile() {
        return new MockMultipartFile("file", "koin.png", "image/png", new byte[] { 1, 2, 3 });
    }

    /** update() に渡されたパラメータを取り出す */
    private MapSqlParameterSource capturedParams() {
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(namedParameterJdbcTemplate).update(anyString(), captor.capture());
        return captor.getValue();
    }

    // ===================================================================
    // getImportHistory — 取込履歴
    // ===================================================================

    @Test
    void getImportHistory_リポジトリの全件をそのまま返す() {
        KoinTorikomi entity = new KoinTorikomi();
        entity.setFileName("koin.png");
        when(koinTorikomiRepository.findAll()).thenReturn(List.of(entity));

        assertThat(service.getImportHistory()).containsExactly(entity);
    }

    // ===================================================================
    // importReportFile — 取込
    // ===================================================================

    @Test
    void importReportFile_UPSERTのSQLが実行される() {
        service.importReportFile(koinFile(), JICHITAI_CD, USER_ID);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(namedParameterJdbcTemplate).update(sql.capture(), any(MapSqlParameterSource.class));
        assertThat(sql.getValue()).contains("INSERT INTO t_koin_torikomi");
        assertThat(sql.getValue()).contains("ON CONFLICT (jichitai_cd, seq) DO UPDATE");
    }

    @Test
    void importReportFile_自治体コードとファイル名と利用者IDが渡される() {
        service.importReportFile(koinFile(), JICHITAI_CD, USER_ID);

        MapSqlParameterSource params = capturedParams();
        assertThat(params.getValue("jichitaiCd")).isEqualTo(JICHITAI_CD);
        assertThat(params.getValue("fileName")).isEqualTo("koin.png");
        assertThat(params.getValue("torikomiUser")).isEqualTo(USER_ID);
        assertThat(params.getValue("addUser")).isEqualTo(USER_ID);
        assertThat(params.getValue("updUser")).isEqualTo(USER_ID);
    }

    @Test
    void importReportFile_取込日時と登録日時が設定される() {
        service.importReportFile(koinFile(), JICHITAI_CD, USER_ID);

        MapSqlParameterSource params = capturedParams();
        assertThat(params.getValue("torikomiDt")).isNotNull();
        assertThat(params.getValue("addDt")).isNotNull();
        assertThat(params.getValue("updDt")).isNotNull();
    }

    /**
     * 連番は 1 固定。自治体ごとに公印を1件しか持てない実装なので、
     * ここでは現状の挙動をそのまま固定している。
     */
    @Test
    void importReportFile_連番とバージョンは固定値が入る() {
        service.importReportFile(koinFile(), JICHITAI_CD, USER_ID);

        MapSqlParameterSource params = capturedParams();
        assertThat(params.getValue("seq")).isEqualTo(1L);
        assertThat(params.getValue("version")).isEqualTo(1);
    }
}
