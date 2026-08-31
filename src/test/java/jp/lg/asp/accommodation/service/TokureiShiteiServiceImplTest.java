package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import jp.lg.asp.accommodation.dto.TokureiShiteiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Nokan;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.NokanRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.TokureiShiteiServiceImpl;

/**
 * 納入申告書の提出期限等の特例適用者指定通知書 単体テスト（サービス）
 *
 * <p>チェックリストの #11〜#23 に1対1で対応する。</p>
 */
@ExtendWith(MockitoExtension.class)
class TokureiShiteiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock NokanRepository nokanRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks TokureiShiteiServiceImpl service;

    private static final String JICHITAI_CD = "01100";
    private static final String SHITEI_NO = "0001";
    private static final BigDecimal ATENA_NO = BigDecimal.ONE;

    @BeforeEach
    void setUp() {
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        // init() 内の reportsCommonService 呼び出しをデフォルトでスタブ（各テストで上書き可）
        lenient().when(reportsCommonService.getJichitaiInfo()).thenReturn(null);
        lenient().when(reportsCommonService.getReportsDefText(any())).thenReturn(null);
        lenient().when(reportsCommonService.getReportsDefData(any())).thenReturn(null);
    }

    // ------------------------------------------------------------------
    // テストデータ生成ヘルパー
    // ------------------------------------------------------------------

    private Tokugimu tokugimu(String shisetsuName, String shisetsuYubinNo, String shisetsuJusho, String biko) {
        Tokugimu t = new Tokugimu();
        t.setJichitaiCd(JICHITAI_CD);
        t.setShiteiNo(SHITEI_NO);
        t.setAtenaNo(ATENA_NO);
        t.setShisetsuName(shisetsuName);
        t.setShisetsuYubinNo(shisetsuYubinNo);
        t.setShisetsuJusho(shisetsuJusho);
        t.setBiko(biko);
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

    private Nokan nokan(String kbn) {
        Nokan n = new Nokan();
        n.setJichitaiCd(JICHITAI_CD);
        n.setShiteiNo(SHITEI_NO);
        n.setKbn(kbn);
        return n;
    }

    private Jichitai jichitai(String name, String kbnName) {
        Jichitai j = new Jichitai();
        j.setJichitaiCd(JICHITAI_CD);
        j.setName(name);
        j.setKbnName(kbnName);
        // NPE回避：param は @Column(nullable=false)
        j.setParam("");
        return j;
    }

    /** 正常系の共通スタブ（tokugimu・atena・nokan） */
    private void stubNormal(Tokugimu t, Atena a, Nokan n) {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0")).thenReturn(Optional.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, ATENA_NO))
                .thenReturn(Optional.of(a));
        when(nokanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.of(n));
    }

    // ==================================================================
    // #11 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#11 getTokugimuInfo 正常系 全情報が正常に取得できる場合")
    void getTokugimuInfo_全情報正常取得() {
        Tokugimu t = tokugimu("テストホテル", "1234567", "東京都千代田区1-1", "備考テスト");
        Atena a = atena("山田太郎", "1234567", "東京都千代田区1-1");
        Nokan n = nokan("1");
        stubNormal(t, a, n);

        Jichitai j = jichitai("札幌市", "市");
        when(reportsCommonService.getJichitaiInfo()).thenReturn(j);
        when(reportsCommonService.getReportsDefText(ReportsConstants.TOKUREI_SHITEI_JOREI))
                .thenReturn("条例テスト");

        TokureiShiteiDto dto = service.getTokugimuInfo(SHITEI_NO);

        assertThat(dto).isNotNull();
        assertThat(dto.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(dto.getTokuName()).isEqualTo("山田太郎");
        assertThat(dto.getShisetsuName()).isEqualTo("テストホテル");
        assertThat(dto.getCity()).isEqualTo("札幌市市");
        assertThat(dto.getJorei()).isEqualTo("条例テスト");
        assertThat(dto.getShonin()).isEqualTo("1");
    }

    // ==================================================================
    // #12 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#12 getTokugimuInfo 異常系 tokugimuRepository.findBy... が Optional.empty() を返す場合")
    void getTokugimuInfo_tokugimuが存在しない場合はnullを返す() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, "9999", "1", "0")).thenReturn(Optional.empty());

        TokureiShiteiDto dto = service.getTokugimuInfo("9999");

        assertThat(dto).isNull();
    }

    // ==================================================================
    // #13 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#13 getTokugimuInfo 異常系 atenaRepository.findBy... が Optional.empty() を返す場合")
    void getTokugimuInfo_atenaが存在しない場合はnullを返す() {
        Tokugimu t = tokugimu("テストホテル", null, null, null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0")).thenReturn(Optional.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, ATENA_NO))
                .thenReturn(Optional.empty());

        TokureiShiteiDto dto = service.getTokugimuInfo(SHITEI_NO);

        assertThat(dto).isNull();
    }

    // ==================================================================
    // #14 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#14 getTokugimuInfo 異常系 nokanRepository.findBy... が Optional.empty() を返す場合")
    void getTokugimuInfo_nokanが存在しない場合はnullを返す() {
        Tokugimu t = tokugimu("テストホテル", null, null, null);
        Atena a = atena("山田太郎", null, null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0")).thenReturn(Optional.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, ATENA_NO))
                .thenReturn(Optional.of(a));
        when(nokanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(Optional.empty());

        TokureiShiteiDto dto = service.getTokugimuInfo(SHITEI_NO);

        assertThat(dto).isNull();
    }

    // ==================================================================
    // #15 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#15 getTokugimuInfo 正常系 atena.getYubinNo() が null の場合")
    void getTokugimuInfo_atenaYubinNoがnullの場合はtokuYubinがnull() {
        Tokugimu t = tokugimu("テストホテル", null, null, null);
        Atena a = atena("山田太郎", null, null);
        stubNormal(t, a, nokan("1"));

        TokureiShiteiDto dto = service.getTokugimuInfo(SHITEI_NO);

        assertThat(dto).isNotNull();
        assertThat(dto.getTokuYubin()).isNull();
    }

    // ==================================================================
    // #16 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#16 getTokugimuInfo 正常系 atena.getYubinNo() が値あり（\"1234567\"）の場合")
    void getTokugimuInfo_atenaYubinNoありの場合は郵便番号記号付きで設定される() {
        Tokugimu t = tokugimu("テストホテル", null, null, null);
        Atena a = atena("山田太郎", "1234567", null);
        stubNormal(t, a, nokan("1"));

        TokureiShiteiDto dto = service.getTokugimuInfo(SHITEI_NO);

        assertThat(dto).isNotNull();
        assertThat(dto.getTokuYubin()).isEqualTo("〒1234567");
    }

    // ==================================================================
    // #17 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#17 getTokugimuInfo 正常系 atena.getJusho() が null の場合")
    void getTokugimuInfo_atenaJushoがnullの場合はtokuJushoがnull() {
        Tokugimu t = tokugimu("テストホテル", null, null, null);
        Atena a = atena("山田太郎", null, null);
        stubNormal(t, a, nokan("1"));

        TokureiShiteiDto dto = service.getTokugimuInfo(SHITEI_NO);

        assertThat(dto).isNotNull();
        assertThat(dto.getTokuJusho()).isNull();
    }

    // ==================================================================
    // #18 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#18 getTokugimuInfo 正常系 tokugimu.getShisetsuYubinNo() が null の場合")
    void getTokugimuInfo_shisetsuYubinNoがnullの場合はshisetsuYubinがnull() {
        Tokugimu t = tokugimu("テストホテル", null, null, null);
        Atena a = atena("山田太郎", null, null);
        stubNormal(t, a, nokan("1"));

        TokureiShiteiDto dto = service.getTokugimuInfo(SHITEI_NO);

        assertThat(dto).isNotNull();
        assertThat(dto.getShisetsuYubin()).isNull();
    }

    // ==================================================================
    // #19 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#19 getTokugimuInfo 正常系 tokugimu.getShisetsuJusho() が null の場合")
    void getTokugimuInfo_shisetsuJushoがnullの場合はshisetsuJushoがnull() {
        Tokugimu t = tokugimu("テストホテル", null, null, null);
        Atena a = atena("山田太郎", null, null);
        stubNormal(t, a, nokan("1"));

        TokureiShiteiDto dto = service.getTokugimuInfo(SHITEI_NO);

        assertThat(dto).isNotNull();
        assertThat(dto.getShisetsuJusho()).isNull();
    }

    // ==================================================================
    // #20 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#20 getTokugimuInfo 正常系 tokugimu.getBiko() が null の場合")
    void getTokugimuInfo_bikoがnullの場合は空文字になる() {
        Tokugimu t = tokugimu("テストホテル", null, null, null);
        Atena a = atena("山田太郎", null, null);
        stubNormal(t, a, nokan("1"));

        TokureiShiteiDto dto = service.getTokugimuInfo(SHITEI_NO);

        assertThat(dto).isNotNull();
        assertThat(dto.getBiko()).isEqualTo("");
    }

    // ==================================================================
    // #21 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#21 getTokugimuInfo 正常系 nokan.getKbn() が \"1\" の場合")
    void getTokugimuInfo_nokanKbnが1の場合はshoninが1になる() {
        Tokugimu t = tokugimu("テストホテル", null, null, null);
        Atena a = atena("山田太郎", null, null);
        stubNormal(t, a, nokan("1"));

        TokureiShiteiDto dto = service.getTokugimuInfo(SHITEI_NO);

        assertThat(dto).isNotNull();
        assertThat(dto.getShonin()).isEqualTo("1");
    }

    // ==================================================================
    // #22 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#22 getTokugimuInfo 正常系 reportsCommonService.getJichitaiInfo() が null を返す場合")
    void getTokugimuInfo_jichitaiInfoがnullの場合は例外なく続行しcityがnull() {
        Tokugimu t = tokugimu("テストホテル", null, null, null);
        Atena a = atena("山田太郎", null, null);
        stubNormal(t, a, nokan("1"));
        when(reportsCommonService.getJichitaiInfo()).thenReturn(null);

        TokureiShiteiDto dto = service.getTokugimuInfo(SHITEI_NO);

        assertThat(dto).isNotNull();
        assertThat(dto.getCity()).isNull();
    }

    // ==================================================================
    // #23 getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#23 getTokugimuInfo 正常系 reportsCommonService.getJichitaiInfo() が値を返す場合")
    void getTokugimuInfo_jichitaiInfoありの場合はcityがname連結になる() {
        Tokugimu t = tokugimu("テストホテル", null, null, null);
        Atena a = atena("山田太郎", null, null);
        stubNormal(t, a, nokan("1"));
        when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai("札幌", "市"));

        TokureiShiteiDto dto = service.getTokugimuInfo(SHITEI_NO);

        assertThat(dto).isNotNull();
        assertThat(dto.getCity()).isEqualTo("札幌市");
    }
}
