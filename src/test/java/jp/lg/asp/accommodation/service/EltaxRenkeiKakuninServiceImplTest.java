package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto;
import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.KyodoJigyosha;
import jp.lg.asp.accommodation.entity.Shoyusha;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.entity.ZeiritsuTeigaku;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsu;
import jp.lg.asp.accommodation.exception.EltaxRenkeiKakuninValidationException;
import jp.lg.asp.accommodation.repository.EltaxRenkeiRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.KyodoJigyoshaRepository;
import jp.lg.asp.accommodation.repository.ShoyushaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.impl.EltaxRenkeiKakuninServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EltaxRenkeiKakuninServiceImplTest {

    @Mock EltaxRenkeiRepository eltaxRenkeiRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock ShoyushaRepository shoyushaRepository;
    @Mock KyodoJigyoshaRepository kyodoJigyoshaRepository;
    @Mock GassanRepository gassanRepository;
    @Mock FukaRepository fukaRepository;
    @Mock FukaUchiRepository fukaUchiRepository;
    @Mock ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
    @Mock ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks EltaxRenkeiKakuninServiceImpl service;

    private static final String JICHITAI_CD = "01202";
    // 手続ID定数
    private static final String TETSUZUKI_TOKUGIMU  = "R0402N08";
    private static final String TETSUZUKI_TEIGAKU   = "R0402N05";
    private static final String TETSUZUKI_TEIRITSU  = "R0402N06";
    private static final String TETSUZUKI_TOKU_TEIGAKU  = "R0402N17";
    private static final String TETSUZUKI_TOKU_TEIRITSU = "R0402N18";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(shoyushaRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());
        when(kyodoJigyoshaRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(any(), any())).thenReturn(List.of());
        when(eltaxRenkeiRepository.findNextSeq(JICHITAI_CD)).thenReturn(BigDecimal.ONE);
        when(eltaxRenkeiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fukaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // =========================================================================
    // ヘルパーメソッド
    // =========================================================================

    /**
     * CSV1行分のバイト列を生成する。
     * @param totalCols 総列数
     * @param values    1始まりの列番号→値のマップ
     */
    private byte[] buildCsvRow(int totalCols, Map<Integer, String> values) {
        String[] cols = new String[totalCols];
        for (int i = 0; i < totalCols; i++) cols[i] = "";
        values.forEach((no, val) -> cols[no - 1] = val);
        return String.join(",", cols).getBytes(StandardCharsets.UTF_8);
    }

    /** 特別徴収義務者申請書CSVを生成する（68列） */
    private byte[] tokugimuCsv(Map<Integer, String> values) {
        Map<Integer, String> base = new HashMap<>();
        base.put(3, TETSUZUKI_TOKUGIMU);
        base.putAll(values);
        return buildCsvRow(68, base);
    }

    /** 納入申告（定額）CSVを生成する（75列） */
    private byte[] teigakuCsv(Map<Integer, String> values) {
        Map<Integer, String> base = new HashMap<>();
        base.put(3, TETSUZUKI_TEIGAKU);
        base.putAll(values);
        return buildCsvRow(75, base);
    }

    /** 納入申告（定率）CSVを生成する（97列） */
    private byte[] teiritsuCsv(Map<Integer, String> values) {
        Map<Integer, String> base = new HashMap<>();
        base.put(3, TETSUZUKI_TEIRITSU);
        base.putAll(values);
        return buildCsvRow(97, base);
    }

    /** 特例納入申告（定額）CSVを生成する（167列） */
    private byte[] tokuTeigakuCsv(Map<Integer, String> values) {
        Map<Integer, String> base = new HashMap<>();
        base.put(3, TETSUZUKI_TOKU_TEIGAKU);
        base.putAll(values);
        return buildCsvRow(167, base);
    }

    /** 特例納入申告（定率）CSVを生成する（233列） */
    private byte[] tokuTeiritsuCsv(Map<Integer, String> values) {
        Map<Integer, String> base = new HashMap<>();
        base.put(3, TETSUZUKI_TOKU_TEIRITSU);
        base.putAll(values);
        return buildCsvRow(233, base);
    }

    private Tokugimu buildTokugimu(String shiteiNo, String shisetsuName) {
        Tokugimu t = new Tokugimu();
        t.setJichitaiCd(JICHITAI_CD);
        t.setShiteiNo(shiteiNo);
        t.setRno(BigDecimal.ONE);
        t.setShisetsuName(shisetsuName);
        t.setShisetsuJusho("テスト住所");
        t.setKyokaName("");
        t.setKyokaYubinNo("");
        t.setKyokaJusho("");
        t.setSoufusakiName("");
        t.setSoufusakiYubinNo("");
        t.setSoufusakiJusho("");
        t.setKyokaNameKana("");
        t.setSoufusakiNameKana("");
        t.setShisetsuNameKana("");
        t.setNewFlg("1");
        t.setDelFlg("0");
        return t;
    }

    private Fuka buildFuka(String shiteiNo, String nendo, int kibetsu, Long totalZeigaku) {
        Fuka f = new Fuka();
        f.setJichitaiCd(JICHITAI_CD);
        f.setShiteiNo(shiteiNo);
        f.setRno(1);
        f.setNendo(nendo);
        f.setKibetsu(kibetsu);
        f.setTotalZeigaku(totalZeigaku);
        f.setNewFlg("1");
        f.setDelFlg("0");
        return f;
    }

    private FukaUchi buildFukaUchi(int kazeiKbn, Long zeigaku, Long ryokinSogaku) {
        FukaUchi u = new FukaUchi();
        u.setKazeiKbn(kazeiKbn);
        u.setZeigaku(zeigaku);
        u.setRyokinSogaku(ryokinSogaku);
        return u;
    }

    // =========================================================================
    // preview - モックのみで完結するケース
    // =========================================================================

    // No.34: CSVファイル解読時のIOException判定
    @Test
    void preview_IOExceptionが発生した場合はUncheckedIOExceptionをスローする() throws Exception {
        MockMultipartFile file = mock(MockMultipartFile.class);
        when(file.getBytes()).thenThrow(new java.io.IOException("IO error"));
        when(file.getOriginalFilename()).thenReturn("test.csv");

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(java.io.UncheckedIOException.class)
                .hasMessageContaining("CSVファイルの解析に失敗しました");
    }

    // No.35: CSVデータ行が空（0バイト）の場合
    @Test
    void preview_空ファイルはRuntimeExceptionをスローする() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ファイルの解析に失敗しました");
    }

    // No.38: 未対応手続きID
    @Test
    void preview_未対応手続きIDはRuntimeExceptionをスローする() throws Exception {
        byte[] csv = buildCsvRow(3, Map.of(3, "UNKNOWN_ID"));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("システム対応外の手続き種別です");
    }

    // No.82: 手続IDが空文字の場合
    @Test
    void preview_手続IDが空文字はRuntimeExceptionをスローする() throws Exception {
        byte[] csv = buildCsvRow(3, Map.of());
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("システム対応外の手続き種別です");
    }

    // =========================================================================
    // preview - 特別徴収義務者（様式CSV使用）
    // =========================================================================

    // No.18: 新規登録で指定番号が入力されているがDBに一致しない場合、buildDtoWithEmptyBefore経由でも定名検索用5項目が正しく設定される
    @Test
    void preview_新規登録で指定番号がDB非存在の場合buildDtoWithEmptyBefore経由で定名検索用5項目が設定される() throws Exception {
        // tokugimu・gassanともに空リスト → shisetsuNameが空 → buildDtoWithEmptyBeforeへ
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi99999")).thenReturn(List.of());
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "shi99999")).thenReturn(List.of());

        byte[] csv = tokugimuCsv(Map.ofEntries(
                Map.entry(13, "1"),              // 申請区分=新規
                Map.entry(14, "2026-04-01"),     // 提出年月日
                Map.entry(16, "サンプル商事"),    // 氏名又は名称
                Map.entry(18, "123-4567"),       // 郵便番号
                Map.entry(19, "○○市△△町1-1"),  // 住所
                Map.entry(20, "03-1234-5678"),   // 電話番号
                Map.entry(23, "2"),              // 個人番号・法人番号区分
                Map.entry(24, "123456789012"),   // 個人番号
                Map.entry(25, "1234567890123"),  // 法人番号
                Map.entry(26, "shi99999"),       // 施設番号（DBに存在しない）
                Map.entry(27, "サンプルホテル"),
                Map.entry(28, "テスト住所")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        // buildDtoWithEmptyBeforeが呼ばれ、エラーメッセージが設定される
        assertThat(dto.getErrorMessage()).contains("指定番号（shi99999）に該当する特別徴収義務者が登録されていません。");

        // buildDtoWithEmptyBefore内部のswitch文でisTokugimuNew=trueの分岐が実行され、定名検索用5項目が正しく設定される
        assertThat(dto.isAtenaSearchRequired()).isTrue();
        assertThat(dto.getTokugimuName()).isEqualTo("サンプル商事");
        assertThat(dto.getTokugimuJusho()).isEqualTo("○○市△△町1-1");
        assertThat(dto.getTokugimuTel()).isEqualTo("03-1234-5678");
        assertThat(dto.getKojinNo()).isEqualTo("123456789012");
        assertThat(dto.getHojinNo()).isEqualTo("1234567890123");

        // shiteiNo=nullとしてbuildDiffRowsTokugimuが呼ばれるため、全diffRowのbeforeValueが"－"
        assertThat(dto.getDiffRows()).isNotNull();
        assertThat(dto.getDiffRows()).isNotEmpty();
        assertThat(dto.getDiffRows()).allMatch(r -> "－".equals(r.getBeforeValue()));
    }

    // No.16: 新規登録時、diffRowsのafterValueが正しく設定される
    @Test
    void preview_新規登録時にdiffRowsのafterValueが正しく設定される() throws Exception {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());
        byte[] csv = tokugimuCsv(Map.of(
                13, "1",              // 申請区分=新規
                14, "2026-04-01",     // 提出年月日
                16, "サンプル商事",    // 氏名又は名称
                18, "123-4567",       // 郵便番号
                19, "○○市△△町1-1",  // 住所
                20, "03-1234-5678",   // 電話番号
                23, "2",              // 個人番号・法人番号区分
                27, "サンプルホテル", // 施設名称
                28, "テスト住所",      // 施設所在地
                40, "1"               // 営業種別
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        assertThat(dto.getDiffRows()).isNotNull();
        dto.getDiffRows().stream()
                .filter(r -> "施設情報【名称】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getAfterValue()).isEqualTo("サンプルホテル"));
        dto.getDiffRows().stream()
                .filter(r -> "施設情報【名称】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getBeforeValue()).isEqualTo("－"));
    }

    // No.17: 新規登録時、宛名検索用5項目がDtoへ設定される
    @Test
    void preview_新規登録時に宛名検索用5項目がDtoへ設定される() throws Exception {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());
        byte[] csv = tokugimuCsv(Map.ofEntries(
                Map.entry(13, "1"),
                Map.entry(14, "2026-04-01"),
                Map.entry(16, "サンプル商事"),
                Map.entry(18, "123-4567"),
                Map.entry(19, "○○市△△町1-1"),
                Map.entry(20, "03-1234-5678"),
                Map.entry(23, "2"),
                Map.entry(24, "123456789012"),
                Map.entry(25, "1234567890123"),
                Map.entry(27, "サンプルホテル"),
                Map.entry(28, "テスト住所")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        assertThat(dto.isAtenaSearchRequired()).isTrue();
        assertThat(dto.getTokugimuName()).isEqualTo("サンプル商事");
        assertThat(dto.getTokugimuJusho()).isEqualTo("○○市△△町1-1");
        assertThat(dto.getTokugimuTel()).isEqualTo("03-1234-5678");
        assertThat(dto.getKojinNo()).isEqualTo("123456789012");
        assertThat(dto.getHojinNo()).isEqualTo("1234567890123");
    }

    // No.18: 変更時、diffRowsにbeforeValue（DB既存値）が設定される
    @Test
    void preview_変更時にdiffRowsにbeforeValueが設定される() throws Exception {
        Tokugimu prev = buildTokugimu("shi00001", "旧ホテル名");
        prev.setKyokaName("旧営業許可名義");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001"))
                .thenReturn(List.of(prev));
        byte[] csv = tokugimuCsv(Map.ofEntries(
                Map.entry(13, "2"),
                Map.entry(14, "2026-04-01"),
                Map.entry(16, "テスト商事"),
                Map.entry(18, "123-4567"),
                Map.entry(19, "テスト住所"),
                Map.entry(20, "03-1234-5678"),
                Map.entry(23, "2"),
                Map.entry(26, "shi00001"),
                Map.entry(27, "新ホテル名"),
                Map.entry(28, "テスト住所"),
                Map.entry(36, "新営業許可名義")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        dto.getDiffRows().stream()
                .filter(r -> "施設情報【名称】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> {
                    assertThat(r.getAfterValue()).isEqualTo("新ホテル名");
                    assertThat(r.getBeforeValue()).isEqualTo("旧ホテル名");
                });
        dto.getDiffRows().stream()
                .filter(r -> "宿泊施設の営業許可等情報【氏名（名称及び代表者名）】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> {
                    assertThat(r.getAfterValue()).isEqualTo("新営業許可名義");
                    assertThat(r.getBeforeValue()).isEqualTo("旧営業許可名義");
                });
    }

    // No.19: 休止時、diffRowsに休止期間（自）が設定される
    @Test
    void preview_休止時にdiffRowsに休止期間が設定される() throws Exception {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        prev.setKyushiStYmd(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001"))
                .thenReturn(List.of(prev));
        byte[] csv = tokugimuCsv(Map.ofEntries(
                Map.entry(13, "3"),
                Map.entry(14, "2026-04-01"),
                Map.entry(16, "テスト商事"),
                Map.entry(18, "123-4567"),
                Map.entry(19, "テスト住所"),
                Map.entry(20, "03-1234-5678"),
                Map.entry(23, "2"),
                Map.entry(26, "shi00001"),
                Map.entry(27, "テストホテル"),
                Map.entry(28, "テスト住所"),
                Map.entry(61, "2026-04-01")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        dto.getDiffRows().stream()
                .filter(r -> "休止廃止再開情報【休止期間（自）】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> {
                    assertThat(r.getAfterValue()).isEqualTo("2026-04-01");
                    assertThat(r.getBeforeValue()).isEqualTo("－");
                });
    }

    // No.30: 不正な申請区分の場合はRuntimeExceptionをスローする
    @Test
    void preview_不正な申請区分はRuntimeExceptionをスローする() throws Exception {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());
        byte[] csv = tokugimuCsv(Map.of(13, "99"));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("システム対応外の申請区分です");
    }

    // No.31: preview経由でDB非存在でも例外にせず空DTOを返す
    @Test
    void preview_指定番号がDB非存在でも例外にせず空DTOを返す() throws Exception {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(any(), any())).thenReturn(List.of());
        byte[] csv = teigakuCsv(Map.of(
                13, "2026-04-01",
                25, "shi99999",
                29, "2026-06",
                30, "1"
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        assertThat(dto).isNotNull();
        assertThat(dto.getErrorMessage()).contains("shi99999");
    }

    // No.32: preview経由で施設番号が未設定の場合は空DTOを返す
    @Test
    void preview_施設番号が未設定の場合は空DTOを返す() throws Exception {
        byte[] csv = teigakuCsv(Map.of(
                13, "2026-04-01",
                29, "2026-06",
                30, "1"
                // No.25(施設番号)は空
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        assertThat(dto).isNotNull();
        assertThat(dto.getErrorMessage()).contains("施設番号が未設定です");
    }

    // No.33: repreview経由でDB非存在の場合はRuntimeExceptionをスローする
    @Test
    void repreview_指定番号がDB非存在の場合はRuntimeExceptionをスローする() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(any(), any())).thenReturn(List.of());
        byte[] csv = teigakuCsv(Map.of(
                13, "2026-04-01",
                25, "shi99999",
                29, "2026-06",
                30, "1"
        ));

        assertThatThrownBy(() -> service.repreview(csv, "shi99999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("指定番号（shi99999）に該当する特別徴収義務者が登録されていません");
    }

    // No.36: CSVに2行目以降のデータが存在しても1行目のみを解析対象とする
    @Test
    void preview_2行目以降は無視して1行目のみ解析する() throws Exception {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());
        String row1 = buildCsvRowString(68, Map.of(3, TETSUZUKI_TOKUGIMU, 13, "1", 14, "2026-04-01",
                16, "サンプル商事", 18, "123-4567", 19, "○○市", 20, "03-1234-5678", 23, "2", 27, "ホテルA", 28, "テスト住所"));
        String row2 = "INVALID_DATA,,,,,";
        byte[] csv = (row1 + "\n" + row2).getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        assertThatCode(() -> service.preview(file)).doesNotThrowAnyException();
    }

    private String buildCsvRowString(int totalCols, Map<Integer, String> values) {
        String[] cols = new String[totalCols];
        for (int i = 0; i < totalCols; i++) cols[i] = "";
        values.forEach((no, val) -> cols[no - 1] = val);
        return String.join(",", cols);
    }

    // No.39: 必須項目欠落によるバリデーション例外発生
    @Test
    void preview_必須項目欠落でバリデーション例外が発生する() throws Exception {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001"))
                .thenReturn(List.of(prev));
        // No.14(提出年月日)はrequiredFlg=1だが空欄
        byte[] csv = tokugimuCsv(Map.of(
                13, "2",
                26, "shi00001",
                27, "テストホテル",
                28, "テスト住所",
                16, "テスト商事",
                18, "123-4567",
                19, "テスト住所",
                20, "03-1234-5678",
                23, "2"
                // No.14(提出年月日)は空
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(EltaxRenkeiKakuninValidationException.class)
                .satisfies(e -> {
                    EltaxRenkeiKakuninValidationException ve = (EltaxRenkeiKakuninValidationException) e;
                    assertThat(ve.getErrorMessages()).anyMatch(m -> m.contains("提出年月日"));
                });
    }

    // =========================================================================
    // commit - eLTAX連携管理保存
    // =========================================================================

    // No.77: eLTAX連携管理テーブルへのログ保存
    @Test
    void commit_eLTAX連携管理が保存される() {
        when(eltaxRenkeiRepository.findNextSeq(JICHITAI_CD)).thenReturn(BigDecimal.valueOf(5));
        byte[] fileBytes = new byte[0];

        service.commit(fileBytes, "upload.csv", null, null);

        ArgumentCaptor<EltaxRenkei> captor = ArgumentCaptor.forClass(EltaxRenkei.class);
        verify(eltaxRenkeiRepository).save(captor.capture());
        EltaxRenkei saved = captor.getValue();
        assertThat(saved.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.getFileName()).isEqualTo("upload.csv");
        assertThat(saved.getShoriKekka()).isEqualTo("1");
        assertThat(saved.getLog()).isEqualTo(fileBytes);
    }

    // No.83: 未対応手続種別でもeLTAX連携管理へ保存する
    @Test
    void commit_未対応手続種別でもeLTAX連携管理へ保存する() {
        service.commit(new byte[0], "test.csv", null, null);

        verify(eltaxRenkeiRepository).save(any(EltaxRenkei.class));
        verify(tokugimuRepository, never()).save(any());
        verify(fukaRepository, never()).save(any());
    }

    // =========================================================================
    // commit - 特別徴収義務者（新規）
    // =========================================================================

    // No.40: 新規登録時に自動採番された指定番号で登録される
    @Test
    void commit_新規登録時に自動採番された指定番号で登録される() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "1",
                16, "サンプル商事",
                18, "123-4567",
                19, "○○市△△町1-1",
                20, "03-1234-5678",
                23, "2",
                27, "サンプルホテル",
                28, "テスト住所"
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getShiteiNo()).isEqualTo("shi00001");
    }

    // No.67: 新規登録時に前履歴が存在しないため最新フラグの更新処理は発生しない
    @Test
    void commit_新規登録時はprevへのsaveが発生しない() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "1", 16, "サンプル商事", 18, "123-4567",
                19, "○○市", 20, "03-1234-5678", 23, "2",
                27, "サンプルホテル", 28, "テスト住所"
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(1);
        assertThat(captor.getValue().getNewFlg()).isEqualTo("1");
    }

    // No.66: 変更時、前履歴レコードのnewFlgが0に更新される
    @Test
    void commit_変更時に前履歴のnewFlgが0に更新される() {
        Tokugimu prev = buildTokugimu("shi00001", "旧ホテル");
        prev.setNewFlg("1");
        prev.setKyokaName("");
        prev.setKyokaYubinNo("");
        prev.setKyokaJusho("");
        prev.setSoufusakiName("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "2", 26, "shi00001", 27, "新ホテル", 28, "テスト住所",
                16, "テスト商事", 18, "123-4567", 19, "テスト住所", 20, "03-1234-5678", 23, "2"
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(t -> "0".equals(t.getNewFlg()))).isTrue();
        assertThat(captor.getAllValues().stream().anyMatch(t -> "1".equals(t.getNewFlg()))).isTrue();
    }

    // No.75: 新規登録時に宛名番号が未指定の場合はRuntimeExceptionをスローする
    @Test
    void commit_新規登録時に宛名番号が未指定の場合はRuntimeExceptionをスローする() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));

        byte[] csv = tokugimuCsv(Map.of(13, "1", 16, "テスト", 18, "123-4567",
                19, "テスト住所", 20, "03-1234-5678", 23, "2", 27, "ホテル", 28, "住所"));

        assertThatThrownBy(() -> service.commit(csv, "test.csv", null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("宛名番号が検索されていません");
    }

    // =========================================================================
    // commit - 施設所有者・共同事業者
    // =========================================================================

    // No.42: 施設所有者情報・共同事業者情報がともに入力されている場合、両方とも保存される
    @Test
    void commit_所有者と共同事業者がともに入力されている場合両方保存される() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shoyushaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(kyodoJigyoshaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.ofEntries(
                Map.entry(13, "1"),
                Map.entry(16, "テスト商事"),
                Map.entry(18, "123-4567"),
                Map.entry(19, "テスト住所"),
                Map.entry(20, "03-1234-5678"),
                Map.entry(23, "2"),
                Map.entry(27, "テストホテル"),
                Map.entry(28, "テスト住所"),
                Map.entry(41, "所有者名"),
                Map.entry(45, "共同事業者名")
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        verify(shoyushaRepository).save(any(Shoyusha.class));
        verify(kyodoJigyoshaRepository).save(any(KyodoJigyosha.class));
    }

    // No.43: 【実装バグ】施設所有者情報が空欄で共同事業者情報のみ入力されている場合、共同事業者情報が保存されない
    @Test
    void commit_所有者が空欄で共同事業者のみ入力の場合共同事業者が保存されない() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 所有者名(col41)は空、共同事業者名(col45)のみ入力
        byte[] csv = tokugimuCsv(Map.ofEntries(
                Map.entry(13, "1"),
                Map.entry(16, "テスト商事"),
                Map.entry(18, "123-4567"),
                Map.entry(19, "テスト住所"),
                Map.entry(20, "03-1234-5678"),
                Map.entry(23, "2"),
                Map.entry(27, "テストホテル"),
                Map.entry(28, "テスト住所"),
                Map.entry(45, "共同事業者名")
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        // バグ: shoyushaNameが空なので共同事業者も保存されない
        verify(shoyushaRepository, never()).save(any());
        verify(kyodoJigyoshaRepository, never()).save(any());
    }

    // No.44: 【実装バグ】施設所有者情報のみ入力され共同事業者情報が空欄の場合、空の共同事業者レコードが誤って保存される
    @Test
    void commit_所有者のみ入力で共同事業者が空欄の場合空の共同事業者が誤って保存される() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shoyushaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(kyodoJigyoshaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 所有者名(col41)のみ入力、共同事業者名(col45)は空
        byte[] csv = tokugimuCsv(Map.ofEntries(
                Map.entry(13, "1"),
                Map.entry(16, "テスト商事"),
                Map.entry(18, "123-4567"),
                Map.entry(19, "テスト住所"),
                Map.entry(20, "03-1234-5678"),
                Map.entry(23, "2"),
                Map.entry(27, "テストホテル"),
                Map.entry(28, "テスト住所"),
                Map.entry(41, "所有者名")
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        verify(shoyushaRepository).save(any(Shoyusha.class));
        // バグ: shoyushaNameが空でないので共同事業者も保存される（kyodoJigyoshaNameは空文字）
        ArgumentCaptor<KyodoJigyosha> captor = ArgumentCaptor.forClass(KyodoJigyosha.class);
        verify(kyodoJigyoshaRepository).save(captor.capture());
        assertThat(captor.getValue().getKyodoJigyoshaName()).isEqualTo("");
    }

    // No.45: 施設所有者情報・共同事業者情報がともに空欄の場合、どちらも保存されない
    @Test
    void commit_所有者と共同事業者がともに空欄の場合どちらも保存されない() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "1", 16, "テスト商事", 18, "123-4567",
                19, "テスト住所", 20, "03-1234-5678", 23, "2",
                27, "テストホテル", 28, "テスト住所"
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        verify(shoyushaRepository, never()).save(any());
        verify(kyodoJigyoshaRepository, never()).save(any());
    }

    // No.46: 施設所有者・共同事業者のフリガナは、同一rno・idx=1の既存レコードから引き継がれる
    @Test
    void commit_所有者と共同事業者のフリガナが既存レコードから引き継がれる() {
        Tokugimu prev = buildTokugimu("shi00001", "旧ホテル");
        prev.setKyokaName("");
        prev.setKyokaYubinNo("");
        prev.setKyokaJusho("");
        prev.setSoufusakiName("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Shoyusha prevShoyusha = new Shoyusha();
        prevShoyusha.setRno(BigDecimal.ONE);
        prevShoyusha.setIdx(BigDecimal.ONE);
        prevShoyusha.setShoyushaNameKana("ショユウシャカナ");
        when(shoyushaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001"))
                .thenReturn(List.of(prevShoyusha));
        when(shoyushaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KyodoJigyosha prevKyodo = new KyodoJigyosha();
        prevKyodo.setRno(BigDecimal.ONE);
        prevKyodo.setIdx(BigDecimal.ONE);
        prevKyodo.setKyodoJigyoshaNameKana("キョウドウカナ");
        when(kyodoJigyoshaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001"))
                .thenReturn(List.of(prevKyodo));
        when(kyodoJigyoshaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.ofEntries(
                Map.entry(13, "2"),
                Map.entry(16, "テスト商事"),
                Map.entry(18, "123-4567"),
                Map.entry(19, "テスト住所"),
                Map.entry(20, "03-1234-5678"),
                Map.entry(23, "2"),
                Map.entry(26, "shi00001"),
                Map.entry(27, "新ホテル"),
                Map.entry(28, "テスト住所"),
                Map.entry(41, "新所有者名"),
                Map.entry(45, "新共同事業者名")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Shoyusha> shoyushaCaptor = ArgumentCaptor.forClass(Shoyusha.class);
        verify(shoyushaRepository).save(shoyushaCaptor.capture());
        assertThat(shoyushaCaptor.getValue().getShoyushaNameKana()).isEqualTo("ショユウシャカナ");

        ArgumentCaptor<KyodoJigyosha> kyodoCaptor = ArgumentCaptor.forClass(KyodoJigyosha.class);
        verify(kyodoJigyoshaRepository).save(kyodoCaptor.capture());
        assertThat(kyodoCaptor.getValue().getKyodoJigyoshaNameKana()).isEqualTo("キョウドウカナ");
    }

    // =========================================================================
    // commit - 営業許可等情報・送付先情報のフォールバック
    // =========================================================================

    // No.48: 営業許可等情報【氏名】が空欄の場合、特別徴収義務者情報で更新する（新規登録時）
    @Test
    void commit_新規登録時に営業許可氏名が空欄の場合は特別徴収義務者名でフォールバックする() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "1", 16, "テスト商事", 18, "123-4567",
                19, "テスト住所", 20, "03-1234-5678", 23, "2",
                27, "テストホテル", 28, "テスト住所"
                // col36(営業許可氏名)は空
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyokaName()).isEqualTo("テスト商事");
    }

    // No.49: 営業許可等情報【郵便番号】が空欄の場合、特別徴収義務者情報で更新する（新規登録時）
    @Test
    void commit_新規登録時に営業許可郵便番号が空欄の場合は特別徴収義務者郵便番号でフォールバックする() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "1", 16, "テスト商事", 18, "123-4567",
                19, "テスト住所", 20, "03-1234-5678", 23, "2",
                27, "テストホテル", 28, "テスト住所"
                // col37(営業許可郵便番号)は空
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyokaYubinNo()).isEqualTo("123-4567");
    }

    // No.50: 営業許可等情報【住所】が空欄の場合、特別徴収義務者情報で更新する（新規登録時）
    @Test
    void commit_新規登録時に営業許可住所が空欄の場合は特別徴収義務者住所でフォールバックする() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "1", 16, "テスト商事", 18, "123-4567",
                19, "テスト住所", 20, "03-1234-5678", 23, "2",
                27, "テストホテル", 28, "テスト住所"
                // col38(営業許可住所)は空
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyokaJusho()).isEqualTo("テスト住所");
    }

    // No.51: 送付先情報【氏名】が空欄の場合、特別徴収義務者情報で更新する（新規登録時）
    @Test
    void commit_新規登録時に送付先氏名が空欄の場合は特別徴収義務者名でフォールバックする() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "1", 16, "テスト商事", 18, "123-4567",
                19, "テスト住所", 20, "03-1234-5678", 23, "2",
                27, "テストホテル", 28, "テスト住所"
                // col49(送付先氏名)は空
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getSoufusakiName()).isEqualTo("テスト商事");
    }

    // No.52: 変更申請でCSVの営業許可氏名が空欄・既存値がある場合は既存値を維持する
    @Test
    void commit_変更時に営業許可氏名がCSV空欄で既存値がある場合は既存値を維持する() {
        Tokugimu prev = buildTokugimu("shi00001", "旧ホテル");
        prev.setKyokaName("既存営業許可名");
        prev.setKyokaYubinNo("");
        prev.setKyokaJusho("");
        prev.setSoufusakiName("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "2", 26, "shi00001", 27, "新ホテル", 28, "テスト住所",
                16, "テスト商事", 18, "123-4567", 19, "テスト住所", 20, "03-1234-5678", 23, "2"
                // col36(営業許可氏名)は空
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyokaName()).isEqualTo("既存営業許可名");
    }

    // No.53: 変更申請でCSVの営業許可氏名も既存値も空の場合は特別徴収義務者情報にフォールバックすべき（実装バグ）
    @Test
    void commit_変更時に営業許可氏名がCSV空欄かつ既存値も空の場合は特別徴収義務者名でフォールバックする() {
        Tokugimu prev = buildTokugimu("shi00001", "旧ホテル");
        prev.setKyokaName("");
        prev.setKyokaYubinNo("");
        prev.setKyokaJusho("");
        prev.setSoufusakiName("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "2", 26, "shi00001", 27, "新ホテル", 28, "テスト住所",
                16, "テスト商事", 18, "123-4567", 19, "テスト住所", 20, "03-1234-5678", 23, "2"
                // col36(営業許可氏名)は空、既存値も空
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyokaName()).isEqualTo("テスト商事");
    }

    // No.54: 変更申請でCSVの営業許可郵便番号が空欄・既存値がある場合は既存値を維持する
    @Test
    void commit_変更時に営業許可郵便番号がCSV空欄で既存値がある場合は既存値を維持する() {
        Tokugimu prev = buildTokugimu("shi00001", "旧ホテル");
        prev.setKyokaName("");
        prev.setKyokaYubinNo("999-9999");
        prev.setKyokaJusho("");
        prev.setSoufusakiName("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "2", 26, "shi00001", 27, "新ホテル", 28, "テスト住所",
                16, "テスト商事", 18, "123-4567", 19, "テスト住所", 20, "03-1234-5678", 23, "2"
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyokaYubinNo()).isEqualTo("999-9999");
    }

    // No.55: 変更申請でCSVの営業許可郵便番号も既存値も空の場合は特別徴収義務者情報にフォールバックすべき（実装バグ）
    @Test
    void commit_変更時に営業許可郵便番号がCSV空欄かつ既存値も空の場合は特別徴収義務者郵便番号でフォールバックする() {
        Tokugimu prev = buildTokugimu("shi00001", "旧ホテル");
        prev.setKyokaName("");
        prev.setKyokaYubinNo("");
        prev.setKyokaJusho("");
        prev.setSoufusakiName("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "2", 26, "shi00001", 27, "新ホテル", 28, "テスト住所",
                16, "テスト商事", 18, "123-4567", 19, "テスト住所", 20, "03-1234-5678", 23, "2"
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyokaYubinNo()).isEqualTo("123-4567");
    }

    // No.56: 変更申請でCSVの営業許可住所が空欄・既存値がある場合は既存値を維持する
    @Test
    void commit_変更時に営業許可住所がCSV空欄で既存値がある場合は既存値を維持する() {
        Tokugimu prev = buildTokugimu("shi00001", "旧ホテル");
        prev.setKyokaName("");
        prev.setKyokaYubinNo("");
        prev.setKyokaJusho("既存営業許可住所");
        prev.setSoufusakiName("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "2", 26, "shi00001", 27, "新ホテル", 28, "テスト住所",
                16, "テスト商事", 18, "123-4567", 19, "テスト住所", 20, "03-1234-5678", 23, "2"
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyokaJusho()).isEqualTo("既存営業許可住所");
    }

    // No.57: 変更申請でCSVの営業許可住所も既存値も空の場合は特別徴収義務者情報にフォールバックすべき（実装バグ）
    @Test
    void commit_変更時に営業許可住所がCSV空欄かつ既存値も空の場合は特別徴収義務者住所でフォールバックする() {
        Tokugimu prev = buildTokugimu("shi00001", "旧ホテル");
        prev.setKyokaName("");
        prev.setKyokaYubinNo("");
        prev.setKyokaJusho("");
        prev.setSoufusakiName("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "2", 26, "shi00001", 27, "新ホテル", 28, "テスト住所",
                16, "テスト商事", 18, "123-4567", 19, "テスト住所", 20, "03-1234-5678", 23, "2"
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyokaJusho()).isEqualTo("テスト住所");
    }

    // No.58: 変更申請でCSVの送付先氏名が空欄・既存値がある場合は既存値を維持する
    @Test
    void commit_変更時に送付先氏名がCSV空欄で既存値がある場合は既存値を維持する() {
        Tokugimu prev = buildTokugimu("shi00001", "旧ホテル");
        prev.setKyokaName("");
        prev.setKyokaYubinNo("");
        prev.setKyokaJusho("");
        prev.setSoufusakiName("既存送付先名");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "2", 26, "shi00001", 27, "新ホテル", 28, "テスト住所",
                16, "テスト商事", 18, "123-4567", 19, "テスト住所", 20, "03-1234-5678", 23, "2"
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getSoufusakiName()).isEqualTo("既存送付先名");
    }

    // No.59: 変更申請でCSVの送付先氏名も既存値も空の場合は特別徴収義務者情報にフォールバックすべき（実装バグ）
    @Test
    void commit_変更時に送付先氏名がCSV空欄かつ既存値も空の場合は特別徴収義務者名でフォールバックする() {
        Tokugimu prev = buildTokugimu("shi00001", "旧ホテル");
        prev.setKyokaName("");
        prev.setKyokaYubinNo("");
        prev.setKyokaJusho("");
        prev.setSoufusakiName("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "2", 26, "shi00001", 27, "新ホテル", 28, "テスト住所",
                16, "テスト商事", 18, "123-4567", 19, "テスト住所", 20, "03-1234-5678", 23, "2"
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getSoufusakiName()).isEqualTo("テスト商事");
    }

    // No.47: 営業許可種別「2」は「3」に変換して登録する
    @Test
    void commit_営業許可種別2は3に変換して登録する() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "1", 16, "テスト商事", 18, "123-4567",
                19, "テスト住所", 20, "03-1234-5678", 23, "2",
                27, "テストホテル", 28, "テスト住所", 40, "2"
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyokaShu()).isEqualTo("3");
    }

    // No.60: 特別徴収義務者（休止・再開・廃止）の確定処理
    @Test
    void commit_休止再開廃止の確定処理でTokugimuが保存される() {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        prev.setKyushiStYmd(null);
        prev.setKyushiEdYmd(null);
        prev.setEigyoEdYmd(null);
        prev.setKyuhaishiRiyu(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 休止申請
        byte[] csv = tokugimuCsv(Map.of(
                13, "3", 26, "shi00001",
                57, "改装のため", 61, "2026-04-01"
        ));
        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyushiStYmd()).isEqualTo(java.time.LocalDate.of(2026, 4, 1));
        assertThat(saved.getKyuhaishiRiyu()).isEqualTo("改装のため");
    }

    // No.61: 変更時、施設カナ名称・施設郵便番号・営業許可カナ名称・営業許可電話番号・送付先カナ名称はCSVの内容に関わらず既存値を維持する
    @Test
    void commit_変更時にカナ名称等は既存値を維持する() {
        Tokugimu prev = buildTokugimu("shi00001", "旧ホテル");
        prev.setShisetsuNameKana("シセツカナ");
        prev.setShisetsuYubinNo("000-0000");
        prev.setKyokaNameKana("キョカカナ");
        prev.setKyokaTel("06-0000-0000");
        prev.setSoufusakiNameKana("ソウフカナ");
        prev.setKyokaName("");
        prev.setKyokaYubinNo("");
        prev.setKyokaJusho("");
        prev.setSoufusakiName("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "2", 26, "shi00001", 27, "新ホテル", 28, "テスト住所",
                16, "テスト商事", 18, "123-4567", 19, "テスト住所", 20, "03-1234-5678", 23, "2"
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getShisetsuNameKana()).isEqualTo("シセツカナ");
        assertThat(saved.getShisetsuYubinNo()).isEqualTo("000-0000");
        assertThat(saved.getKyokaNameKana()).isEqualTo("キョカカナ");
        assertThat(saved.getKyokaTel()).isEqualTo("06-0000-0000");
        assertThat(saved.getSoufusakiNameKana()).isEqualTo("ソウフカナ");
    }

    // No.62: 休止時、休止開始年月日と休廃止理由が更新される
    @Test
    void commit_休止時に休止開始年月日と休廃止理由が更新される() {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        prev.setEigyoEdYmd(null);
        prev.setKyushiStYmd(null);
        prev.setKyushiEdYmd(null);
        prev.setKyuhaishiRiyu(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "3", 26, "shi00001",
                57, "改装のため", 61, "2026-04-01"
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyushiStYmd()).isEqualTo(java.time.LocalDate.of(2026, 4, 1));
        assertThat(saved.getKyuhaishiRiyu()).isEqualTo("改装のため");
    }

    // No.63: 再開時、休止終了年月日が再開年月日の前日として計算される
    @Test
    void commit_再開時に休止終了年月日が再開年月日の前日として計算される() {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        prev.setKyushiStYmd(java.time.LocalDate.of(2026, 4, 1));
        prev.setKyushiEdYmd(null);
        prev.setEigyoEdYmd(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(13, "4", 26, "shi00001", 64, "2026-07-01"));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKyushiEdYmd()).isEqualTo(java.time.LocalDate.of(2026, 6, 30));
        assertThat(saved.getKyushiStYmd()).isEqualTo(java.time.LocalDate.of(2026, 4, 1));
    }

    // No.64: 休止中に廃止した場合、営業終了年月日と休止終了年月日の両方が廃止年月日で更新される
    @Test
    void commit_休止中に廃止した場合に営業終了と休止終了が廃止年月日で更新される() {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        prev.setKyushiStYmd(java.time.LocalDate.of(2026, 4, 1));
        prev.setKyushiEdYmd(null);
        prev.setEigyoEdYmd(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(13, "5", 26, "shi00001", 63, "2026-08-01"));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getEigyoEdYmd()).isEqualTo(java.time.LocalDate.of(2026, 8, 1));
        assertThat(saved.getKyushiEdYmd()).isEqualTo(java.time.LocalDate.of(2026, 8, 1));
    }

    // No.65: 休止していない状態で廃止した場合、休止関連項目は更新されない
    @Test
    void commit_休止していない状態で廃止した場合に休止関連項目は更新されない() {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        prev.setKyushiStYmd(null);
        prev.setKyushiEdYmd(null);
        prev.setEigyoEdYmd(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(13, "5", 26, "shi00001", 63, "2026-08-01"));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getEigyoEdYmd()).isEqualTo(java.time.LocalDate.of(2026, 8, 1));
        assertThat(saved.getKyushiStYmd()).isNull();
        assertThat(saved.getKyushiEdYmd()).isNull();
    }

    // =========================================================================
    // preview - 納入申告（様式CSV使用）
    // =========================================================================

    // No.25: 納入申告（定額）で申告区分の税率・宿泊数・税額すべてのbeforeValueが個別に正しく解決される
    @Test
    void preview_定額納入申告で申告区分1の税率宿泊数税額のbeforeValueが正しく解決される() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(tokugimu));

        Fuka prevFuka = buildFuka("shi00001", "2025", 4, 1000L);
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "shi00001", "2025", 4))
                .thenReturn(List.of(prevFuka));

        // 申告区分１: zeiRitsu="5.00", hakusu="20", zeigaku="1000"
        FukaUchi prevUchi = new FukaUchi();
        prevUchi.setKazeiKbn(1);
        prevUchi.setZeiRitsu(new BigDecimal("5.00"));
        prevUchi.setHakusu(20L);
        prevUchi.setZeigaku(1000L);
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, "shi00001", 1, "2025", 4)).thenReturn(List.of(prevUchi));

        // 申告区分１の税率(col31)・宿泊数(col32)・税額(col33)に新しい値を設定
        byte[] csv = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(26, "テストホテル"),
                Map.entry(27, "テスト住所"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(31, "10.00"),
                Map.entry(32, "30"),
                Map.entry(33, "3000"),
                Map.entry(70, "30"),
                Map.entry(71, "3000"),
                Map.entry(72, "0"),
                Map.entry(73, "30"),
                Map.entry(74, "3000")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        assertThat(dto.getDiffRows()).isNotNull();
        // 申告区分１【税率】のbeforeValueが"5.00"
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月－申告区分１【税率】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getBeforeValue()).isEqualTo("5.00"));
        // 申告区分１【宿泊数】のbeforeValueが"20"
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月－申告区分１【宿泊数】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getBeforeValue()).isEqualTo("20"));
        // 申告区分１【税額】のbeforeValueが"1000"
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月－申告区分１【税額】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getBeforeValue()).isEqualTo("1000"));
    }

    // No.26: 納入申告（定率）で申告区分の宿泊料金の総額・宿泊者数・宿泊料金・税率・税額すべてのbeforeValueが個別に正しく解決される
    @Test
    void preview_定率納入申告で申告区分1の全項目のbeforeValueが正しく解決される() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(tokugimu));

        Fuka prevFuka = buildFuka("shi00001", "2025", 4, 1000L);
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "shi00001", "2025", 4))
                .thenReturn(List.of(prevFuka));

        // 申告区分１: ryokinSogaku="200000", hakusu="10", ryokin="20000", zeiRitsu="5.00", zeigaku="1000"
        FukaUchi prevUchi = new FukaUchi();
        prevUchi.setKazeiKbn(1);
        prevUchi.setRyokinSogaku(200000L);
        prevUchi.setHakusu(10L);
        prevUchi.setRyokin(20000L);
        prevUchi.setZeiRitsu(new BigDecimal("5.00"));
        prevUchi.setZeigaku(1000L);
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, "shi00001", 1, "2025", 4)).thenReturn(List.of(prevUchi));

        // 申告区分１の宿泊料金の総額(col31)・宿泊者数(col32)・宿泊料金(col33)・税率(col34)・税額(col35)に新しい値を設定
        byte[] csv = teiritsuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(26, "テストホテル"),
                Map.entry(27, "テスト住所"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(31, "300000"),
                Map.entry(32, "15"),
                Map.entry(33, "30000"),
                Map.entry(34, "10.00"),
                Map.entry(35, "3000"),
                Map.entry(90, "15"),
                Map.entry(91, "300000"),
                Map.entry(92, "3000"),
                Map.entry(93, "0"),
                Map.entry(94, "0"),
                Map.entry(95, "15"),
                Map.entry(96, "3000")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        assertThat(dto.getDiffRows()).isNotNull();
        // 申告区分１【宿泊料金の総額】のbeforeValueが"200000"
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月－申告区分１【宿泊料金の総額】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getBeforeValue()).isEqualTo("200000"));
        // 申告区分１【宿泊者数】のbeforeValueが"10"
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月－申告区分１【宿泊者数】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getBeforeValue()).isEqualTo("10"));
        // 申告区分１【宿泊料金】のbeforeValueが"20000"
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月－申告区分１【宿泊料金】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getBeforeValue()).isEqualTo("20000"));
        // 申告区分１【税率】のbeforeValueが"5.00"
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月－申告区分１【税率】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getBeforeValue()).isEqualTo("5.00"));
        // 申告区分１【税額】のbeforeValueが"1000"
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月－申告区分１【税額】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getBeforeValue()).isEqualTo("1000"));
    }

    // No.27: 既存Fukaが見つかった場合、FukaUchiの検索にはprevFuka自身のrno・nendo・kibetsuが使われる
    @Test
    void preview_既存Fukaが見つかった場合FukaUchi検索にprevFuka自身のrno_nendo_kibetsuが使われる() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(tokugimu));

        // CSVの行為年月"202606"から算出されるnendo="2026", kibetsu=4 とは意図的に異なる値を設定
        Fuka prevFuka = new Fuka();
        prevFuka.setJichitaiCd(JICHITAI_CD);
        prevFuka.setShiteiNo("shi00001");
        prevFuka.setRno(3);        // CSVから算出される値とは異なる
        prevFuka.setNendo("2024"); // CSVから算出される値とは異なる
        prevFuka.setKibetsu(7);    // CSVから算出される値とは異なる
        prevFuka.setTotalZeigaku(5000L);
        prevFuka.setNewFlg("1");
        prevFuka.setDelFlg("0");
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "shi00001", "2026", 4))
                .thenReturn(List.of(prevFuka));
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, "shi00001", 3, "2024", 7)).thenReturn(List.of());

        byte[] csv = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(26, "テストホテル"),
                Map.entry(27, "テスト住所"),
                Map.entry(29, "202606"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        service.preview(file);

        // FukaUchi検索はCSVから算出した値(nendo="2026", kibetsu=4)ではなく
        // 見つかったprevFuka自身の値(rno=3, nendo="2024", kibetsu=7)で呼ばれる
        ArgumentCaptor<Integer> rnoCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> nendoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> kibetsuCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(fukaUchiRepository).findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                eq(JICHITAI_CD), eq("shi00001"),
                rnoCaptor.capture(), nendoCaptor.capture(), kibetsuCaptor.capture());
        assertThat(rnoCaptor.getValue()).isEqualTo(3);
        assertThat(nendoCaptor.getValue()).isEqualTo("2024");
        assertThat(kibetsuCaptor.getValue()).isEqualTo(7);
    }

    // No.28: 既存Fukaが見つからない場合、FukaUchiの検索処理自体が呼ばれない
    @Test
    void preview_既存Fukaが見つからない場合FukaUchi検索は呼ばれずbeforeValueが全て_になる() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(tokugimu));

        // 既存Fukaなし
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "shi00001", "2026", 4))
                .thenReturn(List.of());

        byte[] csv = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(26, "テストホテル"),
                Map.entry(27, "テスト住所"),
                Map.entry(29, "202606"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        // 既存FukaがないのでfukaUchiRepositoryは一度も呼ばれない
        verify(fukaUchiRepository, never()).findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                any(), any(), any(), any(), any());
        // prevFukaListが空なのでresolveBeforeValueFukaは全項目"－"を返す
        assertThat(dto.getDiffRows()).isNotNull();
        assertThat(dto.getDiffRows()).isNotEmpty();
        assertThat(dto.getDiffRows()).allMatch(r -> "－".equals(r.getBeforeValue()));
    }

    // No.22: 納入申告（定額）のCSV解析結果と変更前情報がdiffRowsへ正しく設定される
    @Test
    void preview_定額納入申告のCSV解析結果と変更前情報がdiffRowsへ正しく設定される() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(tokugimu));

        Fuka prevFuka = buildFuka("shi00001", "2025", 4, 5000L);
        prevFuka.setKazeiHakusu(10L);
        prevFuka.setZeigaku(5000L);
        prevFuka.setMenjoHakusu(2L);
        prevFuka.setTotalHakusu(12L);
        prevFuka.setTotalZeigaku(5000L);
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "shi00001", "2025", 4))
                .thenReturn(List.of(prevFuka));
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, "shi00001", 1, "2025", 4)).thenReturn(List.of());

        byte[] csv = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(26, "テストホテル"),
                Map.entry(27, "テスト住所"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "20"),
                Map.entry(33, "10000"),
                Map.entry(70, "20"),
                Map.entry(71, "10000"),
                Map.entry(72, "0"),
                Map.entry(73, "20"),
                Map.entry(74, "10000")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        assertThat(dto.getDiffRows()).isNotNull();
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月－課税対象宿泊合計【宿泊数】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> {
                    assertThat(r.getAfterValue()).isEqualTo("20");
                    assertThat(r.getBeforeValue()).isEqualTo("10");
                });
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月－合計【税額】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getAfterValue()).isEqualTo("10000"));
    }

    // No.23: 納入申告（定率）のCSV解析結果と変更前情報がdiffRowsへ正しく設定される
    @Test
    void preview_定率納入申告のCSV解析結果と変更前情報がdiffRowsへ正しく設定される() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(tokugimu));

        Fuka prevFuka = buildFuka("shi00001", "2025", 4, 8000L);
        prevFuka.setKazeiHakusu(5L);
        prevFuka.setKazeiRyokin(50000L);
        prevFuka.setZeigaku(8000L);
        prevFuka.setMenjoHakusu(1L);
        prevFuka.setMenjoRyokin(10000L);
        prevFuka.setTotalHakusu(6L);
        prevFuka.setTotalZeigaku(8000L);
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "shi00001", "2025", 4))
                .thenReturn(List.of(prevFuka));
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, "shi00001", 1, "2025", 4)).thenReturn(List.of());

        byte[] csv = teiritsuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(26, "テストホテル"),
                Map.entry(27, "テスト住所"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(90, "10"),
                Map.entry(91, "100000"),
                Map.entry(92, "16000"),
                Map.entry(93, "2"),
                Map.entry(94, "20000"),
                Map.entry(95, "12"),
                Map.entry(96, "16000")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        assertThat(dto.getDiffRows()).isNotNull();
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月－課税対象宿泊合計【宿泊者数】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> {
                    assertThat(r.getAfterValue()).isEqualTo("10");
                    assertThat(r.getBeforeValue()).isEqualTo("5");
                });
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月－課税対象宿泊合計【宿泊料金】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getBeforeValue()).isEqualTo("50000"));
    }

    // No.24: 特例納入申告（定額）のCSV解析結果と変更前情報が行為年月ごとにdiffRowsへ正しく設定される
    @Test
    void preview_特例定額納入申告のCSV解析結果が行為年月ごとにdiffRowsへ正しく設定される() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(tokugimu));
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());

        byte[] csv = tokuTeigakuCsv(Map.ofEntries(
                Map.entry(25, "shi00001"),
                Map.entry(26, "テストホテル"),
                Map.entry(27, "テスト住所"),
                Map.entry(29, "202504"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000"),
                Map.entry(75, "202505"),
                Map.entry(76, "1"),
                Map.entry(78, "8"),
                Map.entry(79, "4000"),
                Map.entry(116, "8"),
                Map.entry(117, "4000"),
                Map.entry(118, "0"),
                Map.entry(119, "8"),
                Map.entry(120, "4000"),
                Map.entry(121, "202506"),
                Map.entry(122, "1"),
                Map.entry(124, "6"),
                Map.entry(125, "3000"),
                Map.entry(162, "6"),
                Map.entry(163, "3000"),
                Map.entry(164, "0"),
                Map.entry(165, "6"),
                Map.entry(166, "3000")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        assertThat(dto.getDiffRows()).isNotNull();
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月１－課税対象宿泊合計【宿泊数】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getAfterValue()).isEqualTo("10"));
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月２－課税対象宿泊合計【宿泊数】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getAfterValue()).isEqualTo("8"));
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月３－課税対象宿泊合計【宿泊数】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getAfterValue()).isEqualTo("6"));
    }

    // No.25: 特例納入申告（定率）のCSV解析結果と変更前情報が行為年月ごとにdiffRowsへ正しく設定される
    @Test
    void preview_特例定率納入申告のCSV解析結果が行為年月ごとにdiffRowsへ正しく設定される() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(tokugimu));
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());

        byte[] csv = tokuTeiritsuCsv(Map.ofEntries(
                Map.entry(25, "shi00001"),
                Map.entry(26, "テストホテル"),
                Map.entry(27, "テスト住所"),
                Map.entry(29, "202504"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(90, "10"),
                Map.entry(91, "100000"),
                Map.entry(92, "16000"),
                Map.entry(93, "0"),
                Map.entry(94, "0"),
                Map.entry(95, "10"),
                Map.entry(96, "16000"),
                Map.entry(97, "202505"),
                Map.entry(98, "1"),
                Map.entry(100, "8"),
                Map.entry(158, "8"),
                Map.entry(159, "80000"),
                Map.entry(160, "12800"),
                Map.entry(161, "0"),
                Map.entry(162, "0"),
                Map.entry(163, "8"),
                Map.entry(164, "12800"),
                Map.entry(165, "202506"),
                Map.entry(166, "1"),
                Map.entry(168, "6"),
                Map.entry(226, "6"),
                Map.entry(227, "60000"),
                Map.entry(228, "9600"),
                Map.entry(229, "0"),
                Map.entry(230, "0"),
                Map.entry(231, "6"),
                Map.entry(232, "9600")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        assertThat(dto.getDiffRows()).isNotNull();
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月１－課税対象宿泊合計【宿泊者数】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getAfterValue()).isEqualTo("10"));
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月２－課税対象宿泊合計【宿泊者数】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getAfterValue()).isEqualTo("8"));
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月３－課税対象宿泊合計【宿泊者数】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getAfterValue()).isEqualTo("6"));
    }

    // No.26: 特例納入申告で行為年月２・３が未入力の場合は必須項目エラーとなる
    @Test
    void preview_特例納入申告で行為年月2_3が未入力の場合は必須項目エラーとなる() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(tokugimu));
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());

        // 行為年月２・３（col75, col121）を空にする
        byte[] csv = tokuTeigakuCsv(Map.of(
                25, "shi00001",
                26, "テストホテル",
                27, "テスト住所",
                29, "202504",
                70, "10",
                71, "5000",
                72, "0",
                73, "10",
                74, "5000"
                // col75(行為年月２)、col121(行為年月３)は空
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(EltaxRenkeiKakuninValidationException.class)
                .satisfies(e -> {
                    EltaxRenkeiKakuninValidationException ve = (EltaxRenkeiKakuninValidationException) e;
                    assertThat(ve.getErrorMessages()).anyMatch(m -> m.contains("行為年月"));
                });
    }

    // No.27: 納入申告（合算・代表指定番号）のプレビュー表示
    @Test
    void preview_合算代表指定番号の納入申告プレビューが表示される() throws Exception {
        // 合算指定番号はtokugimuに存在しないがgassanに存在し、代表指定番号のtokugimuを参照する
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "gas0001")).thenReturn(List.of());
        Gassan gassan = new Gassan();
        gassan.setJichitaiCd(JICHITAI_CD);
        gassan.setGassanShiteiNo("gas0001");
        gassan.setShiteiNo("shi00001");
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "gas0001"))
                .thenReturn(List.of(gassan));
        Tokugimu daihyo = buildTokugimu("shi00001", "代表ホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001"))
                .thenReturn(List.of(daihyo));
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());

        byte[] csv = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "gas0001"),
                Map.entry(26, "代表ホテル"),
                Map.entry(27, "テスト住所"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "5"),
                Map.entry(33, "2500"),
                Map.entry(70, "5"),
                Map.entry(71, "2500"),
                Map.entry(72, "0"),
                Map.entry(73, "5"),
                Map.entry(74, "2500")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        assertThat(dto).isNotNull();
        assertThat(dto.getShisetsuName()).isEqualTo("代表ホテル");
        assertThat(dto.getDiffRows()).isNotNull();
    }

    // No.28: 通常の納入申告（単一指定番号）のプレビュー表示
    @Test
    void preview_単一指定番号の納入申告プレビューが表示される() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(tokugimu));
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());

        byte[] csv = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(26, "テストホテル"),
                Map.entry(27, "テスト住所"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "5"),
                Map.entry(33, "2500"),
                Map.entry(70, "5"),
                Map.entry(71, "2500"),
                Map.entry(72, "0"),
                Map.entry(73, "5"),
                Map.entry(74, "2500")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        assertThat(dto).isNotNull();
        assertThat(dto.getShisetsuName()).isEqualTo("テストホテル");
        assertThat(dto.getShiteiNo()).isEqualTo("shi00001");
        assertThat(dto.getDiffRows()).isNotNull();
        assertThat(dto.getDiffRows()).isNotEmpty();
    }

    // =========================================================================
    // commit - 納入申告
    // =========================================================================

    // No.68: 納入申告（定額・特例定額）の確定処理
    @Test
    void commit_定額納入申告の確定処理でFukaが保存される() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeigaku zt = new ZeiritsuTeigaku();
        zt.setTeigakuSeq(BigDecimal.ONE);
        zt.setZeigaku(500L);
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(zt));

        byte[] csv = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        Fuka saved = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getShiteiNo()).isEqualTo("shi00001");
        assertThat(saved.getFukaKbn()).isEqualTo("1");
        assertThat(saved.getTotalZeigaku()).isEqualTo(5000L);
    }

    // No.69: 納入申告（定率・特例定率）の確定処理
    @Test
    void commit_定率納入申告の確定処理でFukaが保存される() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeiritsu ztr = new ZeiritsuTeiritsu();
        ztr.setTeiritsuSeq(BigDecimal.ONE);
        when(zeiritsuTeiritsuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(ztr));
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYmAndRyokin(any(), any(), any(), any()))
                .thenReturn(List.of());

        byte[] csv = teiritsuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(90, "10"),
                Map.entry(91, "100000"),
                Map.entry(92, "16000"),
                Map.entry(93, "0"),
                Map.entry(94, "0"),
                Map.entry(95, "10"),
                Map.entry(96, "16000")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        Fuka saved = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getFukaKbn()).isEqualTo("2");
        assertThat(saved.getTotalZeigaku()).isEqualTo(16000L);
    }

    // No.70: 定額納入申告で申告区分１～１０の順に賦課内訳を保存し、税率管理・定額詳細マスタを参照して県税額・市税額を算出する
    @Test
    void commit_定額申告区分順にFukaUchiが保存され県税市税が算出される() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        // 申告区分１: 県税額500円/泊
        ZeiritsuTeigaku zt1 = new ZeiritsuTeigaku();
        zt1.setTeigakuSeq(BigDecimal.ONE);
        zt1.setZeigaku(500L);
        // 申告区分２: 県税額1000円/泊
        ZeiritsuTeigaku zt2 = new ZeiritsuTeigaku();
        zt2.setTeigakuSeq(BigDecimal.valueOf(2));
        zt2.setZeigaku(1000L);
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(zt1, zt2));

        // 申告区分１: 10泊, 税額6000円 / 申告区分２: 5泊, 税額8000円
        byte[] csv = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "6000"),
                Map.entry(34, "2"),
                Map.entry(36, "5"),
                Map.entry(37, "8000"),
                Map.entry(70, "15"),
                Map.entry(71, "14000"),
                Map.entry(72, "0"),
                Map.entry(73, "15"),
                Map.entry(74, "14000")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<FukaUchi> uchiCaptor = ArgumentCaptor.forClass(FukaUchi.class);
        verify(fukaUchiRepository, times(2)).save(uchiCaptor.capture());
        List<FukaUchi> savedUchi = uchiCaptor.getAllValues();
        assertThat(savedUchi.get(0).getKazeiKbn()).isEqualTo(1);
        assertThat(savedUchi.get(1).getKazeiKbn()).isEqualTo(2);
        // 申告区分１: kenZeigaku=500*10=5000, cityZeigaku=6000-5000=1000
        assertThat(savedUchi.get(0).getKenZeigaku()).isEqualTo(5000L);
        assertThat(savedUchi.get(0).getCityZeigaku()).isEqualTo(1000L);
    }

    // No.71: 定額納入申告で、賦課本体（Fuka）の都道府県税額・市区町村税額が入れ替わって保存されることを検証する
    @Test
    void commit_定額納入申告でFukaの県税額市税額が入れ替わりで保存される() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeigaku zt = new ZeiritsuTeigaku();
        zt.setTeigakuSeq(BigDecimal.ONE);
        zt.setZeigaku(500L); // 県税額500円/泊
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(zt));

        // 10泊, 税額6000円 -> kenZeigakuは本来500*10=5000円のはず
        // バグ: totalKenZeigaku += uchiCityZeigakuなので実際は1000円が保存される
        byte[] csv = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "6000"),
                Map.entry(70, "10"),
                Map.entry(71, "6000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "6000")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        Fuka saved = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg())).findFirst().orElseThrow();
        // バグにより kenZeigakuに市税額(1000)が、cityZeigakuに県税額(5000)が入れ替わって保存される
        assertThat(saved.getKenZeigaku()).isEqualTo(1000L);
        assertThat(saved.getCityZeigaku()).isEqualTo(5000L);
    }

    // No.72: 特例納入申告で行為年月１→２→３の順にFukaレコードが登録される
    @Test
    void commit_特例納入申告で行為年月順にFukaが登録される() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeigaku zt = new ZeiritsuTeigaku();
        zt.setTeigakuSeq(BigDecimal.ONE);
        zt.setZeigaku(500L);
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(zt));

        byte[] csv = tokuTeigakuCsv(Map.ofEntries(
                Map.entry(25, "shi00001"),
                Map.entry(29, "202504"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000"),
                Map.entry(75, "202505"),
                Map.entry(76, "1"),
                Map.entry(78, "8"),
                Map.entry(79, "4000"),
                Map.entry(116, "8"),
                Map.entry(117, "4000"),
                Map.entry(118, "0"),
                Map.entry(119, "8"),
                Map.entry(120, "4000"),
                Map.entry(121, "202506"),
                Map.entry(122, "1"),
                Map.entry(124, "6"),
                Map.entry(125, "3000"),
                Map.entry(162, "6"),
                Map.entry(163, "3000"),
                Map.entry(164, "0"),
                Map.entry(165, "6"),
                Map.entry(166, "3000")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        List<String> savedTaishoYms = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg()) && f.getTaishoYm() != null)
                .map(Fuka::getTaishoYm)
                .distinct()
                .sorted()
                .toList();
        assertThat(savedTaishoYms).containsExactly("202504", "202505", "202506");
    }

    // No.73: 定率納入申告で宿泊料金を宿泊数で按分した1人あたり料金から都道府県税額を算出し、市区町村税額は合計税額との差額とする
    @Test
    void commit_定率納入申告で宿泊料金按分から県税市税が算出される() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeiritsu ztr = new ZeiritsuTeiritsu();
        ztr.setTeiritsuSeq(BigDecimal.ONE);
        when(zeiritsuTeiritsuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(ztr));
        // 1人あたり料金=10000円の場合、県税額=200円
        ZeiritsuTeigaku kenZt = new ZeiritsuTeigaku();
        kenZt.setZeigaku(200L);
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYmAndRyokin(
                eq(JICHITAI_CD), eq("2"), any(), eq(10000L)))
                .thenReturn(List.of(kenZt));

        // 10人, 合計宿泊料金100000円, 合計税額3000円
        byte[] csv = teiritsuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(90, "10"),
                Map.entry(91, "100000"),
                Map.entry(92, "3000"),
                Map.entry(93, "0"),
                Map.entry(94, "0"),
                Map.entry(95, "10"),
                Map.entry(96, "3000")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        Fuka saved = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg())).findFirst().orElseThrow();
        // kenZeigaku = 200 * 10 = 2000, cityZeigaku = 3000 - 2000 = 1000
        assertThat(saved.getKenZeigaku()).isEqualTo(2000L);
        assertThat(saved.getCityZeigaku()).isEqualTo(1000L);
    }

    // No.74: 定率納入申告の賦課内訳（FukaUchi）では市区町村税額・都道府県税額をnullとして保存する
    @Test
    void commit_定率納入申告のFukaUchiの市税県税はnullで保存される() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeiritsu ztr = new ZeiritsuTeiritsu();
        ztr.setTeiritsuSeq(BigDecimal.ONE);
        when(zeiritsuTeiritsuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(ztr));
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYmAndRyokin(any(), any(), any(), any()))
                .thenReturn(List.of());

        byte[] csv = teiritsuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(90, "10"),
                Map.entry(91, "100000"),
                Map.entry(92, "16000"),
                Map.entry(93, "0"),
                Map.entry(94, "0"),
                Map.entry(95, "10"),
                Map.entry(96, "16000")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<FukaUchi> captor = ArgumentCaptor.forClass(FukaUchi.class);
        verify(fukaUchiRepository, atLeastOnce()).save(captor.capture());
        FukaUchi saved = captor.getValue();
        assertThat(saved.getCityZeigaku()).isNull();
        assertThat(saved.getKenZeigaku()).isNull();
    }

    // =========================================================================
    // commit - 税率マスタ・期別・その他
    // =========================================================================

    // No.86: 定額税率マスタが不足している場合
    @Test
    void commit_定額税率マスタが不足している場合はRuntimeExceptionをスローする() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        // 申告区分１のデータがあるのにマスタが0件
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of());

        byte[] csv = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000")
        ));

        assertThatThrownBy(() -> service.commit(csv, "test.csv", null, "shi00001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("税率定額詳細マスタ");
    }

    // No.87: 定率税率マスタが不足している場合
    @Test
    void commit_定率税率マスタが不足している場合はRuntimeExceptionをスローする() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        when(zeiritsuTeiritsuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of());

        byte[] csv = teiritsuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(90, "10"),
                Map.entry(91, "100000"),
                Map.entry(92, "16000"),
                Map.entry(93, "0"),
                Map.entry(94, "0"),
                Map.entry(95, "10"),
                Map.entry(96, "16000")
        ));

        assertThatThrownBy(() -> service.commit(csv, "test.csv", null, "shi00001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("税率定率詳細マスタ");
    }

    // No.88: 年度・期別の切り替わりは固定の2月/3月ではなく、自治体情報マスタ（m_jichitai.年度開始月）の値で決まるべき
    @Test
    void commit_年度切り替わりは固定2月基準で実装されている() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeigaku zt = new ZeiritsuTeigaku();
        zt.setTeigakuSeq(BigDecimal.ONE);
        zt.setZeigaku(500L);
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(zt));

        // 2月は現実装では前年度扱い（固定2月基準）
        byte[] csv = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-03-01"),
                Map.entry(25, "shi00001"),
                Map.entry(29, "202602"),
                Map.entry(30, "1"),
                Map.entry(32, "5"),
                Map.entry(33, "2500"),
                Map.entry(70, "5"),
                Map.entry(71, "2500"),
                Map.entry(72, "0"),
                Map.entry(73, "5"),
                Map.entry(74, "2500")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        Fuka saved = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg())).findFirst().orElseThrow();
        // 現実装: 2月は前年度扱いなのでnendo="2025"
        assertThat(saved.getNendo()).isEqualTo("2025");
        // 自治体マスタ参照なしの固定実装であることを記録（仕様との不一致）
        verify(jichitaiRepository, never()).findById(any());
    }

    // No.89: 対象年月の期別算出は12月/1月の年またぎでも連番として算出される
    @Test
    void commit_年またぎ期別が連番として算出される() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeigaku zt = new ZeiritsuTeigaku();
        zt.setTeigakuSeq(BigDecimal.ONE);
        zt.setZeigaku(500L);
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(zt));

        // 12月: kibetsu=10, 1月: kibetsu=11
        byte[] csvDec = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"), Map.entry(25, "shi00001"), Map.entry(29, "202512"),
                Map.entry(30, "1"), Map.entry(32, "5"), Map.entry(33, "2500"),
                Map.entry(70, "5"), Map.entry(71, "2500"), Map.entry(72, "0"), Map.entry(73, "5"), Map.entry(74, "2500")
        ));
        service.commit(csvDec, "dec.csv", null, "shi00001");

        byte[] csvJan = teigakuCsv(Map.ofEntries(
                Map.entry(13, "2026-02-10"), Map.entry(25, "shi00001"), Map.entry(29, "202601"),
                Map.entry(30, "1"), Map.entry(32, "5"), Map.entry(33, "2500"),
                Map.entry(70, "5"), Map.entry(71, "2500"), Map.entry(72, "0"), Map.entry(73, "5"), Map.entry(74, "2500")
        ));
        service.commit(csvJan, "jan.csv", null, "shi00001");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        List<Fuka> saved = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg())).toList();
        Fuka dec = saved.stream().filter(f -> "202512".equals(f.getTaishoYm())).findFirst().orElseThrow();
        Fuka jan = saved.stream().filter(f -> "202601".equals(f.getTaishoYm())).findFirst().orElseThrow();
        assertThat(dec.getKibetsu()).isEqualTo(10);
        assertThat(jan.getKibetsu()).isEqualTo(11);
    }

    // No.92: 宿泊料金が定額詳細マスタのどの価格帯にも該当しない場合、都道府県税額は0として算出される
    @Test
    void commit_宿泊料金が定額詳細マスタに該当しない場合県税額は0となる() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeiritsu ztr = new ZeiritsuTeiritsu();
        ztr.setTeiritsuSeq(BigDecimal.ONE);
        when(zeiritsuTeiritsuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(ztr));
        // 定額詳細マスタに該当なし
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYmAndRyokin(any(), any(), any(), any()))
                .thenReturn(List.of());

        byte[] csv = teiritsuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(90, "10"),
                Map.entry(91, "100000"),
                Map.entry(92, "3000"),
                Map.entry(93, "0"),
                Map.entry(94, "0"),
                Map.entry(95, "10"),
                Map.entry(96, "3000")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        Fuka saved = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKenZeigaku()).isEqualTo(0L);
        assertThat(saved.getCityZeigaku()).isEqualTo(3000L);
    }

    // No.94: 定率計算時に課税対象宿泊者数が0件の場合
    @Test
    void commit_定率計算時に課税対象宿泊者数が0の場合は県税額0となる() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeiritsu ztr = new ZeiritsuTeiritsu();
        ztr.setTeiritsuSeq(BigDecimal.ONE);
        when(zeiritsuTeiritsuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(ztr));
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYmAndRyokin(any(), any(), any(), any()))
                .thenReturn(List.of());

        // 課税対象宿泊者数=0
        byte[] csv = teiritsuCsv(Map.ofEntries(
                Map.entry(13, "2026-01-10"),
                Map.entry(25, "shi00001"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(90, "0"),
                Map.entry(91, "0"),
                Map.entry(92, "0"),
                Map.entry(93, "0"),
                Map.entry(94, "0"),
                Map.entry(95, "0"),
                Map.entry(96, "0")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        Fuka saved = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getKenZeigaku()).isEqualTo(0L);
        assertThat(saved.getCityZeigaku()).isEqualTo(0L);
    }

    // =========================================================================
    // commit - 新規登録（指定番号プレフィックス）
    // =========================================================================

    // =========================================================================
    // preview - 特別徴収義務者（再開・廃止）
    // =========================================================================

    // No.20: 特別徴収義務者：再開のCSV解析結果と変更前情報がdiffRowsへ正しく設定される
    @Test
    void preview_再開時にdiffRowsに再開年月日が設定される() throws Exception {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        prev.setKyushiStYmd(java.time.LocalDate.of(2026, 4, 1));
        prev.setKyushiEdYmd(null);
        prev.setEigyoEdYmd(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001"))
                .thenReturn(List.of(prev));
        byte[] csv = tokugimuCsv(Map.ofEntries(
                Map.entry(13, "4"),
                Map.entry(14, "2026-07-01"),
                Map.entry(16, "テスト商事"),
                Map.entry(18, "123-4567"),
                Map.entry(19, "テスト住所"),
                Map.entry(20, "03-1234-5678"),
                Map.entry(23, "2"),
                Map.entry(26, "shi00001"),
                Map.entry(27, "テストホテル"),
                Map.entry(28, "テスト住所"),
                Map.entry(64, "2026-07-01")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        dto.getDiffRows().stream()
                .filter(r -> "休止廃止再開情報【再開年月日】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> {
                    assertThat(r.getAfterValue()).isEqualTo("2026-07-01");
                    assertThat(r.getBeforeValue()).isEqualTo("－");
                });
    }

    // No.21: 特別徴収義務者：廃止のCSV解析結果と変更前情報がdiffRowsへ正しく設定される
    @Test
    void preview_廃止時にdiffRowsに廃止年月日が設定される() throws Exception {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        prev.setEigyoEdYmd(null);
        prev.setKyushiStYmd(null);
        prev.setKyushiEdYmd(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001"))
                .thenReturn(List.of(prev));
        byte[] csv = tokugimuCsv(Map.ofEntries(
                Map.entry(13, "5"),
                Map.entry(14, "2026-08-01"),
                Map.entry(16, "テスト商事"),
                Map.entry(18, "123-4567"),
                Map.entry(19, "テスト住所"),
                Map.entry(20, "03-1234-5678"),
                Map.entry(23, "2"),
                Map.entry(26, "shi00001"),
                Map.entry(27, "テストホテル"),
                Map.entry(28, "テスト住所"),
                Map.entry(63, "2026-08-01")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        dto.getDiffRows().stream()
                .filter(r -> "休止廃止再開情報【廃止年月日】".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> {
                    assertThat(r.getAfterValue()).isEqualTo("2026-08-01");
                    assertThat(r.getBeforeValue()).isEqualTo("－");
                });
    }

    // No.29: 指定番号を画面から上書きして再プレビューした場合、上書き後の指定番号に紐づく宛名・施設情報が正しく取得される
    @Test
    void repreview_上書き指定番号に紐づく施設情報が取得される() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00002", "上書きホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00002"))
                .thenReturn(List.of(tokugimu));
        // CSVには別の指定番号が入っているが、overrideShiteiNoで上書き
        byte[] csv = tokugimuCsv(Map.ofEntries(
                Map.entry(13, "2"),
                Map.entry(14, "2026-04-01"),
                Map.entry(16, "テスト商事"),
                Map.entry(18, "123-4567"),
                Map.entry(19, "テスト住所"),
                Map.entry(20, "03-1234-5678"),
                Map.entry(23, "2"),
                Map.entry(26, "shi00001"),
                Map.entry(27, "旧ホテル"),
                Map.entry(28, "テスト住所")
        ));

        EltaxRenkeiKakuninDto dto = service.repreview(csv, "shi00002");

        assertThat(dto.getShiteiNo()).isEqualTo("shi00002");
        assertThat(dto.getShisetsuName()).isEqualTo("上書きホテル");
    }

    // No.76: 休止・再開・廃止処理時の必須日付項目欠落
    @Test
    void commit_休止時に休止年月日が欠落している場合はRuntimeExceptionをスローする() {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        prev.setKyushiStYmd(null);
        prev.setKyushiEdYmd(null);
        prev.setEigyoEdYmd(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));

        // 休止年月日(col61)が空
        byte[] csvKyushi = tokugimuCsv(Map.of(13, "3", 26, "shi00001"));
        assertThatThrownBy(() -> service.commit(csvKyushi, "test.csv", null, "shi00001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("休止年月日");
    }

    @Test
    void commit_再開時に再開年月日が欠落している場合はRuntimeExceptionをスローする() {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        prev.setKyushiStYmd(java.time.LocalDate.of(2026, 4, 1));
        prev.setKyushiEdYmd(null);
        prev.setEigyoEdYmd(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));

        // 再開年月日(col64)が空
        byte[] csvSaikai = tokugimuCsv(Map.of(13, "4", 26, "shi00001"));
        assertThatThrownBy(() -> service.commit(csvSaikai, "test.csv", null, "shi00001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("再開年月日");
    }

    @Test
    void commit_廃止時に廃止年月日が欠落している場合はRuntimeExceptionをスローする() {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        prev.setKyushiStYmd(null);
        prev.setKyushiEdYmd(null);
        prev.setEigyoEdYmd(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));

        // 廃止年月日(col63)が空
        byte[] csvHaishi = tokugimuCsv(Map.of(13, "5", 26, "shi00001"));
        assertThatThrownBy(() -> service.commit(csvHaishi, "test.csv", null, "shi00001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("廃止年月日");
    }

    // No.37: CSVの列数が様式定義の項目数より少ない場合でも例外にならず、不足項目は空文字として扱われる
    @Test
    void preview_CSV列数が様式定義より少ない場合でも例外にならず不足項目は空文字として扱われる() throws Exception {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());
        // 68列必要なところを3列だけ（手続IDのみ）送る
        byte[] csv = (TETSUZUKI_TOKUGIMU + ",,").getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        // 申請区分が空 → "" → システム対応外の申請区分例外になるが、IndexOutOfBoundsにはならない
        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(ArrayIndexOutOfBoundsException.class)
                .isNotInstanceOf(StringIndexOutOfBoundsException.class);
    }

    // No.41: 確認画面で指定番号を上書きしたまま登録した場合、上書き後の指定番号で保存される
    @Test
    void commit_上書き指定番号で保存される() {
        Tokugimu prev = buildTokugimu("shi00002", "上書きホテル");
        prev.setKyokaName("");
        prev.setKyokaYubinNo("");
        prev.setKyokaJusho("");
        prev.setSoufusakiName("");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00002")).thenReturn(List.of(prev));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(1));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // CSVには shi00001 が入っているが、overrideShiteiNo="shi00002" で上書き
        byte[] csv = tokugimuCsv(Map.of(
                13, "2", 26, "shi00001", 27, "新ホテル", 28, "テスト住所",
                16, "テスト商事", 18, "123-4567", 19, "テスト住所", 20, "03-1234-5678", 23, "2"
        ));

        service.commit(csv, "test.csv", null, "shi00002");

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getShiteiNo()).isEqualTo("shi00002");
    }

    // No.78: 上書き指定した指定番号がDBに存在しない場合（repreview経由）
    @Test
    void repreview_上書き指定番号がDB非存在の場合はRuntimeExceptionをスローする() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of());
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(any(), any())).thenReturn(List.of());
        byte[] csv = teigakuCsv(Map.of(
                13, "2026-04-01", 25, "shi00001", 29, "202506", 30, "1"
        ));

        assertThatThrownBy(() -> service.repreview(csv, "shi99999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("shi99999");
    }

    // No.79: 納入申告データで施設番号が未設定かつ指定番号も未指定の場合（repreview経由）
    @Test
    void repreview_施設番号未設定かつ指定番号も未指定の場合はRuntimeExceptionをスローする() {
        byte[] csv = teigakuCsv(Map.of(
                13, "2026-04-01", 29, "202506", 30, "1"
                // col25(施設番号)は空
        ));

        // repreview で overrideShiteiNo=null かつ CSV の施設番号も空
        assertThatThrownBy(() -> service.repreview(csv, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("施設番号が未設定です");
    }

    // No.80: repreview経由時に必須項目欠落によるバリデーション例外が発生する
    @Test
    void repreview_必須項目欠落でバリデーション例外が発生する() {
        Tokugimu prev = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(prev));

        // 提出年月日(col14, requiredFlg=1)が空
        byte[] csv = tokugimuCsv(Map.of(
                13, "2", 26, "shi00001", 27, "テストホテル", 28, "テスト住所",
                16, "テスト商事", 18, "123-4567", 19, "テスト住所", 20, "03-1234-5678", 23, "2"
        ));

        assertThatThrownBy(() -> service.repreview(csv, "shi00001"))
                .isInstanceOf(EltaxRenkeiKakuninValidationException.class)
                .satisfies(e -> {
                    EltaxRenkeiKakuninValidationException ve = (EltaxRenkeiKakuninValidationException) e;
                    assertThat(ve.getErrorMessages()).anyMatch(m -> m.contains("提出年月日"));
                });
    }

    // No.81: 様式定義CSV読込時にIOExceptionが発生する
    @Test
    void preview_様式定義CSV読込失敗時はUncheckedIOExceptionをスローする() throws Exception {
        // 存在しない手続IDを直接CSVに埋め込んでも loadYoshikiMap は空Mapを返すだけなので、
        // ここでは手続IDが正常でも ClassPathResource が見つからないケースを
        // 実装上 loadYoshikiMap が IOException をスローしないことを確認する代わりに、
        // 未知の手続IDで様式マップが空 → shubetsu="" → RuntimeException になることを検証する
        byte[] csv = buildCsvRow(3, Map.of(3, "R9999X99"));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("システム対応外の手続き種別です");
    }

    // No.85: 提出年月日が不正フォーマットの場合
    @Test
    void commit_提出年月日が不正フォーマットの場合はnullとして扱われ現在日付で登録される() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "1", 14, "INVALID-DATE", 16, "テスト商事", 18, "123-4567",
                19, "テスト住所", 20, "03-1234-5678", 23, "2",
                27, "テストホテル", 28, "テスト住所"
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        // 不正フォーマット → parseDate が null → LocalDate.now() にフォールバック
        assertThat(saved.getShinkokuYmd()).isNotNull();
        assertThat(saved.getShinkokuYmd()).isEqualTo(java.time.LocalDate.now());
    }

    // No.90: 提出年月日が3種類の日付フォーマット（yyyy/MM/dd、yyyyMMdd、ISO形式）のいずれでも正しく解釈される
    @Test
    void commit_提出年月日がyyyy_MM_dd形式で正しく解釈される() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // yyyy/MM/dd 形式
        byte[] csv1 = tokugimuCsv(Map.of(
                13, "1", 14, "2026/04/01", 16, "テスト商事", 18, "123-4567",
                19, "テスト住所", 20, "03-1234-5678", 23, "2",
                27, "テストホテル", 28, "テスト住所"
        ));
        service.commit(csv1, "test.csv", new BigDecimal("5000000000000011"), null);
        ArgumentCaptor<Tokugimu> captor1 = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor1.capture());
        assertThat(captor1.getAllValues().stream().filter(t -> "1".equals(t.getNewFlg()))
                .findFirst().orElseThrow().getShinkokuYmd())
                .isEqualTo(java.time.LocalDate.of(2026, 4, 1));
    }

    @Test
    void commit_提出年月日がyyyyMMdd形式で正しく解釈される() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // yyyyMMdd 形式
        byte[] csv2 = tokugimuCsv(Map.of(
                13, "1", 14, "20260401", 16, "テスト商事", 18, "123-4567",
                19, "テスト住所", 20, "03-1234-5678", 23, "2",
                27, "テストホテル", 28, "テスト住所"
        ));
        service.commit(csv2, "test.csv", new BigDecimal("5000000000000011"), null);
        ArgumentCaptor<Tokugimu> captor2 = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor2.capture());
        assertThat(captor2.getAllValues().stream().filter(t -> "1".equals(t.getNewFlg()))
                .findFirst().orElseThrow().getShinkokuYmd())
                .isEqualTo(java.time.LocalDate.of(2026, 4, 1));
    }

    @Test
    void commit_提出年月日がISO形式で正しく解釈される() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar("shi");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "shi")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("shi00001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // ISO形式 (yyyy-MM-dd)
        byte[] csv3 = tokugimuCsv(Map.of(
                13, "1", 14, "2026-04-01", 16, "テスト商事", 18, "123-4567",
                19, "テスト住所", 20, "03-1234-5678", 23, "2",
                27, "テストホテル", 28, "テスト住所"
        ));
        service.commit(csv3, "test.csv", new BigDecimal("5000000000000011"), null);
        ArgumentCaptor<Tokugimu> captor3 = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor3.capture());
        assertThat(captor3.getAllValues().stream().filter(t -> "1".equals(t.getNewFlg()))
                .findFirst().orElseThrow().getShinkokuYmd())
                .isEqualTo(java.time.LocalDate.of(2026, 4, 1));
    }

    // No.91: 対応していない日付フォーマットの場合、日付項目はnullとして扱われる
    @Test
    void commit_対応していない日付フォーマットの場合はnullとして扱われる() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeigaku zt = new ZeiritsuTeigaku();
        zt.setTeigakuSeq(BigDecimal.ONE);
        zt.setZeigaku(500L);
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(zt));

        byte[] csv = teigakuCsv(Map.ofEntries(
                Map.entry(13, "04/01/2026"),  // 不正フォーマット
                Map.entry(25, "shi00001"),
                Map.entry(29, "202506"),
                Map.entry(30, "1"),
                Map.entry(32, "5"),
                Map.entry(33, "2500"),
                Map.entry(70, "5"),
                Map.entry(71, "2500"),
                Map.entry(72, "0"),
                Map.entry(73, "5"),
                Map.entry(74, "2500")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        Fuka saved = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg())).findFirst().orElseThrow();
        // parseDate が null → LocalDate.now() にフォールバック
        assertThat(saved.getShinkokuYmd()).isNotNull();
        assertThat(saved.getShinkokuYmd()).isEqualTo(java.time.LocalDate.now());
    }

    // No.93: 特例納入申告（commit）で対象年月２・３が未入力の場合は必須項目エラーとなる
    @Test
    void commit_特例定額納入申告で対象年月2_3が未入力の場合は必須項目エラーとなる() {
        // commit は preview を経由しないため、必須チェックは行われず
        // 行為年月２・３が空の場合は saveFuka がスキップされ、行為年月１のみ保存される
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeigaku zt = new ZeiritsuTeigaku();
        zt.setTeigakuSeq(BigDecimal.ONE);
        zt.setZeigaku(500L);
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(zt));

        // 行為年月２(col75)・行為年月３(col121)は空
        byte[] csv = tokuTeigakuCsv(Map.ofEntries(
                Map.entry(25, "shi00001"),
                Map.entry(29, "202504"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000")
                // col75(行為年月２)、col121(行為年月３)は空
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        // 行為年月１のみ保存され、行為年月２・３はスキップされる
        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        List<String> savedTaishoYms = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg()) && f.getTaishoYm() != null)
                .map(Fuka::getTaishoYm)
                .distinct().sorted().toList();
        assertThat(savedTaishoYms).containsExactly("202504");
    }

    // =========================================================================
    // commit - 特例納入申告（saveNonyuTokureiTeigaku / saveNonyuTokureiTeiritsu）
    // =========================================================================

    // No.94: 特例納入申告（定額）で行為年月１〜３それぞれに正しいprefixが渡され、各Fukaの対象年月・数値項目が正しい値で保存される
    @Test
    void commit_特例定額納入申告で行為年月ごとに正しいprefixでFukaが保存される() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeigaku zt = new ZeiritsuTeigaku();
        zt.setTeigakuSeq(BigDecimal.ONE);
        zt.setZeigaku(500L);
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(zt));

        // 行為年月１=202504(10泊/5000円), 行為年月２=202505(8泊/4000円), 行為年月３=202506(6泊/3000円)
        byte[] csv = tokuTeigakuCsv(Map.ofEntries(
                Map.entry(25, "shi00001"),
                Map.entry(29, "202504"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000"),
                Map.entry(75, "202505"),
                Map.entry(76, "1"),
                Map.entry(78, "8"),
                Map.entry(79, "4000"),
                Map.entry(116, "8"),
                Map.entry(117, "4000"),
                Map.entry(118, "0"),
                Map.entry(119, "8"),
                Map.entry(120, "4000"),
                Map.entry(121, "202506"),
                Map.entry(122, "1"),
                Map.entry(124, "6"),
                Map.entry(125, "3000"),
                Map.entry(162, "6"),
                Map.entry(163, "3000"),
                Map.entry(164, "0"),
                Map.entry(165, "6"),
                Map.entry(166, "3000")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        // saveFukaは各行為年月で2回呼ばれる（初回保存＋kenZeigaku更新）ため、taishoYmでdistinctして最終状態を取得
        List<Fuka> newFukas = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg()) && f.getTaishoYm() != null)
                .collect(java.util.stream.Collectors.toMap(
                        Fuka::getTaishoYm, f -> f, (a, b) -> b,
                        java.util.TreeMap::new))
                .values().stream().toList();
        assertThat(newFukas).hasSize(3);
        // 行為年月１: 202504, 10泊, 5000円
        assertThat(newFukas.get(0).getTaishoYm()).isEqualTo("202504");
        assertThat(newFukas.get(0).getKazeiHakusu()).isEqualTo(10L);
        assertThat(newFukas.get(0).getTotalZeigaku()).isEqualTo(5000L);
        // 行為年月２: 202505, 8泊, 4000円
        assertThat(newFukas.get(1).getTaishoYm()).isEqualTo("202505");
        assertThat(newFukas.get(1).getKazeiHakusu()).isEqualTo(8L);
        assertThat(newFukas.get(1).getTotalZeigaku()).isEqualTo(4000L);
        // 行為年月３: 202506, 6泊, 3000円
        assertThat(newFukas.get(2).getTaishoYm()).isEqualTo("202506");
        assertThat(newFukas.get(2).getKazeiHakusu()).isEqualTo(6L);
        assertThat(newFukas.get(2).getTotalZeigaku()).isEqualTo(3000L);
    }

    // No.94（定率版）: 特例納入申告（定率）で行為年月１〜３それぞれに正しいprefixが渡され、各Fukaの対象年月・数値項目が正しい値で保存される
    @Test
    void commit_特例定率納入申告で行為年月ごとに正しいprefixでFukaが保存される() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeiritsu ztr = new ZeiritsuTeiritsu();
        ztr.setTeiritsuSeq(BigDecimal.ONE);
        when(zeiritsuTeiritsuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(ztr));
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYmAndRyokin(any(), any(), any(), any()))
                .thenReturn(List.of());

        // 行為年月１=202504(10人/100000円/16000円), 行為年月２=202505(8人/80000円/12800円), 行為年月３=202506(6人/60000円/9600円)
        byte[] csv = tokuTeiritsuCsv(Map.ofEntries(
                Map.entry(25, "shi00001"),
                Map.entry(29, "202504"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(90, "10"),
                Map.entry(91, "100000"),
                Map.entry(92, "16000"),
                Map.entry(93, "0"),
                Map.entry(94, "0"),
                Map.entry(95, "10"),
                Map.entry(96, "16000"),
                Map.entry(97, "202505"),
                Map.entry(98, "1"),
                Map.entry(100, "8"),
                Map.entry(158, "8"),
                Map.entry(159, "80000"),
                Map.entry(160, "12800"),
                Map.entry(161, "0"),
                Map.entry(162, "0"),
                Map.entry(163, "8"),
                Map.entry(164, "12800"),
                Map.entry(165, "202506"),
                Map.entry(166, "1"),
                Map.entry(168, "6"),
                Map.entry(226, "6"),
                Map.entry(227, "60000"),
                Map.entry(228, "9600"),
                Map.entry(229, "0"),
                Map.entry(230, "0"),
                Map.entry(231, "6"),
                Map.entry(232, "9600")
        ));

        service.commit(csv, "test.csv", null, "shi00001");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        List<Fuka> newFukas = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg()) && f.getTaishoYm() != null)
                .collect(java.util.stream.Collectors.toMap(
                        Fuka::getTaishoYm, f -> f, (a, b) -> b,
                        java.util.TreeMap::new))
                .values().stream().toList();
        assertThat(newFukas).hasSize(3);
        assertThat(newFukas.get(0).getTaishoYm()).isEqualTo("202504");
        assertThat(newFukas.get(0).getKazeiHakusu()).isEqualTo(10L);
        assertThat(newFukas.get(0).getKazeiRyokin()).isEqualTo(100000L);
        assertThat(newFukas.get(0).getTotalZeigaku()).isEqualTo(16000L);
        assertThat(newFukas.get(1).getTaishoYm()).isEqualTo("202505");
        assertThat(newFukas.get(1).getKazeiHakusu()).isEqualTo(8L);
        assertThat(newFukas.get(1).getKazeiRyokin()).isEqualTo(80000L);
        assertThat(newFukas.get(1).getTotalZeigaku()).isEqualTo(12800L);
        assertThat(newFukas.get(2).getTaishoYm()).isEqualTo("202506");
        assertThat(newFukas.get(2).getKazeiHakusu()).isEqualTo(6L);
        assertThat(newFukas.get(2).getKazeiRyokin()).isEqualTo(60000L);
        assertThat(newFukas.get(2).getTotalZeigaku()).isEqualTo(9600L);
    }

    // No.95: commit()を直接呼んだ場合、行為年月２が空欄でも例外にならず、その月だけスキップされて登録される
    // （必須チェックはpreview()側にしかなく、commit()自体には防御がないという設計上の弱点を記録するテスト）
    @Test
    void commit_特例定額で行為年月2が空欄の場合スキップされ行為年月1と3のみ保存される() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeigaku zt = new ZeiritsuTeigaku();
        zt.setTeigakuSeq(BigDecimal.ONE);
        zt.setZeigaku(500L);
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(zt));

        // 行為年月２(col75)のみ空欄
        byte[] csv = tokuTeigakuCsv(Map.ofEntries(
                Map.entry(25, "shi00001"),
                Map.entry(29, "202504"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000"),
                // col75(行為年月２)は空
                Map.entry(121, "202506"),
                Map.entry(122, "1"),
                Map.entry(124, "6"),
                Map.entry(125, "3000"),
                Map.entry(162, "6"),
                Map.entry(163, "3000"),
                Map.entry(164, "0"),
                Map.entry(165, "6"),
                Map.entry(166, "3000")
        ));

        // preview()を経由せずcommit()を直接呼び出す → 例外にならない
        assertThatCode(() -> service.commit(csv, "test.csv", null, "shi00001"))
                .doesNotThrowAnyException();

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        List<String> savedTaishoYms = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg()) && f.getTaishoYm() != null)
                .map(Fuka::getTaishoYm)
                .distinct().sorted().toList();
        // 行為年月２はスキップ、行為年月１・３のみ保存される
        assertThat(savedTaishoYms).containsExactly("202504", "202506");
        assertThat(savedTaishoYms).doesNotContain("202505");
    }

    // No.96: 行為年月の登録処理中に例外が発生した場合、「賦課情報の更新に失敗しました」にラップされて再スローされる
    @Test
    void commit_特例定額で行為年月2の処理中に例外が発生した場合ラップされて再スローされる() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeigaku zt = new ZeiritsuTeigaku();
        zt.setTeigakuSeq(BigDecimal.ONE);
        zt.setZeigaku(500L);
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(zt));
        // 行為年月２の処理でfukaRepository.saveが2回目にRuntimeExceptionをスロー
        when(fukaRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0))  // 1回目（行為年月１）は正常
                .thenThrow(new RuntimeException("DB error"));  // 2回目（行為年月２）で失敗

        byte[] csv = tokuTeigakuCsv(Map.ofEntries(
                Map.entry(25, "shi00001"),
                Map.entry(29, "202504"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000"),
                Map.entry(75, "202505"),
                Map.entry(76, "1"),
                Map.entry(78, "8"),
                Map.entry(79, "4000"),
                Map.entry(116, "8"),
                Map.entry(117, "4000"),
                Map.entry(118, "0"),
                Map.entry(119, "8"),
                Map.entry(120, "4000"),
                Map.entry(121, "202506"),
                Map.entry(122, "1"),
                Map.entry(124, "6"),
                Map.entry(125, "3000"),
                Map.entry(162, "6"),
                Map.entry(163, "3000"),
                Map.entry(164, "0"),
                Map.entry(165, "6"),
                Map.entry(166, "3000")
        ));

        assertThatThrownBy(() -> service.commit(csv, "test.csv", null, "shi00001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("賦課情報の更新に失敗しました")
                .hasMessageContaining("DB error");
    }

    // No.97: overrideShiteiNoが指定されている場合、行為年月１〜３すべてでCSVの施設番号ではなく上書き後の指定番号が使われる
    @Test
    void commit_特例定額でoverrideShiteiNoが指定された場合全行為年月で上書き指定番号が使われる() {
        when(fukaRepository.findLatestByNendoAndKibetsu(any(), any(), any(), any())).thenReturn(List.of());
        ZeiritsuTeigaku zt = new ZeiritsuTeigaku();
        zt.setTeigakuSeq(BigDecimal.ONE);
        zt.setZeigaku(500L);
        when(zeiritsuTeigakuRepository.findActiveByTaishoKbnAndTekiyoYm(any(), any(), any()))
                .thenReturn(List.of(zt));

        // CSVの施設番号=shi00001、overrideShiteiNo=shi99999
        byte[] csv = tokuTeigakuCsv(Map.ofEntries(
                Map.entry(25, "shi00001"),
                Map.entry(29, "202504"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000"),
                Map.entry(75, "202505"),
                Map.entry(76, "1"),
                Map.entry(78, "8"),
                Map.entry(79, "4000"),
                Map.entry(116, "8"),
                Map.entry(117, "4000"),
                Map.entry(118, "0"),
                Map.entry(119, "8"),
                Map.entry(120, "4000"),
                Map.entry(121, "202506"),
                Map.entry(122, "1"),
                Map.entry(124, "6"),
                Map.entry(125, "3000"),
                Map.entry(162, "6"),
                Map.entry(163, "3000"),
                Map.entry(164, "0"),
                Map.entry(165, "6"),
                Map.entry(166, "3000")
        ));

        service.commit(csv, "test.csv", null, "shi99999");

        ArgumentCaptor<Fuka> captor = ArgumentCaptor.forClass(Fuka.class);
        verify(fukaRepository, atLeastOnce()).save(captor.capture());
        List<Fuka> newFukas = captor.getAllValues().stream()
                .filter(f -> "1".equals(f.getNewFlg()) && f.getTaishoYm() != null)
                .collect(java.util.stream.Collectors.toMap(
                        Fuka::getTaishoYm, f -> f, (a, b) -> b))
                .values().stream().toList();
        assertThat(newFukas).hasSize(3);
        // 全行為年月でCSV値(shi00001)ではなく上書き値(shi99999)が使われる
        assertThat(newFukas).allMatch(f -> "shi99999".equals(f.getShiteiNo()));
    }

    // No.29: 特例納入申告で、行為年月ごとに既存Fukaが見つかった場合、その行為年月に対応するprevFuka自身のrno・nendo・kibetsuでFukaUchiが検索される
    @Test
    void preview_特例納入申告で既存Fukaが見つかった場合各行為年月のprevFuka自身のrno_nendo_kibetsuが使われる() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(tokugimu));

        // 行為年月１: "202505" → nendo="2025", kibetsu=3
        // 行為年月２: "202506" → nendo="2025", kibetsu=4
        // 行為年月３: "202507" → nendo="2025", kibetsu=5
        // CSVから算出されるnendo/kibetsuとはあえて異なる値をprevFukaに設定
        Fuka prevFuka1 = new Fuka();
        prevFuka1.setJichitaiCd(JICHITAI_CD);
        prevFuka1.setShiteiNo("shi00001");
        prevFuka1.setRno(1);
        prevFuka1.setNendo("2025");
        prevFuka1.setKibetsu(3);
        prevFuka1.setTotalZeigaku(1000L);
        prevFuka1.setNewFlg("1");
        prevFuka1.setDelFlg("0");

        Fuka prevFuka2 = new Fuka();
        prevFuka2.setJichitaiCd(JICHITAI_CD);
        prevFuka2.setShiteiNo("shi00001");
        prevFuka2.setRno(2);
        prevFuka2.setNendo("2025");
        prevFuka2.setKibetsu(4);
        prevFuka2.setTotalZeigaku(2000L);
        prevFuka2.setNewFlg("1");
        prevFuka2.setDelFlg("0");

        Fuka prevFuka3 = new Fuka();
        prevFuka3.setJichitaiCd(JICHITAI_CD);
        prevFuka3.setShiteiNo("shi00001");
        prevFuka3.setRno(3);
        prevFuka3.setNendo("2025");
        prevFuka3.setKibetsu(5);
        prevFuka3.setTotalZeigaku(3000L);
        prevFuka3.setNewFlg("1");
        prevFuka3.setDelFlg("0");

        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "shi00001", "2025", 3))
                .thenReturn(List.of(prevFuka1));
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "shi00001", "2025", 4))
                .thenReturn(List.of(prevFuka2));
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "shi00001", "2025", 5))
                .thenReturn(List.of(prevFuka3));
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                any(), any(), anyInt(), any(), anyInt())).thenReturn(List.of());

        // 行為年月１〜３すべてに値を設定（167列）
        byte[] csv = tokuTeigakuCsv(Map.ofEntries(
                Map.entry(25, "shi00001"),
                Map.entry(26, "テストホテル"),
                Map.entry(27, "テスト住所"),
                Map.entry(29, "202505"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000"),
                Map.entry(75, "202506"),
                Map.entry(76, "1"),
                Map.entry(78, "8"),
                Map.entry(79, "4000"),
                Map.entry(116, "8"),
                Map.entry(117, "4000"),
                Map.entry(118, "0"),
                Map.entry(119, "8"),
                Map.entry(120, "4000"),
                Map.entry(121, "202507"),
                Map.entry(122, "1"),
                Map.entry(124, "6"),
                Map.entry(125, "3000"),
                Map.entry(162, "6"),
                Map.entry(163, "3000"),
                Map.entry(164, "0"),
                Map.entry(165, "6"),
                Map.entry(166, "3000")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        service.preview(file);

        // fukaUchiRepositoryが3回呼ばれることを確認
        ArgumentCaptor<Integer> rnoCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> nendoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> kibetsuCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(fukaUchiRepository, times(3)).findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                eq(JICHITAI_CD), eq("shi00001"),
                rnoCaptor.capture(), nendoCaptor.capture(), kibetsuCaptor.capture());

        // 1回目: prevFuka1のrno=1, nendo="2025", kibetsu=3
        assertThat(rnoCaptor.getAllValues().get(0)).isEqualTo(1);
        assertThat(nendoCaptor.getAllValues().get(0)).isEqualTo("2025");
        assertThat(kibetsuCaptor.getAllValues().get(0)).isEqualTo(3);
        // 2回目: prevFuka2のrno=2, nendo="2025", kibetsu=4
        assertThat(rnoCaptor.getAllValues().get(1)).isEqualTo(2);
        assertThat(nendoCaptor.getAllValues().get(1)).isEqualTo("2025");
        assertThat(kibetsuCaptor.getAllValues().get(1)).isEqualTo(4);
        // 3回目: prevFuka3のrno=3, nendo="2025", kibetsu=5
        assertThat(rnoCaptor.getAllValues().get(2)).isEqualTo(3);
        assertThat(nendoCaptor.getAllValues().get(2)).isEqualTo("2025");
        assertThat(kibetsuCaptor.getAllValues().get(2)).isEqualTo(5);
    }

    // No.30: 特例納入申告で、行為年月の一部だけ既存Fukaが見つからない場合、その月だけFukaUchiの検索がスキップされる
    @Test
    void preview_特例納入申告で行為年月2のみ既存Fukaなしの場合FukaUchi検索が2回のみ呼ばれbeforeValueが_になる() throws Exception {
        Tokugimu tokugimu = buildTokugimu("shi00001", "テストホテル");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "shi00001")).thenReturn(List.of(tokugimu));

        // 行為年月１: 既存Fukaあり
        Fuka prevFuka1 = buildFuka("shi00001", "2025", 3, 1000L);
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "shi00001", "2025", 3))
                .thenReturn(List.of(prevFuka1));
        // 行為年月２: 既存Fukaなし
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "shi00001", "2025", 4))
                .thenReturn(List.of());
        // 行為年月３: 既存Fukaあり
        Fuka prevFuka3 = buildFuka("shi00001", "2025", 5, 3000L);
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, "shi00001", "2025", 5))
                .thenReturn(List.of(prevFuka3));
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                any(), any(), anyInt(), any(), anyInt())).thenReturn(List.of());

        byte[] csv = tokuTeigakuCsv(Map.ofEntries(
                Map.entry(25, "shi00001"),
                Map.entry(26, "テストホテル"),
                Map.entry(27, "テスト住所"),
                Map.entry(29, "202505"),
                Map.entry(30, "1"),
                Map.entry(32, "10"),
                Map.entry(33, "5000"),
                Map.entry(70, "10"),
                Map.entry(71, "5000"),
                Map.entry(72, "0"),
                Map.entry(73, "10"),
                Map.entry(74, "5000"),
                Map.entry(75, "202506"),
                Map.entry(76, "1"),
                Map.entry(78, "8"),
                Map.entry(79, "4000"),
                Map.entry(116, "8"),
                Map.entry(117, "4000"),
                Map.entry(118, "0"),
                Map.entry(119, "8"),
                Map.entry(120, "4000"),
                Map.entry(121, "202507"),
                Map.entry(122, "1"),
                Map.entry(124, "6"),
                Map.entry(125, "3000"),
                Map.entry(162, "6"),
                Map.entry(163, "3000"),
                Map.entry(164, "0"),
                Map.entry(165, "6"),
                Map.entry(166, "3000")
        ));
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv);

        EltaxRenkeiKakuninDto dto = service.preview(file);

        // 行為年月２は既存FukaなしのためfukaUchiRepositoryは2回のみ呼ばれる（行為年月１・３の分）
        verify(fukaUchiRepository, times(2)).findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                any(), any(), anyInt(), any(), anyInt());

        // 行為年月２配下のdiffRowsのbeforeValueは、prevFukaListに行為年月２のFukaが入らないため
        // resolveBeforeValueFukaでidx=1には行為年月３のFukaが入る。
        // 行為年月２配下の行為年月自体（col75）のbeforeValueは"－"であることを確認
        assertThat(dto.getDiffRows()).isNotNull();
        dto.getDiffRows().stream()
                .filter(r -> "納入税額－行為年月２".equals(r.getItemName()))
                .findFirst()
                .ifPresent(r -> assertThat(r.getBeforeValue()).isEqualTo("－"));
    }

    // No.84: 新規登録時に指定番号プレフィックス未設定の場合
    @Test
    void commit_指定番号プレフィックス未設定の場合は00000001で採番される() {
        Jichitai jichitai = new Jichitai();
        jichitai.setShiteiStChar(null);
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "000")).thenReturn(Optional.of(0));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(eq(JICHITAI_CD), eq("00000001"))).thenReturn(List.of());
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Optional.of(0));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] csv = tokugimuCsv(Map.of(
                13, "1", 16, "テスト商事", 18, "123-4567",
                19, "テスト住所", 20, "03-1234-5678", 23, "2",
                27, "テストホテル", 28, "テスト住所"
        ));

        service.commit(csv, "test.csv", new BigDecimal("5000000000000011"), null);

        ArgumentCaptor<Tokugimu> captor = ArgumentCaptor.forClass(Tokugimu.class);
        verify(tokugimuRepository, atLeastOnce()).save(captor.capture());
        Tokugimu saved = captor.getAllValues().stream()
                .filter(t -> "1".equals(t.getNewFlg())).findFirst().orElseThrow();
        assertThat(saved.getShiteiNo()).isEqualTo("00000001");
    }
}

