package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.constant.ReportsConstants.ReportsOutputField;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.ReportsDefId;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.service.impl.ReportsOutputConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReportsOutputConfigServiceImplTest {

    private static final String JICHITAI_CD = "01100";
    private static final String USER_ID = "U001";
    private static final int FIELD_COUNT = ReportsOutputField.values().length;

    @InjectMocks ReportsOutputConfigServiceImpl service;

    @Mock ReportsDefRepository reportsDefRepository;

    // ─── getDefTextMap ───────────────────────────────────────────────────────

    @Test
    @DisplayName("#7 getDefTextMap 正常系 全項目にレコードが存在する場合：各項目に定義テキストが設定される")
    void 確認7_getDefTextMap_全項目存在() {
        when(reportsDefRepository.findById(new ReportsDefId(JICHITAI_CD, "RPT0000002")))
                .thenReturn(Optional.of(def(JICHITAI_CD, "RPT0000002", "第1条")));
        when(reportsDefRepository.findById(argThat(id ->
                id != null && !id.getId().equals("RPT0000002"))))
                .thenReturn(Optional.of(def(JICHITAI_CD, "OTHER", "定義テキスト")));

        Map<ReportsOutputField, String> result = service.getDefTextMap(JICHITAI_CD);

        assertThat(result).hasSize(FIELD_COUNT);
        assertThat(result.get(ReportsOutputField.TOKUGIMU_SHITEI_JOREI)).isEqualTo("第1条");
        assertThat(result.get(ReportsOutputField.TOKUGIMU_JURI_JOREI)).isEqualTo("定義テキスト");
        verify(reportsDefRepository, times(FIELD_COUNT)).findById(any(ReportsDefId.class));
    }

    @Test
    @DisplayName("#8 getDefTextMap 異常系 一部の項目にレコードが存在しない場合：その項目は空文字となる")
    void 確認8_getDefTextMap_一部存在しない() {
        when(reportsDefRepository.findById(new ReportsDefId(JICHITAI_CD, "RPT0000002")))
                .thenReturn(Optional.empty());
        when(reportsDefRepository.findById(argThat(id ->
                id != null && !id.getId().equals("RPT0000002"))))
                .thenReturn(Optional.of(def(JICHITAI_CD, "OTHER", "定義テキスト")));

        Map<ReportsOutputField, String> result = service.getDefTextMap(JICHITAI_CD);

        assertThat(result.get(ReportsOutputField.TOKUGIMU_SHITEI_JOREI)).isEqualTo("");
        assertThat(result.get(ReportsOutputField.TOKUGIMU_JURI_JOREI)).isEqualTo("定義テキスト");
        assertThat(result).hasSize(FIELD_COUNT);
    }

    @Test
    @DisplayName("#9 getDefTextMap 異常系 全項目のレコードが存在しない場合：全項目が空文字となる")
    void 確認9_getDefTextMap_全項目存在しない() {
        when(reportsDefRepository.findById(any(ReportsDefId.class))).thenReturn(Optional.empty());

        Map<ReportsOutputField, String> result = assertDoesNotThrow(
                () -> service.getDefTextMap(JICHITAI_CD));

        assertThat(result).hasSize(FIELD_COUNT);
        assertThat(result.values()).allMatch(v -> v.equals(""));
    }

    @Test
    @DisplayName("#10 getDefTextMap 異常系 レコードは存在するが定義テキストが null の場合")
    void 確認10_getDefTextMap_defTextがnull() {
        when(reportsDefRepository.findById(any(ReportsDefId.class)))
                .thenReturn(Optional.of(def(JICHITAI_CD, "RPT0000002", null)));

        Map<ReportsOutputField, String> result = assertDoesNotThrow(
                () -> service.getDefTextMap(JICHITAI_CD));

        assertThat(result).hasSize(FIELD_COUNT);
        assertThat(result.values()).allMatch(v -> v == null);
    }

    @Test
    @DisplayName("#11 getDefTextMap 正常系 戻り値の並び順が ReportsOutputField の宣言順であること")
    void 確認11_getDefTextMap_並び順() {
        when(reportsDefRepository.findById(any(ReportsDefId.class)))
                .thenReturn(Optional.of(def(JICHITAI_CD, "ANY", "定義テキスト")));

        Map<ReportsOutputField, String> result = service.getDefTextMap(JICHITAI_CD);

        List<ReportsOutputField> keys = List.copyOf(result.keySet());
        ReportsOutputField[] expected = ReportsOutputField.values();
        for (int i = 0; i < expected.length; i++) {
            assertThat(keys.get(i)).isEqualTo(expected[i]);
        }
        assertThat(keys.get(0)).isEqualTo(ReportsOutputField.TOKUGIMU_SHITEI_JOREI);
        assertThat(keys.get(keys.size() - 1)).isEqualTo(ReportsOutputField.KANPU_MENJO_SHINSEI_JOREI);
    }

    // ─── saveDefText ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("#12 saveDefText 正常系 既存レコードがある場合：定義テキスト・区分が更新される")
    void 確認12_saveDefText_既存レコード更新() {
        when(reportsDefRepository.findById(new ReportsDefId(JICHITAI_CD, "RPT0000002")))
                .thenReturn(Optional.of(defWithKbn(JICHITAI_CD, "RPT0000002", "旧テキスト", "1", "OLD",
                        LocalDateTime.of(2026, 1, 1, 0, 0))));
        when(reportsDefRepository.findById(argThat(id ->
                id != null && !"RPT0000002".equals(id.getId()))))
                .thenReturn(Optional.of(def(JICHITAI_CD, "OTHER", "その他")));
        when(reportsDefRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> defTextMap = new LinkedHashMap<>();
        defTextMap.put("RPT0000002", "新テキスト");

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        service.saveDefText(JICHITAI_CD, USER_ID, defTextMap);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        ArgumentCaptor<ReportsDef> captor = ArgumentCaptor.forClass(ReportsDef.class);
        verify(reportsDefRepository, times(FIELD_COUNT)).save(captor.capture());

        ReportsDef saved = captor.getAllValues().stream()
                .filter(d -> "RPT0000002".equals(d.getId()))
                .findFirst().orElseThrow();
        assertThat(saved.getDefText()).isEqualTo("新テキスト");
        assertThat(saved.getKbn()).isEqualTo("1");
        assertThat(saved.getUpdUser()).isEqualTo(USER_ID);
        assertThat(saved.getUpdDt()).isBetween(before, after);
    }

    @Test
    @DisplayName("#13 saveDefText 正常系 既存レコードが無い場合：新規エンティティが生成されて保存される")
    void 確認13_saveDefText_新規作成() {
        when(reportsDefRepository.findById(any(ReportsDefId.class))).thenReturn(Optional.empty());
        when(reportsDefRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> defTextMap = new LinkedHashMap<>();
        defTextMap.put("RPT0000002", "新テキスト");

        service.saveDefText(JICHITAI_CD, USER_ID, defTextMap);

        ArgumentCaptor<ReportsDef> captor = ArgumentCaptor.forClass(ReportsDef.class);
        verify(reportsDefRepository, times(FIELD_COUNT)).save(captor.capture());

        ReportsDef saved = captor.getAllValues().stream()
                .filter(d -> "RPT0000002".equals(d.getId()))
                .findFirst().orElseThrow();
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getId()).isEqualTo("RPT0000002");
        assertThat(saved.getKbn()).isEqualTo("1");
        assertThat(saved.getDefText()).isEqualTo("新テキスト");
    }

    @Test
    @DisplayName("#14 saveDefText 正常系 defTextMap に含まれない項目：空文字で保存される")
    void 確認14_saveDefText_未指定項目は空文字() {
        when(reportsDefRepository.findById(any(ReportsDefId.class))).thenReturn(Optional.empty());
        when(reportsDefRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> defTextMap = new LinkedHashMap<>();
        defTextMap.put("RPT0000002", "新テキスト");

        service.saveDefText(JICHITAI_CD, USER_ID, defTextMap);

        ArgumentCaptor<ReportsDef> captor = ArgumentCaptor.forClass(ReportsDef.class);
        verify(reportsDefRepository, times(FIELD_COUNT)).save(captor.capture());

        ReportsDef rpt3 = captor.getAllValues().stream()
                .filter(d -> "RPT0000003".equals(d.getId()))
                .findFirst().orElseThrow();
        assertThat(rpt3.getDefText()).isEqualTo("");

        ReportsDef rpt2 = captor.getAllValues().stream()
                .filter(d -> "RPT0000002".equals(d.getId()))
                .findFirst().orElseThrow();
        assertThat(rpt2.getDefText()).isEqualTo("新テキスト");
    }

    @Test
    @DisplayName("#15 saveDefText 異常系 defTextMap が空の場合：全項目が空文字で保存される")
    void 確認15_saveDefText_defTextMapが空() {
        when(reportsDefRepository.findById(any(ReportsDefId.class))).thenReturn(Optional.empty());
        when(reportsDefRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.saveDefText(JICHITAI_CD, USER_ID, new LinkedHashMap<>()));

        ArgumentCaptor<ReportsDef> captor = ArgumentCaptor.forClass(ReportsDef.class);
        verify(reportsDefRepository, times(FIELD_COUNT)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(d -> "".equals(d.getDefText()));
    }

    @Test
    @DisplayName("#16 saveDefText 異常系 defTextMap の値が空文字の場合：空文字のまま保存される")
    void 確認16_saveDefText_値が空文字() {
        ReportsDef existing = def(JICHITAI_CD, "RPT0000002", "旧テキスト");
        existing.setKbn("1");
        when(reportsDefRepository.findById(any(ReportsDefId.class)))
                .thenReturn(Optional.of(existing));
        when(reportsDefRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> defTextMap = new LinkedHashMap<>();
        defTextMap.put("RPT0000002", "");

        service.saveDefText(JICHITAI_CD, USER_ID, defTextMap);

        ArgumentCaptor<ReportsDef> captor = ArgumentCaptor.forClass(ReportsDef.class);
        verify(reportsDefRepository, times(FIELD_COUNT)).save(captor.capture());

        ReportsDef saved = captor.getAllValues().stream()
                .filter(d -> "RPT0000002".equals(d.getId()))
                .findFirst().orElseThrow();
        assertThat(saved.getDefText()).isEqualTo("");
    }

    @Test
    @DisplayName("#17 saveDefText 異常系 ReportsOutputField に存在しないIDが含まれる場合：無視される")
    void 確認17_saveDefText_存在しないIDは無視() {
        when(reportsDefRepository.findById(any(ReportsDefId.class))).thenReturn(Optional.empty());
        when(reportsDefRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> defTextMap = new LinkedHashMap<>();
        defTextMap.put("RPT0000002", "新テキスト");
        defTextMap.put("RPT9999999", "無関係");

        service.saveDefText(JICHITAI_CD, USER_ID, defTextMap);

        ArgumentCaptor<ReportsDef> captor = ArgumentCaptor.forClass(ReportsDef.class);
        verify(reportsDefRepository, times(FIELD_COUNT)).save(captor.capture());
        assertThat(captor.getAllValues())
                .noneMatch(d -> "RPT9999999".equals(d.getId()));
    }

    @Test
    @DisplayName("#18 saveDefText 正常系 既存レコードの区分が誤っている場合：\"1\"（テキスト）に上書きされる")
    void 確認18_saveDefText_kbnが上書きされる() {
        ReportsDef existing = def(JICHITAI_CD, "RPT0000002", "旧テキスト");
        existing.setKbn("2");
        when(reportsDefRepository.findById(any(ReportsDefId.class)))
                .thenReturn(Optional.of(existing));
        when(reportsDefRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> defTextMap = new LinkedHashMap<>();
        defTextMap.put("RPT0000002", "新テキスト");

        service.saveDefText(JICHITAI_CD, USER_ID, defTextMap);

        ArgumentCaptor<ReportsDef> captor = ArgumentCaptor.forClass(ReportsDef.class);
        verify(reportsDefRepository, times(FIELD_COUNT)).save(captor.capture());
        assertThat(captor.getAllValues())
                .allMatch(d -> ReportsConstants.KBN_TEXT.equals(d.getKbn()));
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private ReportsDef def(String jichitaiCd, String id, String defText) {
        ReportsDef d = new ReportsDef();
        d.setJichitaiCd(jichitaiCd);
        d.setId(id);
        d.setDefText(defText);
        return d;
    }

    private ReportsDef defWithKbn(String jichitaiCd, String id, String defText, String kbn,
            String addUser, LocalDateTime addDt) {
        ReportsDef d = def(jichitaiCd, id, defText);
        d.setKbn(kbn);
        d.setAddUser(addUser);
        d.setAddDt(addDt);
        return d;
    }
}
