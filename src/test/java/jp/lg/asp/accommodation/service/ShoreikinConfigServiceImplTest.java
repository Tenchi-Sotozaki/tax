package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.KofuRitsu;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.ShoreikinId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.KofuRitsuRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.ShunoRirekiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.ShoreikinConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
class ShoreikinConfigServiceImplTest {

    @Mock ShoreikinRepository shoreikinRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock FukaRepository fukaRepository;
    @Mock KofuRitsuRepository kofuRitsuRepository;
    @Mock ShunoRirekiRepository shunoRirekiRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks ShoreikinConfigServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";
    private static final String NENDO = "2024";

    @BeforeEach
    void setUp() {
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    @Test
    void getShoreikin_existingRecord_returnsViewMode() {
        Tokugimu t = new Tokugimu();
        t.setShisetsuName("施設");
        t.setAtenaNo(BigDecimal.ONE);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(new Atena()));

        Shoreikin shoreikin = new Shoreikin();
        shoreikin.setKofuGaku(10000L);
        shoreikin.setVersion(1);
        when(shoreikinRepository.findById(new ShoreikinId(JICHITAI_CD, SHITEI_NO, NENDO)))
                .thenReturn(Optional.of(shoreikin));

        ShoreikinConfigDto result = service.getShoreikin(SHITEI_NO, NENDO);

        assertThat(result.getMode()).isEqualTo("view");
        assertThat(result.isExists()).isTrue();
    }

    @Test
    void getShoreikin_noRecord_returnsCreateMode() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(shoreikinRepository.findById(any())).thenReturn(Optional.empty());
        when(kofuRitsuRepository.findKofuRitsuByJichitaiCd(eq(JICHITAI_CD), any(Integer.class)))
                .thenReturn(List.of(BigDecimal.valueOf(10)));

        ShoreikinConfigDto result = service.getShoreikin(SHITEI_NO, NENDO);

        assertThat(result.getMode()).isEqualTo("create");
        assertThat(result.isExists()).isFalse();
    }

    @Test
    void createShoreikin_savesAndReturnsViewMode() {
        ShoreikinConfigDto dto = new ShoreikinConfigDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setNendo(NENDO);
        dto.setKofuGaku(10000L);
        when(shoreikinRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShoreikinConfigDto result = service.createShoreikin(dto);

        assertThat(result.getMode()).isEqualTo("view");
        assertThat(result.isExists()).isTrue();
        assertThat(result.getVersion()).isEqualTo(1);
    }

    @Test
    void updateShoreikin_versionMismatch_throwsException() {
        ShoreikinConfigDto dto = new ShoreikinConfigDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setNendo(NENDO);
        dto.setVersion(1);

        Shoreikin existing = new Shoreikin();
        existing.setVersion(2);
        when(shoreikinRepository.findById(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateShoreikin(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("他のユーザー");
    }

    @Test
    void updateShoreikin_notFound_throwsException() {
        ShoreikinConfigDto dto = new ShoreikinConfigDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setNendo(NENDO);
        when(shoreikinRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateShoreikin(dto))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void calculateKofuZeigaku_sumsTotalZeigaku() {
        Fuka f1 = new Fuka();
        f1.setKibetsu(1);
        f1.setRno(1);
        f1.setTotalZeigaku(5000L);
        f1.setShinkokuYmd(java.time.LocalDate.of(2024, 1, 1));

        Fuka f2 = new Fuka();
        f2.setKibetsu(2);
        f2.setRno(1);
        f2.setTotalZeigaku(3000L);
        f2.setShinkokuYmd(java.time.LocalDate.of(2024, 2, 1));

        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(
                JICHITAI_CD, SHITEI_NO, NENDO, "0", "1"))
                .thenReturn(List.of(f1, f2));
        // 両期別とも納付済み
        when(shunoRirekiRepository.sumNonyugakuByShiteiNoIn(eq(JICHITAI_CD), anyList()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{SHITEI_NO, NENDO, 1, 5000L},
                        new Object[]{SHITEI_NO, NENDO, 2, 3000L}));

        Long result = service.calculateKofuZeigaku(SHITEI_NO, NENDO);

        assertThat(result).isEqualTo(8000L);
    }

    /** 交付率設定のダミー。端数処理を挟まないよう算出単位1・切り捨て・最低額0にする */
    private KofuRitsu kofuRitsu(String ritsu) {
        KofuRitsu k = new KofuRitsu();
        k.setKofuRitsu(new BigDecimal(ritsu));
        k.setSanshutsu(1);
        k.setKbn("1");
        k.setSaiteigaku(BigDecimal.ZERO);
        return k;
    }

    /** 1期・税額10000円・納付済みの賦課をモックに仕込む */
    private void arrangeNofuzumiFuka() {
        Fuka f = new Fuka();
        f.setKibetsu(1);
        f.setRno(1);
        f.setTotalZeigaku(10000L);
        f.setShinkokuYmd(java.time.LocalDate.of(2024, 1, 1));
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(any(), any(), any(), any(), any()))
                .thenReturn(List.of(f));
        when(shunoRirekiRepository.sumNonyugakuByShiteiNoIn(eq(JICHITAI_CD), anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{SHITEI_NO, NENDO, 1, 10000L}));
    }

    private ShoreikinConfigDto configDto(String kofuRitsuOnScreen) {
        ShoreikinConfigDto dto = new ShoreikinConfigDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setNendo(NENDO);
        if (kofuRitsuOnScreen != null) {
            dto.setKofuRitsu(new BigDecimal(kofuRitsuOnScreen));
        }
        return dto;
    }

    @Test
    void calculateShoreikin_calculatesKofuGaku() {
        arrangeNofuzumiFuka();
        when(kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(eq(JICHITAI_CD), any(Integer.class)))
                .thenReturn(List.of(kofuRitsu("10")));

        ShoreikinConfigDto result = service.calculateShoreikin(configDto("10"));

        assertThat(result.getKofuZeigaku()).isEqualTo(10000L);
        assertThat(result.getKofuGaku()).isEqualTo(1000L); // 10000 * 10 / 100
    }

    /**
     * 交付率は画面で入力せず交付率設定から取得するため、
     * 画面から送られてきた値ではなく必ずマスタの値で計算する。
     */
    @Test
    void calculateShoreikin_交付率は画面の値ではなくマスタの値で上書きされる() {
        arrangeNofuzumiFuka();
        when(kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(eq(JICHITAI_CD), any(Integer.class)))
                .thenReturn(List.of(kofuRitsu("10")));

        // 画面からは 99 が送られてきたことにする
        ShoreikinConfigDto result = service.calculateShoreikin(configDto("99"));

        assertThat(result.getKofuRitsu()).isEqualByComparingTo("10");
        assertThat(result.getKofuGaku()).isEqualTo(1000L); // 99 ではなく 10 で計算される
    }

    /** 交付率設定が無ければ算出できないため、エラーにする */
    @Test
    void calculateShoreikin_交付率が取得できない場合は例外になる() {
        when(kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(eq(JICHITAI_CD), any(Integer.class)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.calculateShoreikin(configDto("10")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("交付率が設定されていません");
    }
}
