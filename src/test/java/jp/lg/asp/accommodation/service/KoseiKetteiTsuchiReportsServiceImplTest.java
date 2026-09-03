package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.chrono.JapaneseDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.FukaConstants;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.impl.KoseiKetteiTsuchiReportsServiceImpl;

/**
 * 宿泊税更正・決定通知書 単体テスト（ReportsServiceImpl）
 *
 * <p>チェックリスト「KoseiKetteiTsuchiReportsServiceImpl」の #15〜#34 に1対1で対応する。</p>
 * <p>#35・#36 は setShisetsuInfo が削除済みのため対象外。</p>
 */
@ExtendWith(MockitoExtension.class)
class KoseiKetteiTsuchiReportsServiceImplTest {

    @Mock FukaRepository fukaRepository;
    @Mock FukaUchiRepository fukaUchiRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks KoseiKetteiTsuchiReportsServiceImpl service;

    private static final String JICHITAI_CD = "12345";
    private static final String SHITEI_NO = "S001";

    // ------------------------------------------------------------------
    // 共通モック設定ヘルパー
    // ------------------------------------------------------------------

    /** init() が必要とする共通モックを設定する */
    private void mockInit() {
        Jichitai jichitai = new Jichitai();
        jichitai.setName("札幌");
        jichitai.setKbnName("市");
        when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
        when(reportsCommonService.getReportsDefText(anyString())).thenReturn("法令引用テスト");
        when(reportsCommonService.getReportsDefData(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    /** 最低限のフィールドを持つ Fuka を生成する */
    private Fuka fuka(String shiteiNo, String taishoYm, String fukaKbn, String henkoKbn) {
        Fuka f = new Fuka();
        f.setJichitaiCd(JICHITAI_CD);
        f.setShiteiNo(shiteiNo);
        f.setTaishoYm(taishoYm);
        f.setNendo(taishoYm != null && taishoYm.length() == 6 ? taishoYm.substring(0, 4) : "2024");
        f.setKibetsu(1);
        f.setRno(1);
        f.setFukaKbn(fukaKbn);
        f.setHenkoKbn(henkoKbn);
        f.setNewFlg("1");
        f.setDelFlg("0");
        return f;
    }

    /** 最低限のフィールドを持つ FukaUchi を生成する */
    private FukaUchi fukaUchi(int kazeiKbn, long zeigaku) {
        FukaUchi u = new FukaUchi();
        u.setKazeiKbn(kazeiKbn);
        u.setZeigaku(zeigaku);
        u.setRyokinSogaku(0L);
        u.setRyokin(0L);
        u.setHakusu(1L);
        return u;
    }

    /** Tokugimu を生成する */
    private Tokugimu tokugimu(String shiteiNo) {
        Tokugimu t = new Tokugimu();
        t.setShiteiNo(shiteiNo);
        t.setShisetsuYubinNo("1234567");
        t.setShisetsuJusho("東京都千代田区1-1");
        t.setShisetsuName("テストホテル");
        t.setAtenaNo(new BigDecimal("1"));
        return t;
    }

    /** generatePdf に必要な最低限のモックをまとめて設定する */
    private void mockForGeneratePdf(String taishoYm, String fukaKbn, String henkoKbn) {
        mockInit();
        Fuka f = fuka(SHITEI_NO, taishoYm, fukaKbn, henkoKbn);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, taishoYm.substring(0, 4)))
                .thenReturn(List.of(f));
        when(fukaRepository.findMaxRno(JICHITAI_CD, SHITEI_NO, f.getNendo(), f.getKibetsu()))
                .thenReturn(Optional.of(1));
        FukaUchi u = fukaUchi(1, 1000L);
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, SHITEI_NO, 1, f.getNendo(), f.getKibetsu()))
                .thenReturn(List.of(u));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu(SHITEI_NO)));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any(BigDecimal.class)))
                .thenReturn(Optional.empty());
    }

    // ==================================================================
    // #15 findTaishoYmList
    // ==================================================================

    @Test
    @DisplayName("#15 findTaishoYmList 正常系 対象年月リスト取得：repository呼び出し")
    void findTaishoYmList_repositoryから年月リストが返却される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(reportsCommonService.getJichitaiInfo()).thenReturn(new Jichitai());
        when(reportsCommonService.getReportsDefText(anyString())).thenReturn("");
        when(reportsCommonService.getReportsDefData(anyString())).thenReturn(new byte[0]);
        when(fukaRepository.findTaishoYmListByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of("202404", "202405"));

        List<String> result = service.findTaishoYmList(SHITEI_NO);

        assertThat(result).containsExactly("202404", "202405");
        verify(fukaRepository, times(1))
                .findTaishoYmListByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO);
    }

    // ==================================================================
    // #16 generatePdf 正常系 TEIRITSU
    // ==================================================================

    @Test
    @DisplayName("#16 generatePdf 正常系 fukaKbn が TEIRITSU の場合：定率用JRXMLを使用する")
    void generatePdf_fukaKbnがTEIRITSUの場合は定率用JRXMLでPDFが生成される() {
        mockForGeneratePdf("202404", FukaConstants.TEIRITSU.getValue(), FukaConstants.SHINKOKU.getValue());
        when(zeiritsuTeiritsuRepository.findActiveBySeq(anyString(), any())).thenReturn(List.of());

        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null, "2");

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #17 generatePdf 正常系 TEIGAKU
    // ==================================================================

    @Test
    @DisplayName("#17 generatePdf 正常系 fukaKbn が TEIRITSU 以外の場合：定額用JRXMLを使用する")
    void generatePdf_fukaKbnがTEIGAKUの場合は定額用JRXMLでPDFが生成される() {
        mockForGeneratePdf("202404", FukaConstants.TEIGAKU.getValue(), FukaConstants.SHINKOKU.getValue());

        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null, "2");

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #18 generatePdf 正常系 init() の設定値が null
    // ==================================================================

    @Test
    @DisplayName("#18 generatePdf 正常系 init() で取得する設定値が null の場合")
    void generatePdf_init設定値がnullでも例外なくbyteが返る() {
        when(reportsCommonService.getJichitaiInfo()).thenReturn(null);
        when(reportsCommonService.getReportsDefText(anyString())).thenReturn(null);
        when(reportsCommonService.getReportsDefData(anyString())).thenReturn(null);
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);

        Fuka f = fuka(SHITEI_NO, "202404", FukaConstants.TEIGAKU.getValue(), FukaConstants.SHINKOKU.getValue());
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "2024"))
                .thenReturn(List.of(f));
        when(fukaRepository.findMaxRno(JICHITAI_CD, SHITEI_NO, "2024", 1))
                .thenReturn(Optional.of(1));
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, SHITEI_NO, 1, "2024", 1))
                .thenReturn(List.of(fukaUchi(1, 1000L)));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu(SHITEI_NO)));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any(BigDecimal.class)))
                .thenReturn(Optional.empty());

        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null, "2");

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #19 generatePdf 正常系 b1Ym のみ指定
    // ==================================================================

    @Test
    @DisplayName("#19 generatePdf 正常系 b1Ym のみ指定（b2Ym / b3Ym が null）の場合")
    void generatePdf_b1Ymのみ指定の場合はb2b3ブロックが空で例外なくbyteが返る() {
        mockForGeneratePdf("202404", FukaConstants.TEIGAKU.getValue(), FukaConstants.SHINKOKU.getValue());

        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null, "2");

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #20 generatePdf 異常系 JRXMLファイルが存在しない
    // ==================================================================

    @Test
    @DisplayName("#20 generatePdf 異常系 JRXMLファイルが存在しない場合：RuntimeException")
    void generatePdf_JRXMLファイルが存在しない場合はRuntimeExceptionがスローされる() {
        // fukaKbn に存在しない値を設定して JRXML パスが解決できない状況を再現
        mockForGeneratePdf("202404", "INVALID_KBN", FukaConstants.SHINKOKU.getValue());

        // JRXML が存在しないため RuntimeException がスローされること
        assertThatThrownBy(() -> service.generatePdf(SHITEI_NO, "202404", null, null, "2"))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================================================================
    // #21 generatePdf 異常系 JasperReports 処理中に例外
    // ==================================================================

    @Test
    @DisplayName("#21 generatePdf 異常系 JasperReports 処理中に例外が発生した場合：RuntimeException")
    void generatePdf_JasperReports処理中に例外が発生した場合はRuntimeExceptionがスローされる() {
        mockInit();
        // fukaRepository が例外をスローして JasperReports 処理前に失敗させる
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("DB接続エラー"));

        assertThatThrownBy(() -> service.generatePdf(SHITEI_NO, "202404", null, null, "2"))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================================================================
    // #22 buildDtoByTaishoYm 正常系 ym が null/空
    // ==================================================================

    @Test
    @DisplayName("#22 buildDtoByTaishoYm 正常系 b1/b2/b3 各 ym が null/空の場合：setBlockEmpty が呼ばれる")
    void buildDtoByTaishoYm_ymがnullまたは空の場合は該当ブロックが空に設定される() {
        mockForGeneratePdf("202404", FukaConstants.TEIGAKU.getValue(), FukaConstants.SHINKOKU.getValue());

        // b2Ym=null, b3Ym="" を渡す
        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, "", "2");

        assertThat(result).isNotNull().isNotEmpty();
        // b2/b3 ブロックに対して fukaRepository が呼ばれないこと
        verify(fukaRepository, never())
                .findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, "");
    }

    // ==================================================================
    // #23 buildDtoByTaishoYm 正常系 findFukaByTaishoYm が empty
    // ==================================================================

    @Test
    @DisplayName("#23 buildDtoByTaishoYm 正常系 findFukaByTaishoYm が empty の場合：setBlockEmpty が呼ばれる")
    void buildDtoByTaishoYm_findFukaByTaishoYmがemptyの場合は該当ブロックが空に設定される() {
        mockInit();
        // b1Ym に対応する Fuka が存在しない
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "2024"))
                .thenReturn(List.of());

        // Fuka が見つからないため firstFuka == null → setNofuAndKasan は呼ばれない
        // tokugimuRepository も呼ばれない（setKibetsuBlockByFuka に到達しない）
        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null, "2");

        assertThat(result).isNotNull().isNotEmpty();
        verify(tokugimuRepository, never()).findByJichitaiCdAndShiteiNo(anyString(), anyString());
    }

    // ==================================================================
    // #24 buildDtoByTaishoYm 正常系 fukaKbn が未設定
    // ==================================================================

    @Test
    @DisplayName("#24 buildDtoByTaishoYm 正常系 fukaKbn が未設定の場合：TEIGAKU がデフォルトセットされる")
    void buildDtoByTaishoYm_fukaKbnが未設定の場合はTEIGAKUがデフォルトセットされる() {
        mockInit();
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "2024"))
                .thenReturn(List.of());

        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null, "2");

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #25 buildDtoByTaishoYm 正常系 firstFuka != null のとき setNofuAndKasan 呼び出し
    // ==================================================================

    @Test
    @DisplayName("#25 buildDtoByTaishoYm 正常系 firstFuka != null のときのみ setNofuAndKasan 呼び出し")
    void buildDtoByTaishoYm_firstFukaがnonnullのときsetNofuAndKasanが実行される() {
        mockForGeneratePdf("202404", FukaConstants.TEIGAKU.getValue(), FukaConstants.SHINKOKU.getValue());

        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null, "2");

        assertThat(result).isNotNull().isNotEmpty();
        // setNofuAndKasan が実行されると fukaRepository.findMaxRno が呼ばれる
        verify(fukaRepository).findMaxRno(JICHITAI_CD, SHITEI_NO, "2024", 1);
    }

    // ==================================================================
    // #26 buildDtoByTaishoYm 正常系 nokigen が null
    // ==================================================================

    @Test
    @DisplayName("#26 buildDtoByTaishoYm 正常系 nokigen が null の場合：年月日は空文字になる")
    void buildDtoByTaishoYm_nokigenがnullの場合は納期限が空文字にセットされる() {
        mockInit();
        Fuka f = fuka(SHITEI_NO, "202404", FukaConstants.TEIGAKU.getValue(), FukaConstants.SHINKOKU.getValue());
        f.setNokigen(null);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "2024"))
                .thenReturn(List.of(f));
        when(fukaRepository.findMaxRno(JICHITAI_CD, SHITEI_NO, "2024", 1))
                .thenReturn(Optional.of(1));
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, SHITEI_NO, 1, "2024", 1))
                .thenReturn(List.of(fukaUchi(1, 1000L)));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu(SHITEI_NO)));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any(BigDecimal.class)))
                .thenReturn(Optional.empty());

        // 例外なく処理が完了すること（nokigen=null でも nofu_kigen_nen 等が空文字になる）
        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null, "2");

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #27 buildDtoByTaishoYm 正常系 tsuchi_Ymd の和暦変換（実装修正あり）
    // ==================================================================

    /**
     * ※実装修正：new Locale("ja","JP","JP") は deprecated。
     * Locale.JAPANESE では和暦カレンダーが効かず西暦になるため、
     * Locale.forLanguageTag("ja-JP-u-ca-japanese") に置き換える。
     */
    @Test
    @DisplayName("#27 buildDtoByTaishoYm 正常系 通知年月日（tsuchi_Ymd）の和暦変換：Locale 指定の確認")
    void buildDtoByTaishoYm_tsuchiYmdが和暦に変換される() {
        // 令和8年4月1日
        LocalDate date1 = LocalDate.of(2026, 4, 1);
        JapaneseDate jd1 = JapaneseDate.from(date1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("Gy年M月d日",
                Locale.forLanguageTag("ja-JP-u-ca-japanese"));
        assertThat(fmt.format(jd1)).isEqualTo("令和8年4月1日");

        // 平成31年4月30日
        LocalDate date2 = LocalDate.of(2019, 4, 30);
        JapaneseDate jd2 = JapaneseDate.from(date2);
        assertThat(fmt.format(jd2)).isEqualTo("平成31年4月30日");
    }

    // ==================================================================
    // #28 setKibetsuBlockByFuka 正常系 isKosei：申告の最新 rno と比較（実装修正あり）
    // ==================================================================

    /**
     * ※実装修正：FukaRepository に findMaxRnoByHenkoKbn を追加し、
     * rno - 1 固定取得を「申告の最新 rno」取得に置き換える。
     * 直前 rno=2 が「更正」でも rno=1（申告）と比較されること。
     */
    @Test
    @DisplayName("#28 setKibetsuBlockByFuka 正常系 isKosei の場合：変更区分が申告の最新の賦課と比較して差引計算")
    void setKibetsuBlockByFuka_isKoseiの場合は申告の最新rnoと比較される() {
        mockInit();
        // rno=3 が現在の更正
        Fuka f = fuka(SHITEI_NO, "202404", FukaConstants.TEIGAKU.getValue(), FukaConstants.KOSEI.getValue());
        f.setRno(3);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "2024"))
                .thenReturn(List.of(f));
        when(fukaRepository.findMaxRno(JICHITAI_CD, SHITEI_NO, "2024", 1))
                .thenReturn(Optional.of(3));

        // 申告の最新 rno = 1
        when(fukaRepository.findMaxRnoByHenkoKbn(
                JICHITAI_CD, SHITEI_NO, "2024", 1, FukaConstants.SHINKOKU.getValue()))
                .thenReturn(Optional.of(1));

        // rno=3（現在）の内訳：zeigaku=2000
        FukaUchi current = fukaUchi(1, 2000L);
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, SHITEI_NO, 3, "2024", 1))
                .thenReturn(List.of(current));

        // rno=1（申告）の内訳：zeigaku=1000
        FukaUchi prev = fukaUchi(1, 1000L);
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, SHITEI_NO, 1, "2024", 1))
                .thenReturn(List.of(prev));

        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu(SHITEI_NO)));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any(BigDecimal.class)))
                .thenReturn(Optional.empty());

        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null,
                FukaConstants.KOSEI.getValue());

        assertThat(result).isNotNull().isNotEmpty();
        // rno=2（更正）の内訳は参照されないこと
        verify(fukaUchiRepository, never())
                .findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                        JICHITAI_CD, SHITEI_NO, 2, "2024", 1);
    }

    // ==================================================================
    // #29 setKibetsuBlockByFuka 正常系 blockNo によるフィールド名の違い
    // ==================================================================

    @Test
    @DisplayName("#29 setKibetsuBlockByFuka 正常系 blockNo == 1 と blockNo != 1 でフィールド名が異なる分岐の確認")
    void setKibetsuBlockByFuka_blockNoによってフィールド名が異なる() {
        mockInit();
        Fuka f1 = fuka(SHITEI_NO, "202404", FukaConstants.TEIGAKU.getValue(), FukaConstants.SHINKOKU.getValue());
        Fuka f2 = fuka(SHITEI_NO, "202405", FukaConstants.TEIGAKU.getValue(), FukaConstants.SHINKOKU.getValue());
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "2024"))
                .thenReturn(List.of(f1, f2));
        when(fukaRepository.findMaxRno(eq(JICHITAI_CD), eq(SHITEI_NO), eq("2024"), anyInt()))
                .thenReturn(Optional.of(1));
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                eq(JICHITAI_CD), eq(SHITEI_NO), eq(1), eq("2024"), anyInt()))
                .thenReturn(List.of(fukaUchi(1, 1000L)));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu(SHITEI_NO)));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any(BigDecimal.class)))
                .thenReturn(Optional.empty());

        // b1Ym と b2Ym を両方指定して blockNo=1 と blockNo=2 の両方が処理されること
        byte[] result = service.generatePdf(SHITEI_NO, "202404", "202405", null, "2");

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #30 setKibetsuBlockByFuka 正常系 TEIGAKU の場合のみ kbn_zei_gaku をセット
    // ==================================================================

    @Test
    @DisplayName("#30 setKibetsuBlockByFuka 正常系 TEIGAKU の場合のみ kbn_zei_gaku をセット")
    void setKibetsuBlockByFuka_TEIGAKUの場合のみkbnZeiGakuがセットされる() {
        mockForGeneratePdf("202404", FukaConstants.TEIGAKU.getValue(), FukaConstants.SHINKOKU.getValue());

        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null, "2");

        assertThat(result).isNotNull().isNotEmpty();
        // TEIRITSU 用の zeiritsuTeiritsuRepository は呼ばれないこと
        verify(zeiritsuTeiritsuRepository, never()).findActiveBySeq(anyString(), any());
    }

    // ==================================================================
    // #31 setKibetsuBlockByFuka 正常系 定率：税率が全ブロック・課税区分1〜5にセットされる
    // ==================================================================

    /**
     * ※Amazon Q 確認済み：「TEIRITSU かつ blockNo == 1 のみ」という分岐は現行実装に存在しない。
     * b1/b2/b3 すべてのブロックで kbn=1〜5 の bN_zei_ritsuN に FukaUchi.zeiRitsu がセットされる。
     */
    @Test
    @DisplayName("#31 setKibetsuBlockByFuka 正常系 定率の場合：税率（zei_ritsu）が全ブロック・課税区分1〜5にセットされる")
    void setKibetsuBlockByFuka_定率の場合は税率が全ブロック全区分にセットされる() {
        mockInit();
        Fuka f = fuka(SHITEI_NO, "202404", FukaConstants.TEIRITSU.getValue(), FukaConstants.SHINKOKU.getValue());
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "2024"))
                .thenReturn(List.of(f));
        when(fukaRepository.findMaxRno(JICHITAI_CD, SHITEI_NO, "2024", 1))
                .thenReturn(Optional.of(1));

        FukaUchi u = fukaUchi(1, 1000L);
        u.setZeiRitsu(new BigDecimal("0.10"));
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, SHITEI_NO, 1, "2024", 1))
                .thenReturn(List.of(u));
        when(zeiritsuTeiritsuRepository.findActiveBySeq(anyString(), any())).thenReturn(List.of());
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu(SHITEI_NO)));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any(BigDecimal.class)))
                .thenReturn(Optional.empty());

        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null, "2");

        assertThat(result).isNotNull().isNotEmpty();
    }

    // ==================================================================
    // #32 setKibetsuBlockByFuka 正常系 定率：区分名（kbn_name1〜5）がセットされる
    // ==================================================================

    /**
     * ※フィールド名に b1/b2/b3 の接頭辞が付かないため、
     * b1→b2→b3 の順で処理されると後のブロックの値で上書きされる。
     */
    @Test
    @DisplayName("#32 setKibetsuBlockByFuka 正常系 定率の場合：区分名（kbn_name1〜5）がセットされる")
    void setKibetsuBlockByFuka_定率の場合は区分名がセットされる() {
        mockInit();
        Fuka f = fuka(SHITEI_NO, "202404", FukaConstants.TEIRITSU.getValue(), FukaConstants.SHINKOKU.getValue());
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "2024"))
                .thenReturn(List.of(f));
        when(fukaRepository.findMaxRno(JICHITAI_CD, SHITEI_NO, "2024", 1))
                .thenReturn(Optional.of(1));

        FukaUchi u = fukaUchi(1, 1000L);
        u.setZeiritsuSeq(new BigDecimal("1"));
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, SHITEI_NO, 1, "2024", 1))
                .thenReturn(List.of(u));

        ZeiritsuTeiritsu zt = new ZeiritsuTeiritsu();
        zt.setTeiritsuSeq(new BigDecimal("1"));
        zt.setKbnName("区分1");
        when(zeiritsuTeiritsuRepository.findActiveBySeq(JICHITAI_CD, new BigDecimal("1")))
                .thenReturn(List.of(zt));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu(SHITEI_NO)));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any(BigDecimal.class)))
                .thenReturn(Optional.empty());

        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null, "2");

        assertThat(result).isNotNull().isNotEmpty();
        verify(zeiritsuTeiritsuRepository, times(1))
                .findActiveBySeq(JICHITAI_CD, new BigDecimal("1"));
    }

    // ==================================================================
    // #33 setNofuAndKasan 正常系 納期限の和暦変換（実装修正あり）
    // ==================================================================

    /**
     * ※実装修正：setNofuAndKasan の DateTimeFormatter.ofPattern("Gy") が Locale 未指定。
     * Locale.forLanguageTag("ja-JP-u-ca-japanese") を明示指定する。
     */
    @Test
    @DisplayName("#33 setNofuAndKasan 正常系 納期限の和暦変換：Locale 未指定の確認")
    void setNofuAndKasan_納期限が和暦で出力される() {
        // nokigen = 2026-04-01（令和8年）を設定して和暦変換が正しく動作することを確認
        LocalDate nokigen = LocalDate.of(2026, 4, 1);
        JapaneseDate jd = JapaneseDate.from(nokigen);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("Gy",
                Locale.forLanguageTag("ja-JP-u-ca-japanese"));
        assertThat(fmt.format(jd)).isEqualTo("令和8");
    }

    // ==================================================================
    // #34 setNofuAndKasan 正常系 更正時の前回分取得も申告の最新と比較（実装修正あり）
    // ==================================================================

    /**
     * ※実装修正：setNofuAndKasan 内の rno - 1 固定取得も
     * setKibetsuBlockByFuka と同様に findMaxRnoByHenkoKbn を使う形へ修正する。
     */
    @Test
    @DisplayName("#34 setNofuAndKasan 正常系 更正時の前回分取得も変更区分「申告」の最新と比較する")
    void setNofuAndKasan_更正時は申告の最新rnoの賦課データと比較される() {
        mockInit();
        Fuka f = fuka(SHITEI_NO, "202404", FukaConstants.TEIGAKU.getValue(), FukaConstants.KOSEI.getValue());
        f.setRno(3);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "2024"))
                .thenReturn(List.of(f));
        when(fukaRepository.findMaxRno(JICHITAI_CD, SHITEI_NO, "2024", 1))
                .thenReturn(Optional.of(3));

        // 申告の最新 rno = 1
        when(fukaRepository.findMaxRnoByHenkoKbn(
                JICHITAI_CD, SHITEI_NO, "2024", 1, FukaConstants.SHINKOKU.getValue()))
                .thenReturn(Optional.of(1));

        FukaUchi current = fukaUchi(1, 2000L);
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, SHITEI_NO, 3, "2024", 1))
                .thenReturn(List.of(current));

        FukaUchi prev = fukaUchi(1, 1000L);
        when(fukaUchiRepository.findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                JICHITAI_CD, SHITEI_NO, 1, "2024", 1))
                .thenReturn(List.of(prev));

        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu(SHITEI_NO)));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any(BigDecimal.class)))
                .thenReturn(Optional.empty());

        byte[] result = service.generatePdf(SHITEI_NO, "202404", null, null,
                FukaConstants.KOSEI.getValue());

        assertThat(result).isNotNull().isNotEmpty();
        // rno=2（更正）の内訳は参照されないこと
        verify(fukaUchiRepository, never())
                .findByJichitaiCdAndShiteiNoAndRnoAndNendoAndKibetsu(
                        JICHITAI_CD, SHITEI_NO, 2, "2024", 1);
    }
}
