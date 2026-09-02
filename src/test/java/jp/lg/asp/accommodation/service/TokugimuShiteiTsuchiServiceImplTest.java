package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
import jp.lg.asp.accommodation.dto.TokugimuShiteiTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.TokugimuShiteiTsuchiServiceImpl;

/**
 * 特別徴収義務者指定通知 単体テスト（サービス）
 *
 * <p>チェックリストの #11〜#18 に1対1で対応する。
 * チェックリストはあるべき仕様で書かれている。テストが通るように期待値を実装へ寄せないこと。</p>
 */
@ExtendWith(MockitoExtension.class)
class TokugimuShiteiTsuchiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks TokugimuShiteiTsuchiServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "0001";
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
        t.setShisetsuYubinNo("0600001");
        t.setShisetsuJusho("北海道札幌市");
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

    private Jichitai jichitai() {
        Jichitai j = new Jichitai();
        j.setName("○○市");
        j.setKbnName("市");
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

    private void stubJichitaiInfo(Jichitai j) {
        when(reportsCommonService.getJichitaiInfo()).thenReturn(j);
    }

    // ==================================================================
    // getTokugimuInfo
    // ==================================================================

    @Test
    @DisplayName("#11 getTokugimuInfo 正常系 正常系：tokugimuRepository と atenaRepository が値を返す場合")
    void getTokugimuInfo_全情報が正常に取得できる() {
        stubJichitaiInfo(jichitai());
        when(reportsCommonService.getReportsDefText(ReportsConstants.TOKUGIMU_SHITEI_JOREI))
                .thenReturn("○○市宿泊税条例");
        stubTokugimu(tokugimu());
        stubAtena(atena());

        TokugimuShiteiTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(result.getTokuName()).isEqualTo("山田太郎");
        assertThat(result.getShisetsuName()).isEqualTo("テストホテル");
        assertThat(result.getCityName()).isEqualTo("○○市");
        assertThat(result.getJorei()).isEqualTo("○○市宿泊税条例");
    }

    @Test
    @DisplayName("#12 getTokugimuInfo 異常系 tokugimuRepository.findBy... が Optional.empty() を返す場合")
    void getTokugimuInfo_特別徴収義務者なしはnull() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0")).thenReturn(Optional.empty());

        assertThat(service.getTokugimuInfo(SHITEI_NO)).isNull();
    }

    @Test
    @DisplayName("#13 getTokugimuInfo 異常系 atenaRepository.findBy... が Optional.empty() を返す場合")
    void getTokugimuInfo_宛名なしはnull() {
        stubTokugimu(tokugimu());
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, ATENA_NO))
                .thenReturn(Optional.empty());

        assertThat(service.getTokugimuInfo(SHITEI_NO)).isNull();
    }

    @Test
    @DisplayName("#14 getTokugimuInfo 正常系 atena.getYubinNo() が null の場合")
    void getTokugimuInfo_宛名郵便番号nullは空文字() {
        stubTokugimu(tokugimu());
        Atena atena = atena();
        atena.setYubinNo(null);
        stubAtena(atena);

        assertThat(service.getTokugimuInfo(SHITEI_NO).getTokuYubinNo()).isEmpty();
    }

    @Test
    @DisplayName("#15 getTokugimuInfo 正常系 atena.getJusho() が null の場合")
    void getTokugimuInfo_宛名住所nullは空文字() {
        stubTokugimu(tokugimu());
        Atena atena = atena();
        atena.setJusho(null);
        stubAtena(atena);

        assertThat(service.getTokugimuInfo(SHITEI_NO).getTokuJusho()).isEmpty();
    }

    @Test
    @DisplayName("#16 getTokugimuInfo 正常系 tokugimu.getShisetsuYubinNo() が null の場合")
    void getTokugimuInfo_施設郵便番号nullは空文字() {
        Tokugimu tokugimu = tokugimu();
        tokugimu.setShisetsuYubinNo(null);
        stubTokugimu(tokugimu);
        stubAtena(atena());

        assertThat(service.getTokugimuInfo(SHITEI_NO).getShisetsuYubinNo()).isEmpty();
    }

    @Test
    @DisplayName("#17 getTokugimuInfo 正常系 tokugimu.getShisetsuJusho() が null の場合")
    void getTokugimuInfo_施設住所nullは空文字() {
        Tokugimu tokugimu = tokugimu();
        tokugimu.setShisetsuJusho(null);
        stubTokugimu(tokugimu);
        stubAtena(atena());

        assertThat(service.getTokugimuInfo(SHITEI_NO).getShisetsuJusho()).isEmpty();
    }

    @Test
    @DisplayName("#18 getTokugimuInfo 正常系 reportsCommonService.getJichitaiInfo() が null を返す場合")
    void getTokugimuInfo_自治体情報nullでも処理が続行される() {
        lenient().when(reportsCommonService.getJichitaiInfo()).thenReturn(null);
        stubTokugimu(tokugimu());
        stubAtena(atena());

        assertThatCode(() -> service.getTokugimuInfo(SHITEI_NO)).doesNotThrowAnyException();

        TokugimuShiteiTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);
        assertThat(result.getCityName()).isNull();
        assertThat(result.getCity()).isNull();
    }
}
