package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.dto.AtenaImportPreviewDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;
import jp.lg.asp.accommodation.entity.AtenaRenkei;
import jp.lg.asp.accommodation.entity.AtenaRenkeiDef;
import jp.lg.asp.accommodation.repository.AtenaRenkeiDefRepository;
import jp.lg.asp.accommodation.repository.AtenaRenkeiRepository;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.service.impl.AtenaImportServiceImpl;
import jp.lg.asp.accommodation.util.HashUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtenaImportServiceImplTest {

    @Mock AtenaRepository atenaRepository;
    @Mock AtenaRenkeiRepository atenaRenkeiRepository;
    @Mock AtenaRenkeiDefRepository atenaRenkeiDefRepository;
    @Mock HashUtil hashUtil;
    @Mock MultipartFile file;

    @InjectMocks AtenaImportServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String USER_ID = "user01";
    private static final String VALID_HEADER = "宛名番号,個人番号,法人番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\n";

    @BeforeEach
    void setUp() {
        when(atenaRenkeiRepository.findMaxSeqByJichitaiCd(JICHITAI_CD)).thenReturn(BigDecimal.ZERO);
        when(atenaRenkeiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(atenaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ============================================================
    // 解析フェーズ
    // ============================================================

    @Test
    void analyze_既存データが無い場合は新規として扱う() throws Exception {
        String csv = VALID_HEADER + "1001,,,テスト太郎,テストタロウ,060-0001,札幌市,011-111-1111\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));
        when(file.getOriginalFilename()).thenReturn("test.csv");
        when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.empty());

        AtenaImportPreviewDto preview = service.analyze(file, JICHITAI_CD);

        assertThat(preview.getRows()).hasSize(1);
        assertThat(preview.getRows().get(0).isShinki()).isTrue();
        // 新規は差分確認の対象外
        assertThat(preview.getRows().get(0).isSabunAri()).isFalse();
        assertThat(preview.getShinkiKensu()).isEqualTo(1);
        // 解析フェーズではDBを更新しない
        verify(atenaRepository, never()).save(any());
    }

    @Test
    void analyze_既存データと差異がある場合は差分ありとする() throws Exception {
        String csv = VALID_HEADER + "1001,,,テスト太郎,テストタロウ,060-0001,札幌市,011-111-1111\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));
        when(file.getOriginalFilename()).thenReturn("test.csv");

        Atena current = new Atena();
        current.setName("旧太郎");
        current.setNameKana("テストタロウ");
        current.setYubinNo("060-0001");
        current.setJusho("札幌市");
        current.setTel1("011-111-1111");
        when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.of(current));

        AtenaImportPreviewDto preview = service.analyze(file, JICHITAI_CD);

        assertThat(preview.getRows().get(0).isSabunAri()).isTrue();
        assertThat(preview.getSabunKensu()).isEqualTo(1);
        assertThat(preview.getRows().get(0).getDiffs())
                .filteredOn("changed", true)
                .extracting("label")
                .containsExactly("氏名/名称");
    }

    @Test
    void analyze_既存データと同一の場合は差異なしとする() throws Exception {
        String csv = VALID_HEADER + "1001,,,テスト太郎,テストタロウ,060-0001,札幌市,011-111-1111\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));
        when(file.getOriginalFilename()).thenReturn("test.csv");

        Atena current = new Atena();
        current.setName("テスト太郎");
        current.setNameKana("テストタロウ");
        current.setYubinNo("060-0001");
        current.setJusho("札幌市");
        current.setTel1("011-111-1111");
        when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.of(current));

        AtenaImportPreviewDto preview = service.analyze(file, JICHITAI_CD);

        assertThat(preview.getRows().get(0).isSabunAri()).isFalse();
        assertThat(preview.getSaiNashiKensu()).isEqualTo(1);
    }

    @Test
    void analyze_空ファイルは例外() throws Exception {
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        assertThatThrownBy(() -> service.analyze(file, JICHITAI_CD))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CSVファイルが空");
    }

    @Test
    void analyze_ヘッダー不正は例外() throws Exception {
        String csv = "不正ヘッダー,col2,col3,col4,col5,col6,col7,col8\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));

        assertThatThrownBy(() -> service.analyze(file, JICHITAI_CD))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void analyze_データ行の宛名番号空は例外() throws Exception {
        String csv = VALID_HEADER + ",,,テスト太郎,テストタロウ,060-0001,札幌市,011-111-1111\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));

        assertThatThrownBy(() -> service.analyze(file, JICHITAI_CD))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("宛名番号");
    }

    // ============================================================
    // 確定フェーズ
    // ============================================================

    @Test
    void confirm_新規は選択に関わらず登録する() throws Exception {
        AtenaImportPreviewDto preview = analyzeShinki();

        AtenaRenkei result = service.confirm(preview, Set.of(), JICHITAI_CD, USER_ID);

        assertThat(result.getShinkiKensu()).isEqualTo(BigDecimal.valueOf(1));
        assertThat(result.getKoshinKensu()).isEqualTo(BigDecimal.valueOf(0));
        verify(atenaRepository).save(any(Atena.class));
        verify(atenaRenkeiDefRepository).save(argThat(
                (AtenaRenkeiDef d) -> AtenaRenkeiDef.KBN_TORIKOMI.equals(d.getKbn())));
    }

    @Test
    void confirm_差分ありで選択された宛名は更新する() throws Exception {
        AtenaImportPreviewDto preview = analyzeSabunAri();

        AtenaRenkei result = service.confirm(preview, Set.of("1001"), JICHITAI_CD, USER_ID);

        assertThat(result.getKoshinKensu()).isEqualTo(BigDecimal.valueOf(1));
        verify(atenaRepository).save(any(Atena.class));
        verify(atenaRenkeiDefRepository).save(argThat(
                (AtenaRenkeiDef d) -> AtenaRenkeiDef.KBN_TORIKOMI.equals(d.getKbn())));
    }

    @Test
    void confirm_差分ありで選択されなかった宛名はスキップする() throws Exception {
        AtenaImportPreviewDto preview = analyzeSabunAri();

        AtenaRenkei result = service.confirm(preview, Set.of(), JICHITAI_CD, USER_ID);

        assertThat(result.getShoriKensu()).isEqualTo(BigDecimal.valueOf(0));
        verify(atenaRepository, never()).save(any());
        verify(atenaRenkeiDefRepository).save(argThat(
                (AtenaRenkeiDef d) -> AtenaRenkeiDef.KBN_SKIP.equals(d.getKbn())));
    }

    @Test
    void confirm_差異なしは更新せず差異なしとして記録する() throws Exception {
        String csv = VALID_HEADER + "1001,,,テスト太郎,テストタロウ,060-0001,札幌市,011-111-1111\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));
        when(file.getOriginalFilename()).thenReturn("test.csv");
        Atena current = new Atena();
        current.setName("テスト太郎");
        current.setNameKana("テストタロウ");
        current.setYubinNo("060-0001");
        current.setJusho("札幌市");
        current.setTel1("011-111-1111");
        when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.of(current));
        AtenaImportPreviewDto preview = service.analyze(file, JICHITAI_CD);

        AtenaRenkei result = service.confirm(preview, Set.of(), JICHITAI_CD, USER_ID);

        assertThat(result.getShoriKensu()).isEqualTo(BigDecimal.valueOf(0));
        verify(atenaRepository, never()).save(any());
        verify(atenaRenkeiDefRepository).save(argThat(
                (AtenaRenkeiDef d) -> AtenaRenkeiDef.KBN_SAI_NASHI.equals(d.getKbn())));
    }

    // ============================================================
    // 参照
    // ============================================================

    @Test
    void findHistory_履歴一覧取得() {
        List<AtenaRenkei> expected = List.of(new AtenaRenkei());
        when(atenaRenkeiRepository.findByJichitaiCdOrderBySeqDesc(JICHITAI_CD)).thenReturn(expected);

        List<AtenaRenkei> result = service.findHistory(JICHITAI_CD);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findDetail_明細取得() {
        List<AtenaRenkeiDef> expected = List.of(new AtenaRenkeiDef());
        when(atenaRenkeiDefRepository.findByJichitaiCdAndSeqOrderByAtenaNoAsc(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(expected);

        List<AtenaRenkeiDef> result = service.findDetail(JICHITAI_CD, BigDecimal.ONE);

        assertThat(result).isEqualTo(expected);
    }

    // ============================================================
    // ヘルパー
    // ============================================================

    private AtenaImportPreviewDto analyzeShinki() throws Exception {
        String csv = VALID_HEADER + "1001,,,テスト太郎,テストタロウ,060-0001,札幌市,011-111-1111\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));
        when(file.getOriginalFilename()).thenReturn("test.csv");
        when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.empty());
        return service.analyze(file, JICHITAI_CD);
    }

    private AtenaImportPreviewDto analyzeSabunAri() throws Exception {
        String csv = VALID_HEADER + "1001,,,テスト太郎,テストタロウ,060-0001,札幌市,011-111-1111\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));
        when(file.getOriginalFilename()).thenReturn("test.csv");
        Atena current = new Atena();
        current.setName("旧太郎");
        when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.of(current));
        return service.analyze(file, JICHITAI_CD);
    }
}
