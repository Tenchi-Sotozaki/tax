package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.NozeiKanriShoninTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Nokan;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.NokanRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.NozeiKanriShoninTsuchiServiceImpl;

/**
 * 納税管理人承認(不承認)通知書 単体テスト（サービス）
 *
 * <p>チェックリストの #15〜#25 に1対1で対応する。</p>
 */
@ExtendWith(MockitoExtension.class)
class NozeiKanriShoninTsuchiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock NokanRepository nokanRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks NozeiKanriShoninTsuchiServiceImpl service;

    private static final String JICHITAI_CD = "01100";
    private static final String SHITEI_NO = "S001";
    private static final BigDecimal ATENA_NO = BigDecimal.ONE;

    @BeforeEach
    void setUp() {
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        lenient().when(reportsCommonService.getReportsDefText(any())).thenReturn("○○市宿泊税条例第○条");
        lenient().when(reportsCommonService.getReportsDefData(any())).thenReturn(null);
    }

    // ------------------------------------------------------------------
    // テストデータ生成ヘルパー
    // ------------------------------------------------------------------

    private Jichitai jichitai(String name) {
        Jichitai j = new Jichitai();
        j.setJichitaiCd(JICHITAI_CD);
        j.setName(name);
        j.setKbnName("市");
        // NPE回避：param は @Column(nullable=false)
        j.setParam("");
        return j;
    }

    private Tokugimu tokugimu(String shisetsuName, String shisetsuYubinNo, String shisetsuJusho) {
        Tokugimu t = new Tokugimu();
        t.setJichitaiCd(JICHITAI_CD);
        t.setShiteiNo(SHITEI_NO);
        t.setAtenaNo(ATENA_NO);
        t.setShisetsuName(shisetsuName);
        t.setShisetsuYubinNo(shisetsuYubinNo);
        t.setShisetsuJusho(shisetsuJusho);
        // NPE回避：@Column(nullable=false) フィールドに最低限の値を設定
        t.setNewFlg("1");
        t.setDelFlg("0");
        return t;
    }

    private Atena atena(String name, String yubinNo, String jusho) {
        Atena a = new Atena();
        a.setJichitaiCd(JICHITAI_CD);
        a.setAtenaNo(ATENA_NO);
        a.setName(name);
        a.setYubinNo(yubinNo);
        a.setJusho(jusho);
        // NPE回避：kbn は @Column(nullable=false)
        a.setKbn("1");
        return a;
    }

    private Nokan nokan(String yubinNo, String jusho, String name) {
        Nokan n = new Nokan();
        n.setJichitaiCd(JICHITAI_CD);
        n.setShiteiNo(SHITEI_NO);
        n.setYubinNo(yubinNo);
        n.setJusho(jusho);
        n.setName(name);
        n.setKbn("1");
        return n;
    }

    /** 正常系の共通スタブ（jichitai・tokugimu・atena・nokan） */
    private void stubNormal(Jichitai j, Tokugimu t, Atena a, Nokan n) {
        lenient().when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.ofNullable(j));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, ATENA_NO))
                .thenReturn(Optional.of(a));
        when(nokanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(n));
    }

    // ==================================================================
    // #15 getNozeiKanriInfo
    // ==================================================================

    @Test
    @DisplayName("#15 getNozeiKanriInfo 正常系 全情報が正常に取得できる場合")
    void getNozeiKanriInfo_全情報が正常に取得できる場合() {
        Jichitai j = jichitai("札幌");
        Tokugimu t = tokugimu("テストホテル", "1234567", "東京都千代田区1-1");
        Atena a = atena("山田太郎", "1234567", "東京都千代田区1-1");
        Nokan n = nokan("1234567", "東京都港区2-2", "納税管理人太郎");
        stubNormal(j, t, a, n);
        when(reportsCommonService.getReportsDefText(ReportsConstants.NOZEI_KANRININ_SHONIN_JOREI))
                .thenReturn("○○市宿泊税条例第○条");

        NozeiKanriShoninTsuchiDto dto = service.getNozeiKanriInfo(SHITEI_NO);

        assertThat(dto.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(dto.getCityName()).isEqualTo("札幌");
        assertThat(dto.getJorei()).isEqualTo("○○市宿泊税条例第○条");
        assertThat(dto.getTokuName()).isEqualTo("山田太郎");
        assertThat(dto.getShisetsuName()).isEqualTo("テストホテル");
        assertThat(dto.getNozeiKanriName()).isEqualTo("納税管理人太郎");
    }

    // ==================================================================
    // #16 getNozeiKanriInfo
    // ==================================================================

    /**
     * ※現行実装は ifPresent で読み飛ばし、納税管理人の項目が null のまま DTO を返すため、実装側の修正が必要
     */
    @Test
    @DisplayName("#16 getNozeiKanriInfo 異常系 nokanRepository が Optional.empty() を返す場合（納税管理人未登録）：例外となる")
    void getNozeiKanriInfo_nokanが存在しない場合は例外がスローされる() {
        Tokugimu t = tokugimu("テストホテル", "1234567", "東京都千代田区1-1");
        Atena a = atena("山田太郎", "1234567", "東京都千代田区1-1");
        lenient().when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai("札幌")));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, ATENA_NO))
                .thenReturn(Optional.of(a));
        when(nokanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNozeiKanriInfo(SHITEI_NO))
                .isInstanceOf(Exception.class);
    }

    // ==================================================================
    // #17 getNozeiKanriInfo
    // ==================================================================

    /**
     * ※現行実装は "[自治体名]宿泊税条例" にフォールバックするため、実装側の修正が必要
     */
    @Test
    @DisplayName("#17 getNozeiKanriInfo 正常系 reportsCommonService.getReportsDefText が null を返す場合：条例は空欄となる")
    void getNozeiKanriInfo_getReportsDefTextがnullの場合はjoreiが空文字になる() {
        Jichitai j = jichitai("札幌");
        Tokugimu t = tokugimu("テストホテル", "1234567", "東京都千代田区1-1");
        Atena a = atena("山田太郎", "1234567", "東京都千代田区1-1");
        Nokan n = nokan("1234567", "東京都港区2-2", "納税管理人太郎");
        stubNormal(j, t, a, n);
        when(reportsCommonService.getReportsDefText(ReportsConstants.NOZEI_KANRININ_SHONIN_JOREI))
                .thenReturn(null);

        NozeiKanriShoninTsuchiDto dto = service.getNozeiKanriInfo(SHITEI_NO);

        assertThat(dto.getJorei()).isEqualTo("");
    }

    // ==================================================================
    // #18 getNozeiKanriInfo
    // ==================================================================

    /**
     * ※現行実装は "[自治体名]宿泊税条例" にフォールバックするため、実装側の修正が必要
     */
    @Test
    @DisplayName("#18 getNozeiKanriInfo 正常系 reportsCommonService.getReportsDefText が空文字を返す場合：条例は空欄となる")
    void getNozeiKanriInfo_getReportsDefTextが空文字の場合はjoreiが空文字になる() {
        Jichitai j = jichitai("札幌");
        Tokugimu t = tokugimu("テストホテル", "1234567", "東京都千代田区1-1");
        Atena a = atena("山田太郎", "1234567", "東京都千代田区1-1");
        Nokan n = nokan("1234567", "東京都港区2-2", "納税管理人太郎");
        stubNormal(j, t, a, n);
        when(reportsCommonService.getReportsDefText(ReportsConstants.NOZEI_KANRININ_SHONIN_JOREI))
                .thenReturn("");

        NozeiKanriShoninTsuchiDto dto = service.getNozeiKanriInfo(SHITEI_NO);

        assertThat(dto.getJorei()).isEqualTo("");
    }

    // ==================================================================
    // #19 getNozeiKanriInfo
    // ==================================================================

    @Test
    @DisplayName("#19 getNozeiKanriInfo 正常系 jichitaiRepository.findById が Optional.empty() を返す場合")
    void getNozeiKanriInfo_jichitaiが存在しない場合はcityNameが空文字になる() {
        Tokugimu t = tokugimu("テストホテル", "1234567", "東京都千代田区1-1");
        Atena a = atena("山田太郎", "1234567", "東京都千代田区1-1");
        Nokan n = nokan("1234567", "東京都港区2-2", "納税管理人太郎");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, ATENA_NO))
                .thenReturn(Optional.of(a));
        when(nokanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(n));

        NozeiKanriShoninTsuchiDto dto = service.getNozeiKanriInfo(SHITEI_NO);

        assertThat(dto.getCityName()).isEqualTo("");
    }

    // ==================================================================
    // #20 getNozeiKanriInfo
    // ==================================================================

    @Test
    @DisplayName("#20 getNozeiKanriInfo 異常系 tokugimuRepository.findByJichitaiCdAndShiteiNo が空リストを返す場合")
    void getNozeiKanriInfo_tokugimuが存在しない場合はRuntimeExceptionがスローされる() {
        lenient().when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai("札幌")));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S999"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getNozeiKanriInfo("S999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("特別徴収義務者が見つかりません: S999");
    }

    // ==================================================================
    // #21 getNozeiKanriInfo
    // ==================================================================

    @Test
    @DisplayName("#21 getNozeiKanriInfo 異常系 atenaRepository.findByJichitaiCdAndAtenaNo が Optional.empty() を返す場合")
    void getNozeiKanriInfo_atenaが存在しない場合はRuntimeExceptionがスローされる() {
        Tokugimu t = tokugimu("テストホテル", "1234567", "東京都千代田区1-1");
        lenient().when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai("札幌")));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, ATENA_NO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNozeiKanriInfo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("宛名情報が見つかりません");
    }

    // ==================================================================
    // #22 getNozeiKanriInfo
    // ==================================================================

    @Test
    @DisplayName("#22 getNozeiKanriInfo 正常系 atena.getYubinNo() が値あり（\"1234567\"）の場合")
    void getNozeiKanriInfo_atenaYubinNoありの場合は郵便番号記号付きで設定される() {
        Jichitai j = jichitai("札幌");
        Tokugimu t = tokugimu("テストホテル", "1234567", "東京都千代田区1-1");
        Atena a = atena("山田太郎", "1234567", "東京都千代田区1-1");
        Nokan n = nokan("1234567", "東京都港区2-2", "納税管理人太郎");
        stubNormal(j, t, a, n);

        NozeiKanriShoninTsuchiDto dto = service.getNozeiKanriInfo(SHITEI_NO);

        assertThat(dto.getTokuYubin()).isEqualTo("〒1234567");
    }

    // ==================================================================
    // #23 getNozeiKanriInfo
    // ==================================================================

    /**
     * ※現行実装は null チェックなしで連結しているため、実装側の修正が必要
     */
    @Test
    @DisplayName("#23 getNozeiKanriInfo 正常系 atena.getYubinNo() が null の場合")
    void getNozeiKanriInfo_atenaYubinNoがnullの場合はtokuYubinが空文字になる() {
        Jichitai j = jichitai("札幌");
        Tokugimu t = tokugimu("テストホテル", "1234567", "東京都千代田区1-1");
        Atena a = atena("山田太郎", null, "東京都千代田区1-1");
        Nokan n = nokan("1234567", "東京都港区2-2", "納税管理人太郎");
        stubNormal(j, t, a, n);

        NozeiKanriShoninTsuchiDto dto = service.getNozeiKanriInfo(SHITEI_NO);

        assertThat(dto.getTokuYubin()).isEqualTo("");
    }

    // ==================================================================
    // #24 getNozeiKanriInfo
    // ==================================================================

    /**
     * ※現行実装は null チェックなしで連結しているため、実装側の修正が必要
     */
    @Test
    @DisplayName("#24 getNozeiKanriInfo 正常系 tokugimu.getShisetsuYubinNo() が null の場合")
    void getNozeiKanriInfo_shisetsuYubinNoがnullの場合はshisetsuYubinが空文字になる() {
        Jichitai j = jichitai("札幌");
        Tokugimu t = tokugimu("テストホテル", null, "東京都千代田区1-1");
        Atena a = atena("山田太郎", "1234567", "東京都千代田区1-1");
        Nokan n = nokan("1234567", "東京都港区2-2", "納税管理人太郎");
        stubNormal(j, t, a, n);

        NozeiKanriShoninTsuchiDto dto = service.getNozeiKanriInfo(SHITEI_NO);

        assertThat(dto.getShisetsuYubin()).isEqualTo("");
    }

    // ==================================================================
    // #25 getNozeiKanriInfo
    // ==================================================================

    /**
     * ※現行実装は null チェックなしで連結しているため、実装側の修正が必要
     */
    @Test
    @DisplayName("#25 getNozeiKanriInfo 正常系 nokan.getYubinNo() が null の場合")
    void getNozeiKanriInfo_nokanYubinNoがnullの場合はnozeiKanriYubinが空文字になる() {
        Jichitai j = jichitai("札幌");
        Tokugimu t = tokugimu("テストホテル", "1234567", "東京都千代田区1-1");
        Atena a = atena("山田太郎", "1234567", "東京都千代田区1-1");
        Nokan n = nokan(null, "東京都港区2-2", "納税管理人太郎");
        stubNormal(j, t, a, n);

        NozeiKanriShoninTsuchiDto dto = service.getNozeiKanriInfo(SHITEI_NO);

        assertThat(dto.getNozeiKanriYubin()).isEqualTo("");
    }
}
