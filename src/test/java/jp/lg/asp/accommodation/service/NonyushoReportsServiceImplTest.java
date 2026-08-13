package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.NonyushoDataResponse;
import jp.lg.asp.accommodation.dto.NonyushoDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.service.impl.NonyushoReportsServiceImpl;

/**
 * 納入書（ACCOMMODATION_TAX-352）の単体テスト。
 *
 * 賦課データからの税額・加算額・納期限の組み立てと、賦課データが無い場合の扱いを検証する。
 * generateNonyushoPdf のみ JasperReports を実際に動かしているため、そこだけ時間がかかる。
 */
@ExtendWith(MockitoExtension.class)
class NonyushoReportsServiceImplTest {

    @Mock TokugimuService tokugimuService;
    @Mock FukaRepository fukaRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock ReportsDefRepository reportsDefRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks NonyushoReportsServiceImpl service;

    private static final String JICHITAI_CD = "01100";
    private static final String SHITEI_NO = "00100001";
    private static final String NENDO = "2026";

    @BeforeEach
    void setUp() {
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        lenient().when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
        lenient().when(reportsDefRepository.findByJichitaiCdAndDefText(any(), any()))
                .thenReturn(Optional.empty());
    }

    // ===================================================================
    // テストデータ
    // ===================================================================

    private Fuka fuka(int rno) {
        Fuka f = new Fuka();
        f.setJichitaiCd(JICHITAI_CD);
        f.setShiteiNo(SHITEI_NO);
        f.setRno(rno);
        f.setNendo(NENDO);
        f.setKibetsu(1);
        f.setTaishoYm("202603");
        f.setHenkoKbn("1");
        f.setTotalZeigaku(50000L);
        f.setKasanGaku1(1000L);
        f.setKasanGaku2(200L);
        f.setKasanGaku3(30L);
        f.setNokigen(LocalDate.of(2026, 4, 30));
        f.setShinkokuYmd(LocalDate.of(2026, 3, 25));
        return f;
    }

    private NonyushoDto dto() {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setNendo(NENDO);
        dto.setShinkokuYmd("202603");
        dto.setCityName("札幌市");
        dto.setKozaNo("1234567");
        dto.setKozaName("サッポロシ");
        dto.setZeigaku("50,000");
        dto.setEntai("0");
        dto.setKasan("1,230");
        dto.setGokei("51,230");
        dto.setNokigen("2026-04-30");
        dto.setTokuJusho("札幌市中央区北1条西1丁目");
        dto.setTokuYubinNo("0600001");
        dto.setTokuName("株式会社ホテルA");
        dto.setNonyuBasho("札幌市役所");
        dto.setShiteiKinyuName("北海道銀行");
        dto.setTorimatome("本店");
        return dto;
    }

    private ReportsDef reportsDef(String value) {
        ReportsDef def = new ReportsDef();
        def.setDefData(value.getBytes(StandardCharsets.UTF_8));
        return def;
    }

    // ===================================================================
    // dataCheck — 賦課データの有無判定
    // ===================================================================

    /** 戻り値は「データが空かどうか」。true = 賦課データ無し。 */
    @Test
    void dataCheck_賦課データが無ければtrueを返す() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndTaishoYmOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "202603")).thenReturn(List.of());

        assertThat(service.dataCheck(dto())).isTrue();
    }

    @Test
    void dataCheck_賦課データがあればfalseを返す() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndTaishoYmOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "202603")).thenReturn(List.of(fuka(1)));

        assertThat(service.dataCheck(dto())).isFalse();
    }

    @Test
    void dataCheck_年度ではなく対象年月で検索する() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndTaishoYmOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "202603")).thenReturn(List.of(fuka(1)));

        service.dataCheck(dto());

        verify(fukaRepository).findByJichitaiCdAndShiteiNoAndTaishoYmOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, "202603");
        verify(fukaRepository, never()).findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                any(), any(), any());
    }

    // ===================================================================
    // getNonyushoData — 画面表示用データの取得
    // ===================================================================

    @Test
    void getNonyushoData_税額と加算額の合計が設定される() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of(fuka(1)));

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, "2026-03");

        assertThat(res.getZeigaku()).isEqualTo("50000");
        // 1000 + 200 + 30
        assertThat(res.getKasan()).isEqualTo("1230");
    }

    @Test
    void getNonyushoData_加算額がnullなら0として合計される() {
        Fuka f = fuka(1);
        f.setKasanGaku1(null);
        f.setKasanGaku2(null);
        f.setKasanGaku3(500L);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of(f));

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, "2026-03");

        assertThat(res.getKasan()).isEqualTo("500");
    }

    @Test
    void getNonyushoData_申告年月のハイフンを除いた対象年月で絞り込む() {
        Fuka match = fuka(1);
        match.setTaishoYm("202603");
        Fuka other = fuka(2);
        other.setTaishoYm("202604");
        other.setTotalZeigaku(99999L);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of(match, other));

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, "2026-03");

        assertThat(res.getZeigaku()).isEqualTo("50000");
    }

    @Test
    void getNonyushoData_申告年月が未指定なら絞り込まず履歴番号が最大のものを採用する() {
        Fuka old = fuka(1);
        Fuka latest = fuka(3);
        latest.setTotalZeigaku(70000L);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of(old, latest));

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, null);

        assertThat(res.getZeigaku()).isEqualTo("70000");
    }

    @Test
    void getNonyushoData_賦課データが無ければ税額0で納期限は空になる() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of());

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, "2026-03");

        assertThat(res.getZeigaku()).isEqualTo("0");
        assertThat(res.getKasan()).isEqualTo("0");
        assertThat(res.getNokigen()).isEmpty();
    }

    @Test
    void getNonyushoData_納期限があればそのまま設定される() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of(fuka(1)));

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, "2026-03");

        assertThat(res.getNokigen()).isEqualTo("2026-04-30");
    }

    @Test
    void getNonyushoData_納期限が無ければ申告日の翌月末が設定される() {
        Fuka f = fuka(1);
        f.setNokigen(null);
        f.setShinkokuYmd(LocalDate.of(2026, 1, 20));
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of(f));

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, "2026-03");

        assertThat(res.getNokigen()).isEqualTo("2026-02-28");
    }

    @Test
    void getNonyushoData_納期限も申告日も無ければ空になる() {
        Fuka f = fuka(1);
        f.setNokigen(null);
        f.setShinkokuYmd(null);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of(f));

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, "2026-03");

        assertThat(res.getNokigen()).isEmpty();
    }

    @Test
    void getNonyushoData_自治体名が設定される() {
        Jichitai jichitai = new Jichitai();
        jichitai.setName("札幌市");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of(fuka(1)));

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, "2026-03");

        assertThat(res.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(res.getCityName()).isEqualTo("札幌市");
    }

    @Test
    void getNonyushoData_自治体が見つからなければ自治体名は空になる() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of(fuka(1)));

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, "2026-03");

        assertThat(res.getCityName()).isEmpty();
    }

    @Test
    void getNonyushoData_帳票定義から口座番号などが設定される() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of(fuka(1)));
        when(reportsDefRepository.findByJichitaiCdAndDefText(JICHITAI_CD, "口座番号"))
                .thenReturn(Optional.of(reportsDef("1234567")));
        when(reportsDefRepository.findByJichitaiCdAndDefText(JICHITAI_CD, "納入場所"))
                .thenReturn(Optional.of(reportsDef("札幌市役所")));
        when(reportsDefRepository.findByJichitaiCdAndDefText(JICHITAI_CD, "指定金融機関名"))
                .thenReturn(Optional.of(reportsDef("北海道銀行")));
        when(reportsDefRepository.findByJichitaiCdAndDefText(JICHITAI_CD, "取りまとめ店"))
                .thenReturn(Optional.of(reportsDef("本店")));

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, "2026-03");

        assertThat(res.getKozaNo()).isEqualTo("1234567");
        assertThat(res.getNonyuBasho()).isEqualTo("札幌市役所");
        assertThat(res.getShiteiKinyuName()).isEqualTo("北海道銀行");
        assertThat(res.getTorimatome()).isEqualTo("本店");
    }

    @Test
    void getNonyushoData_帳票定義が無ければ空文字になる() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of(fuka(1)));

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, "2026-03");

        assertThat(res.getKozaNo()).isEmpty();
        assertThat(res.getNonyuBasho()).isEmpty();
        assertThat(res.getShiteiKinyuName()).isEmpty();
        assertThat(res.getTorimatome()).isEmpty();
    }

    /** 現状の実装は例外を握りつぶしてデフォルト値のレスポンスを返す */
    @Test
    void getNonyushoData_リポジトリが例外を投げてもデフォルト値のレスポンスを返す() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenThrow(new RuntimeException("DB接続エラー"));

        NonyushoDataResponse res = service.getNonyushoData(SHITEI_NO, NENDO, "2026-03");

        assertThat(res.getZeigaku()).isEqualTo("0");
        assertThat(res.getKasan()).isEqualTo("0");
        assertThat(res.getNokigen()).isEmpty();
        assertThat(res.getCityName()).isEmpty();
        assertThat(res.getJichitaiCd()).isEqualTo(JICHITAI_CD);
    }

    // ===================================================================
    // generateNonyushoPdf — PDF生成
    // ===================================================================

    @Test
    void generateNonyushoPdf_賦課情報が無ければ例外を投げる() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of());

        assertThatThrownBy(() -> service.generateNonyushoPdf(dto()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("賦課情報が見つかりません");
    }

    @Test
    void generateNonyushoPdf_賦課情報があればPDFが生成される() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(
                JICHITAI_CD, SHITEI_NO, NENDO)).thenReturn(List.of(fuka(1)));

        byte[] pdf = service.generateNonyushoPdf(dto());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
