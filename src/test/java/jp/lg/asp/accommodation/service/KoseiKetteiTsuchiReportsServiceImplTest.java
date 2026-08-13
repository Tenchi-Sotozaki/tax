package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KoseiKetteiTsuchiReportsDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.impl.KoseiKetteiTsuchiReportsServiceImpl;

/**
 * 宿泊税更正・決定通知書（ACCOMMODATION_TAX-358）の単体テスト。
 *
 * 対象月の一覧取得と、画面表示用DTOの組み立てを検証する。
 * generatePdf は JasperReports を実際に動かしているため、そこだけ時間がかかる。
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

    private static final String JICHITAI_CD = "01100";
    private static final String SHITEI_NO = "00100001";

    /**
     * 公開メソッドはいずれも先頭で init() を呼び、自治体情報と法令引用文を読み込む。
     * どのテストでも通る共通の前提としてここで用意する。
     */
    @BeforeEach
    void setUp() {
        Jichitai jichitai = new Jichitai();
        jichitai.setName("札幌市");
        jichitai.setKbnName("北海道");
        when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
        lenient().when(reportsCommonService.getReportsDefText(ReportsConstants.KOSEI_KETTEI_HOREI_INYOU1))
                .thenReturn("法令引用文1");
        lenient().when(reportsCommonService.getReportsDefText(ReportsConstants.KOSEI_KETTEI_HOREI_INYOU2))
                .thenReturn("法令引用文2");
        lenient().when(reportsCommonService.getReportsDefData(ReportsConstants.KOIN))
                .thenReturn(new byte[0]);
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    private Tokugimu tokugimu(String yubinNo, String jusho, String name) {
        Tokugimu t = new Tokugimu();
        t.setJichitaiCd(JICHITAI_CD);
        t.setShiteiNo(SHITEI_NO);
        t.setRno(BigDecimal.ONE);
        t.setShisetsuYubinNo(yubinNo);
        t.setShisetsuJusho(jusho);
        t.setShisetsuName(name);
        t.setAtenaNo(new BigDecimal("1001"));
        return t;
    }

    // ===================================================================
    // findTaishoYmList — 対象月の候補取得
    // ===================================================================

    @Test
    void findTaishoYmList_リポジトリの結果をそのまま返す() {
        when(fukaRepository.findTaishoYmListByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of("202604", "202603", "202602"));

        assertThat(service.findTaishoYmList(SHITEI_NO))
                .containsExactly("202604", "202603", "202602");
    }

    @Test
    void findTaishoYmList_該当が無ければ空リストを返す() {
        when(fukaRepository.findTaishoYmListByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThat(service.findTaishoYmList(SHITEI_NO)).isEmpty();
    }

    @Test
    void findTaishoYmList_セッションの自治体コードで検索する() {
        when(fukaRepository.findTaishoYmListByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        service.findTaishoYmList(SHITEI_NO);

        verify(fukaRepository).findTaishoYmListByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO);
    }

    // ===================================================================
    // buildDtoForDisplay — 画面表示用DTOの組み立て
    // ===================================================================

    @Test
    void buildDtoForDisplay_施設名と郵便番号付きの住所が設定される() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu("060-0002", "札幌市中央区北2条西2丁目", "ホテルA 札幌")));

        KoseiKetteiTsuchiReportsDto dto = service.buildDtoForDisplay(SHITEI_NO);

        assertThat(dto.getShitei_no()).isEqualTo(SHITEI_NO);
        assertThat(dto.getShisetsu_name()).isEqualTo("ホテルA 札幌");
        assertThat(dto.getShisetsu_jusho()).isEqualTo("060-0002 札幌市中央区北2条西2丁目");
    }

    @Test
    void buildDtoForDisplay_郵便番号が無ければ住所だけになる() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu(null, "札幌市中央区北2条西2丁目", "ホテルA 札幌")));

        KoseiKetteiTsuchiReportsDto dto = service.buildDtoForDisplay(SHITEI_NO);

        assertThat(dto.getShisetsu_jusho()).isEqualTo("札幌市中央区北2条西2丁目");
    }

    @Test
    void buildDtoForDisplay_施設名がnullなら空文字になる() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu("060-0002", null, null)));

        KoseiKetteiTsuchiReportsDto dto = service.buildDtoForDisplay(SHITEI_NO);

        assertThat(dto.getShisetsu_name()).isEmpty();
        // 郵便番号のみが残る
        assertThat(dto.getShisetsu_jusho()).isEqualTo("060-0002 ");
    }

    @Test
    void buildDtoForDisplay_特別徴収義務者が見つからなければ指定番号だけ設定される() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.empty());

        KoseiKetteiTsuchiReportsDto dto = service.buildDtoForDisplay(SHITEI_NO);

        assertThat(dto.getShitei_no()).isEqualTo(SHITEI_NO);
        assertThat(dto.getShisetsu_name()).isNull();
        assertThat(dto.getShisetsu_jusho()).isNull();
    }

    @Test
    void buildDtoForDisplay_最新かつ未削除のレコードだけを対象にする() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.empty());

        service.buildDtoForDisplay(SHITEI_NO);

        verify(tokugimuRepository).findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(
                JICHITAI_CD, SHITEI_NO, "1", "0");
    }

    // ===================================================================
    // generatePdf — PDF生成
    // ===================================================================

    /**
     * 対象月を1つも指定しない場合、賦課区分は定額として扱われ、
     * 定額用のJRXMLでPDFが生成される。
     */
    @Test
    void generatePdf_対象月が未指定でも定額様式でPDFが生成される() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        byte[] pdf = service.generatePdf(SHITEI_NO, null, null, null, null);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void generatePdf_施設情報が取得できる場合もPDFが生成される() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tokugimu("060-0002", "札幌市中央区北2条西2丁目", "ホテルA 札幌")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any()))
                .thenReturn(Optional.empty());

        byte[] pdf = service.generatePdf(SHITEI_NO, null, null, null, null);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
