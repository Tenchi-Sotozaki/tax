package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.NonyushoDataResponse;
import jp.lg.asp.accommodation.dto.NonyushoDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.ReportsDefId;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.service.impl.NonyushoReportsServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NonyushoReportsServiceImplTest {

    @Mock FukaRepository fukaRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock ReportsDefRepository reportsDefRepository;
    @Mock JichitaiContext jichitaiContext;
    @Mock TokugimuService tokugimuService;
    @InjectMocks NonyushoReportsServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "S001";
    private static final String NENDO = "2024";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        when(reportsDefRepository.findById(any(ReportsDefId.class))).thenReturn(Optional.empty());
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
    }

    // ===== getNonyushoData =====

    // No.20 正常系: 賦課データあり・nokigenあり の場合、nokigenをそのまま返す
    @Test
    void getNonyushoData_賦課データあり_nokigenあり_nokigenをそのまま返す() {
        Fuka fuka = buildFuka(10000L, 0L, 0L, 0L, LocalDate.of(2024, 5, 31), null);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of(fuka));

        NonyushoDataResponse result = service.getNonyushoData(SHITEI_NO, NENDO, null);

        assertThat(result.getNokigen()).isEqualTo("2024-05-31");
        assertThat(result.getZeigaku()).isEqualTo("10000");
    }

    // No.21 正常系: 賦課データあり・nokigenなし・shinkokuYmdあり の場合、翌月末を返す
    @Test
    void getNonyushoData_賦課データあり_nokigenなし_shinkokuYmdあり_翌月末を返す() {
        Fuka fuka = buildFuka(10000L, 0L, 0L, 0L, null, LocalDate.of(2024, 4, 10));
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of(fuka));

        NonyushoDataResponse result = service.getNonyushoData(SHITEI_NO, NENDO, null);

        assertThat(result.getNokigen()).isEqualTo("2024-05-31");
    }

    // No.22 正常系: 賦課データあり・nokigenなし・shinkokuYmdなし の場合、nokigenは空文字を返す
    @Test
    void getNonyushoData_賦課データあり_nokigenなし_shinkokuYmdなし_空文字を返す() {
        Fuka fuka = buildFuka(10000L, 0L, 0L, 0L, null, null);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of(fuka));

        NonyushoDataResponse result = service.getNonyushoData(SHITEI_NO, NENDO, null);

        assertThat(result.getNokigen()).isEqualTo("");
    }

    // No.23 正常系: 賦課データあり・加算額あり の場合、加算額の合計を返す
    @Test
    void getNonyushoData_賦課データあり_加算額あり_加算額合計を返す() {
        Fuka fuka = buildFuka(10000L, 1000L, 500L, 200L, LocalDate.of(2024, 5, 31), null);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of(fuka));

        NonyushoDataResponse result = service.getNonyushoData(SHITEI_NO, NENDO, null);

        assertThat(result.getKasan()).isEqualTo("1700");
    }

    // No.24 正常系: 賦課データなし の場合、zeigaku=0・kasan=0・nokigen=空文字を返す
    @Test
    void getNonyushoData_賦課データなし_デフォルト値を返す() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Collections.emptyList());

        NonyushoDataResponse result = service.getNonyushoData(SHITEI_NO, NENDO, null);

        assertThat(result.getZeigaku()).isEqualTo("0");
        assertThat(result.getKasan()).isEqualTo("0");
        assertThat(result.getNokigen()).isEqualTo("");
    }

    // No.25 正常系: shinkokuYmあり の場合、taishoYmで絞り込んだ賦課データを返す
    @Test
    void getNonyushoData_shinkokuYmあり_taishoYmで絞り込んだ賦課データを返す() {
        Fuka fuka = buildFuka(20000L, 0L, 0L, 0L, LocalDate.of(2024, 5, 31), null);
        fuka.setTaishoYm("202404");
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of(fuka));

        NonyushoDataResponse result = service.getNonyushoData(SHITEI_NO, NENDO, "2024-04");

        assertThat(result.getZeigaku()).isEqualTo("20000");
    }

    // No.26 正常系: shinkokuYmあり・対象年月不一致 の場合、zeigaku=0を返す
    @Test
    void getNonyushoData_shinkokuYmあり_対象年月不一致_zeigaku0を返す() {
        Fuka fuka = buildFuka(20000L, 0L, 0L, 0L, LocalDate.of(2024, 5, 31), null);
        fuka.setTaishoYm("202403");
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of(fuka));

        NonyushoDataResponse result = service.getNonyushoData(SHITEI_NO, NENDO, "2024-04");

        assertThat(result.getZeigaku()).isEqualTo("0");
    }

    // No.27 正常系（未実装）: 特例適用中の場合、特例用nokigenを返す
    @Disabled("NonyushoReportsServiceImplに特例適用時のnokigen取得処理が未実装のためスキップ")
    @Test
    void getNonyushoData_特例適用中_特例用nokigenを返す() {
        // 特例適用時のnokigen取得処理が実装されたら有効化する
    }

    // No.28 正常系: 自治体情報あり の場合、cityNameを返す
    @Test
    void getNonyushoData_自治体情報あり_cityNameを返す() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Collections.emptyList());
        Jichitai jichitai = new Jichitai();
        jichitai.setName("札幌市");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        NonyushoDataResponse result = service.getNonyushoData(SHITEI_NO, NENDO, null);

        assertThat(result.getCityName()).isEqualTo("札幌市");
    }

    // No.29 正常系: 自治体情報なし の場合、cityNameは空文字を返す
    @Test
    void getNonyushoData_自治体情報なし_cityNameは空文字を返す() {
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(Collections.emptyList());
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());

        NonyushoDataResponse result = service.getNonyushoData(SHITEI_NO, NENDO, null);

        assertThat(result.getCityName()).isEqualTo("");
    }

    // ===== dataCheck =====

    // No.30 正常系: 賦課データが存在する場合、falseを返す
    @Test
    void dataCheck_賦課データあり_falseを返す() {
        Fuka fuka = buildFuka(10000L, 0L, 0L, 0L, null, null);
        NonyushoDto dto = buildDto(SHITEI_NO, "202404");
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndTaishoYmOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, "202404"))
                .thenReturn(List.of(fuka));

        boolean result = service.dataCheck(dto);

        assertThat(result).isFalse();
    }

    // No.31 正常系: 賦課データが存在しない場合、trueを返す
    @Test
    void dataCheck_賦課データなし_trueを返す() {
        NonyushoDto dto = buildDto(SHITEI_NO, "202404");
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndTaishoYmOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, "202404"))
                .thenReturn(Collections.emptyList());

        boolean result = service.dataCheck(dto);

        assertThat(result).isTrue();
    }

    // No.32 正常系: shinkokuYmdがnullの場合、trueを返す
    @Test
    void dataCheck_shinkokuYmdがnull_trueを返す() {
        NonyushoDto dto = buildDto(SHITEI_NO, null);
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndTaishoYmOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, null))
                .thenReturn(Collections.emptyList());

        boolean result = service.dataCheck(dto);

        assertThat(result).isTrue();
    }

    // ===== helpers =====

    private Fuka buildFuka(Long totalZeigaku, Long kasan1, Long kasan2, Long kasan3,
            LocalDate nokigen, LocalDate shinkokuYmd) {
        Fuka fuka = new Fuka();
        fuka.setJichitaiCd(JICHITAI_CD);
        fuka.setShiteiNo(SHITEI_NO);
        fuka.setNendo(NENDO);
        fuka.setRno(1);
        fuka.setKibetsu(1);
        fuka.setTotalZeigaku(totalZeigaku);
        fuka.setKasanGaku1(kasan1);
        fuka.setKasanGaku2(kasan2);
        fuka.setKasanGaku3(kasan3);
        fuka.setNokigen(nokigen);
        fuka.setShinkokuYmd(shinkokuYmd);
        return fuka;
    }

    private NonyushoDto buildDto(String shiteiNo, String shinkokuYmd) {
        NonyushoDto dto = new NonyushoDto();
        dto.setShiteiNo(shiteiNo);
        dto.setShinkokuYmd(shinkokuYmd);
        dto.setNendo(NENDO);
        return dto;
    }
}
