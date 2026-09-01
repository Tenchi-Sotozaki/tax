package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.TokugimuJuriTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.TokugimuJuriTsuchiServiceImpl;

/**
 * 特別徴収義務者申請受理通知 単体テスト（ServiceImpl）
 *
 * <p>チェックリスト「TokugimuShiteiTsuchiServiceImpl」の #11〜#18 に1対1で対応する。</p>
 */
@ExtendWith(MockitoExtension.class)
class TokugimuShiteiTsuchiServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock ReportsCommonService reportsCommonService;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks TokugimuJuriTsuchiServiceImpl service;

    private static final String JICHITAI_CD = "12345";
    private static final String SHITEI_NO = "0001";

    private void mockInit() {
        Jichitai jichitai = new Jichitai();
        jichitai.setName("札幌");
        jichitai.setKbnName("市");
        when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
        when(reportsCommonService.getReportsDefText(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("条例テスト");
        when(reportsCommonService.getReportsDefData(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new byte[]{1, 2, 3});
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    private Tokugimu tokugimu() {
        Tokugimu t = new Tokugimu();
        t.setShiteiNo(SHITEI_NO);
        t.setShisetsuName("テストホテル");
        t.setShisetsuYubinNo("1234567");
        t.setShisetsuJusho("東京都千代田区1-1");
        t.setAtenaNo(new BigDecimal("1"));
        return t;
    }

    private Atena atena() {
        Atena a = new Atena();
        a.setName("山田太郎");
        a.setYubinNo("1234567");
        a.setJusho("東京都千代田区1-1");
        return a;
    }

    // ==================================================================
    // #11 getTokugimuInfo 正常系
    // ==================================================================

    @Test
    @DisplayName("#11 getTokugimuInfo 正常系 tokugimuRepository と atenaRepository が値を返す場合")
    void getTokugimuInfo_正常系() {
        mockInit();
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu()));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, new BigDecimal("1")))
                .thenReturn(Optional.of(atena()));

        TokugimuJuriTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result).isNotNull();
        assertThat(result.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(result.getTokuName()).isEqualTo("山田太郎");
        assertThat(result.getShisetsuName()).isEqualTo("テストホテル");
        assertThat(result.getCityName()).isEqualTo("札幌");
        assertThat(result.getJorei()).isEqualTo("条例テスト");
    }

    // ==================================================================
    // #12 getTokugimuInfo 異常系
    // ==================================================================

    @Test
    @DisplayName("#12 getTokugimuInfo 異常系 tokugimuRepository.findBy... が Optional.empty() を返す場合")
    void getTokugimuInfo_tokugimuが存在しない場合はnullを返す() {
        mockInit();
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.empty());

        TokugimuJuriTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result).isNull();
    }

    // ==================================================================
    // #13 getTokugimuInfo 異常系
    // ==================================================================

    @Test
    @DisplayName("#13 getTokugimuInfo 異常系 atenaRepository.findBy... が Optional.empty() を返す場合")
    void getTokugimuInfo_atenaが存在しない場合はnullを返す() {
        mockInit();
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu()));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, new BigDecimal("1")))
                .thenReturn(Optional.empty());

        TokugimuJuriTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result).isNull();
    }

    // ==================================================================
    // #14 getTokugimuInfo 正常系
    // ==================================================================

    @Test
    @DisplayName("#14 getTokugimuInfo 正常系 atena.getYubinNo() が null の場合")
    void getTokugimuInfo_atenaYubinNoがnullの場合はtokuYubinNoが空文字() {
        mockInit();
        Atena a = atena();
        a.setYubinNo(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu()));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, new BigDecimal("1")))
                .thenReturn(Optional.of(a));

        TokugimuJuriTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result.getTokuYubin()).isEqualTo("");
    }

    // ==================================================================
    // #15 getTokugimuInfo 正常系
    // ==================================================================

    @Test
    @DisplayName("#15 getTokugimuInfo 正常系 atena.getJusho() が null の場合")
    void getTokugimuInfo_atenaJushoがnullの場合はtokuJushoが空文字() {
        mockInit();
        Atena a = atena();
        a.setJusho(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu()));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, new BigDecimal("1")))
                .thenReturn(Optional.of(a));

        TokugimuJuriTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result.getTokuJusho()).isEqualTo("");
    }

    // ==================================================================
    // #16 getTokugimuInfo 正常系
    // ==================================================================

    @Test
    @DisplayName("#16 getTokugimuInfo 正常系 tokugimu.getShisetsuYubinNo() が null の場合")
    void getTokugimuInfo_shisetsuYubinNoがnullの場合はshisetsuYubinNoが空文字() {
        mockInit();
        Tokugimu t = tokugimu();
        t.setShisetsuYubinNo(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, new BigDecimal("1")))
                .thenReturn(Optional.of(atena()));

        TokugimuJuriTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result.getShisetsuYubin()).isEqualTo("");
    }

    // ==================================================================
    // #17 getTokugimuInfo 正常系
    // ==================================================================

    @Test
    @DisplayName("#17 getTokugimuInfo 正常系 tokugimu.getShisetsuJusho() が null の場合")
    void getTokugimuInfo_shisetsuJushoがnullの場合はshisetsuJushoが空文字() {
        mockInit();
        Tokugimu t = tokugimu();
        t.setShisetsuJusho(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, new BigDecimal("1")))
                .thenReturn(Optional.of(atena()));

        TokugimuJuriTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result.getShisetsuJusho()).isEqualTo("");
    }

    // ==================================================================
    // #18 getTokugimuInfo 正常系
    // ==================================================================

    @Test
    @DisplayName("#18 getTokugimuInfo 正常系 reportsCommonService.getJichitaiInfo() が null を返す場合")
    void getTokugimuInfo_getJichitaiInfoがnullの場合は例外なく処理が続行される() {
        when(reportsCommonService.getJichitaiInfo()).thenReturn(null);
        when(reportsCommonService.getReportsDefText(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("条例テスト");
        when(reportsCommonService.getReportsDefData(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new byte[]{1, 2, 3});
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu()));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, new BigDecimal("1")))
                .thenReturn(Optional.of(atena()));

        TokugimuJuriTsuchiDto result = service.getTokugimuInfo(SHITEI_NO);

        assertThat(result).isNotNull();
        assertThat(result.getCityName()).isNull();
    }
}
