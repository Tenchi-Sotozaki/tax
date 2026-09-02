package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import jp.lg.asp.accommodation.dto.TokugimuJuriTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.TokugimuJuriTsuchiServiceImpl;

/**
 * 特別徴収義務者申請受理通知 単体テスト（サービス）
 *
 * <p>チェックリストの #11〜#22 に1対1で対応する。
 * チェックリストはあるべき仕様で書かれているため、現行実装では落ちるケースがある（#18）。
 * テストが通るように期待値を実装へ寄せないこと。</p>
 */
@ExtendWith(MockitoExtension.class)
class TokugimuJuriTsuchiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks TokugimuJuriTsuchiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "S001";
    private static final BigDecimal ATENA_NO = BigDecimal.ONE;

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ------------------------------------------------------------------
    // テストデータ生成
    // ------------------------------------------------------------------

    private Tokugimu tokugimu() {
        Tokugimu t = new Tokugimu();
        t.setJichitaiCd(JICHITAI_CD);
        t.setShiteiNo(SHITEI_NO);
        t.setAtenaNo(ATENA_NO);
        t.setShisetsuName("テストホテル");
        t.setShisetsuYubinNo("7654321");
        t.setShisetsuJusho("北海道札幌市");
        t.setBiko("備考テスト");
        return t;
    }

    private Atena atena() {
        Atena a = new Atena();
        a.setJichitaiCd(JICHITAI_CD);
        a.setName("山田太郎");
        a.setYubinNo("1234567");
        a.setJusho("東京都千代田区1-1");
        return a;
    }

    private Jichitai jichitai(String name, String kbnName) {
        Jichitai j = new Jichitai();
        j.setName(name);
        j.setKbnName(kbnName);
        return j;
    }

    private void stubTokugimu(Tokugimu t) {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0")).thenReturn(Optional.of(t));
    }

    private void stubAtena(Atena a) {
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, ATENA_NO))
                .thenReturn(Optional.of(a));
    }

    // ==================================================================
    // getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#11 getTokugimuInfo 正常系 tokugimuRepository と atenaRepository が値を返す場合")
    void getTokugimuInfo_全情報が正常に取得できる() {
        when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai("○○市", "長"));
        when(reportsCommonService.getReportsDefText(ReportsConstants.TOKUGIMU_JURI_JOREI))
                .thenReturn("○○市宿泊税条例");
        stubTokugimu(tokugimu());
        stubAtena(atena());

        TokugimuJuriTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(result.getTokuName()).isEqualTo("山田太郎");
        assertThat(result.getShisetsuName()).isEqualTo("テストホテル");
        assertThat(result.getCityName()).isEqualTo("○○市長");
        assertThat(result.getJorei()).isEqualTo("○○市宿泊税条例");
    }

    @Test
    @DisplayName("#12 getTokugimuInfo 正常系 atena.getYubinNo() が null の場合")
    void getTokugimuInfo_宛名郵便番号nullは空文字() {
        stubTokugimu(tokugimu());
        Atena atena = atena();
        atena.setYubinNo(null);
        stubAtena(atena);

        assertThat(service.getTokugimuInfo(SHITEI_NO).getTokuYubin()).isEmpty();
    }

    @Test
    @DisplayName("#13 getTokugimuInfo 正常系 atena.getYubinNo() が値あり（\"1234567\"）の場合")
    void getTokugimuInfo_宛名郵便番号ありは郵便記号付き() {
        stubTokugimu(tokugimu());
        Atena atena = atena();
        atena.setYubinNo("1234567");
        stubAtena(atena);

        assertThat(service.getTokugimuInfo(SHITEI_NO).getTokuYubin()).isEqualTo("〒1234567");
    }

    @Test
    @DisplayName("#14 getTokugimuInfo 正常系 tokugimu.getShisetsuYubinNo() が null の場合")
    void getTokugimuInfo_施設郵便番号nullは空文字() {
        Tokugimu tokugimu = tokugimu();
        tokugimu.setShisetsuYubinNo(null);
        stubTokugimu(tokugimu);
        stubAtena(atena());

        assertThat(service.getTokugimuInfo(SHITEI_NO).getShisetsuYubin()).isEmpty();
    }

    @Test
    @DisplayName("#15 getTokugimuInfo 正常系 tokugimu.getShisetsuYubinNo() が値あり（\"7654321\"）の場合")
    void getTokugimuInfo_施設郵便番号ありは郵便記号付き() {
        Tokugimu tokugimu = tokugimu();
        tokugimu.setShisetsuYubinNo("7654321");
        stubTokugimu(tokugimu);
        stubAtena(atena());

        assertThat(service.getTokugimuInfo(SHITEI_NO).getShisetsuYubin()).isEqualTo("〒7654321");
    }

    @Test
    @DisplayName("#16 getTokugimuInfo 正常系 tokugimu.getShisetsuJusho() が null の場合")
    void getTokugimuInfo_施設住所nullは空文字() {
        Tokugimu tokugimu = tokugimu();
        tokugimu.setShisetsuJusho(null);
        stubTokugimu(tokugimu);
        stubAtena(atena());

        assertThat(service.getTokugimuInfo(SHITEI_NO).getShisetsuJusho()).isEmpty();
    }

    @Test
    @DisplayName("#17 getTokugimuInfo 正常系 reportsCommonService.getJichitaiInfo() が null を返す場合")
    void getTokugimuInfo_自治体情報nullでも処理が続行される() {
        when(reportsCommonService.getJichitaiInfo()).thenReturn(null);
        stubTokugimu(tokugimu());
        stubAtena(atena());

        TokugimuJuriTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result).isNotNull();
        assertThat(result.getCityName()).isNull();
    }

    /**
     * ※現行実装は getName() のみを設定しているため、実装側の修正が必要
     */
    @Test
    @DisplayName("#18 getTokugimuInfo 正常系 reportsCommonService.getJichitaiInfo() が値を返す場合")
    void getTokugimuInfo_自治体名と区分名が連結される() {
        when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai("○○市", "長"));
        stubTokugimu(tokugimu());
        stubAtena(atena());

        assertThat(service.getTokugimuInfo(SHITEI_NO).getCityName())
                .as("getName() + getKbnName() の連結であること")
                .isEqualTo("○○市長");
    }

    @Test
    @DisplayName("#19 getTokugimuInfo 正常系 atena.getJusho() が null の場合")
    void getTokugimuInfo_宛名住所nullは空文字() {
        stubTokugimu(tokugimu());
        Atena atena = atena();
        atena.setJusho(null);
        stubAtena(atena);

        assertThat(service.getTokugimuInfo(SHITEI_NO).getTokuJusho()).isEmpty();
    }

    @Test
    @DisplayName("#20 getTokugimuInfo 正常系 tokugimu.getBiko() が null の場合")
    void getTokugimuInfo_備考nullは空文字() {
        Tokugimu tokugimu = tokugimu();
        tokugimu.setBiko(null);
        stubTokugimu(tokugimu);
        stubAtena(atena());

        assertThat(service.getTokugimuInfo(SHITEI_NO).getBiko())
                .as("NullPointerException とならないこと")
                .isEmpty();
    }

    @Test
    @DisplayName("#21 getTokugimuInfo 異常系 tokugimuRepository.findBy... が Optional.empty() を返す場合")
    void getTokugimuInfo_特別徴収義務者なしはnull() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, "9999", "1", "0")).thenReturn(Optional.empty());

        assertThat(service.getTokugimuInfo("9999")).isNull();
        verify(atenaRepository, never()).findByJichitaiCdAndAtenaNo(any(), any());
    }

    @Test
    @DisplayName("#22 getTokugimuInfo 異常系 atenaRepository.findBy... が Optional.empty() を返す場合")
    void getTokugimuInfo_宛名なしはnull() {
        stubTokugimu(tokugimu());
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, ATENA_NO))
                .thenReturn(Optional.empty());

        assertThat(service.getTokugimuInfo(SHITEI_NO)).isNull();
    }
}
