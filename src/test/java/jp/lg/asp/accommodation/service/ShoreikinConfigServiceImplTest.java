package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.KofuRitsu;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.ShoreikinId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.KofuRitsuRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.ShunoRirekiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.ShoreikinConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShoreikinConfigServiceImplTest {

    @Mock
    private ShoreikinRepository shoreikinRepository;
    @Mock
    private TokugimuRepository tokugimuRepository;
    @Mock
    private AtenaRepository atenaRepository;
    @Mock
    private FukaRepository fukaRepository;
    @Mock
    private KofuRitsuRepository kofuRitsuRepository;
    @Mock
    private ShunoRirekiRepository shunoRirekiRepository;
    @Mock
    private JichitaiRepository jichitaiRepository;
    @Mock
    private JichitaiContext jichitaiContext;

    @InjectMocks
    private ShoreikinConfigServiceImpl shoreikinConfigService;

    private static final String JICHITAI_CD = "010006";

    @Nested
    @DisplayName("getShoreikin メソッドのテスト")
    class GetShoreikinTest {

        @Test
        @DisplayName("正常系：既存データが存在する場合、viewモードのDTOが返却されること")
        void getShoreikin_existingData_returnsViewMode() {
            when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
            Tokugimu tokugimu = new Tokugimu();
            tokugimu.setShisetsuName("テスト施設");
            tokugimu.setAtenaNo(BigDecimal.ONE);
            when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, "123", "1", "0"))
                    .thenReturn(Optional.of(tokugimu));

            Atena atena = new Atena();
            atena.setName("テスト氏名");
            when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                    .thenReturn(Optional.of(atena));

            Shoreikin shoreikin = new Shoreikin();
            shoreikin.setVersion(1);
            when(shoreikinRepository.findById(new ShoreikinId(JICHITAI_CD, "123", "2026")))
                    .thenReturn(Optional.of(shoreikin));

            ShoreikinConfigDto result = shoreikinConfigService.getShoreikin("123", "2026");

            assertThat(result.isExists()).isTrue();
            assertThat(result.getMode()).isEqualTo("view");
        }

        @Test
        @DisplayName("境界値：nendoが未指定の場合、今年度が自動算出されcreateモードのDTOが返却されること")
        void getShoreikin_nullNendo_resolvesCurrentNendo() {
            when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
            when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                    .thenReturn(Optional.empty());
            Jichitai jichitai = new Jichitai();
            jichitai.setNendoStMonth("4");
            when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
            when(kofuRitsuRepository.findKofuRitsuByJichitaiCd(eq(JICHITAI_CD), anyInt()))
                    .thenReturn(List.of(BigDecimal.valueOf(10.0)));

            ShoreikinConfigDto result = shoreikinConfigService.getShoreikin("123", null);

            assertThat(result.isExists()).isFalse();
            assertThat(result.getMode()).isEqualTo("create");
            assertThat(result.getNendo()).isNotNull();
        }
    }

    @Nested
    @DisplayName("createShoreikin メソッドのテスト")
    class CreateShoreikinTest {

        @Test
        @DisplayName("正常系：新規登録が正常に行われること")
        void createShoreikin_success() {
            when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
            ShoreikinConfigDto dto = new ShoreikinConfigDto();
            dto.setShiteiNo("123");
            dto.setNendo("2026");

            ShoreikinConfigDto result = shoreikinConfigService.createShoreikin(dto);

            assertThat(result.isExists()).isTrue();
            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getMode()).isEqualTo("view");
            verify(shoreikinRepository, times(1)).save(any(Shoreikin.class));
        }
    }

    @Nested
    @DisplayName("updateShoreikin メソッドのテスト")
    class UpdateShoreikinTest {

        @Test
        @DisplayName("正常系：バージョンが一致し正常に更新されること")
        void updateShoreikin_success() {
            when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
            Shoreikin shoreikin = new Shoreikin();
            shoreikin.setVersion(1);
            when(shoreikinRepository.findById(any())).thenReturn(Optional.of(shoreikin));

            ShoreikinConfigDto dto = new ShoreikinConfigDto();
            dto.setShiteiNo("123");
            dto.setNendo("2026");
            dto.setVersion(1);

            ShoreikinConfigDto result = shoreikinConfigService.updateShoreikin(dto);

            assertThat(result.getMode()).isEqualTo("view");
            verify(shoreikinRepository, times(1)).save(shoreikin);
        }

        @Test
        @DisplayName("異常系：更新対象が存在しない場合、例外がスローされること")
        void updateShoreikin_notFound_throwsException() {
            when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
            when(shoreikinRepository.findById(any())).thenReturn(Optional.empty());

            ShoreikinConfigDto dto = new ShoreikinConfigDto();
            dto.setShiteiNo("123");
            dto.setNendo("2026");

            assertThatThrownBy(() -> shoreikinConfigService.updateShoreikin(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("更新対象の交付金情報が見つかりません");
        }

        @Test
        @DisplayName("異常系：バージョンが不一致の場合、楽観的ロック例外がスローされること")
        void updateShoreikin_optimisticLock_throwsException() {
            when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
            Shoreikin shoreikin = new Shoreikin();
            shoreikin.setVersion(2);
            when(shoreikinRepository.findById(any())).thenReturn(Optional.of(shoreikin));

            ShoreikinConfigDto dto = new ShoreikinConfigDto();
            dto.setShiteiNo("123");
            dto.setNendo("2026");
            dto.setVersion(1);

            assertThatThrownBy(() -> shoreikinConfigService.updateShoreikin(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("他のユーザーによって更新されています");
        }
    }

    @Nested
    @DisplayName("calculateShoreikin メソッドのテスト")
    class CalculateShoreikinTest {

        @Test
        @DisplayName("正常系：交付率と税額が取得でき計算結果が設定されること")
        void calculateShoreikin_success() {
            when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
            KofuRitsu kofuRitsu = new KofuRitsu();
            kofuRitsu.setKofuRitsu(BigDecimal.valueOf(5.0));
            kofuRitsu.setSanshutsu(1);
            kofuRitsu.setKbn("1");
            when(kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(eq(JICHITAI_CD), eq(2026)))
                    .thenReturn(List.of(kofuRitsu));
            when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            ShoreikinConfigDto dto = new ShoreikinConfigDto();
            dto.setShiteiNo("123");
            dto.setNendo("2026");

            ShoreikinConfigDto result = shoreikinConfigService.calculateShoreikin(dto);

            assertThat(result.getKofuRitsu()).isEqualByComparingTo("5.0");
        }

        @Test
        @DisplayName("異常系：交付率マスタが存在しない場合、IllegalStateExceptionがスローされること")
        void calculateShoreikin_noRitsu_throwsException() {
            when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
            when(kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(any(), any()))
                    .thenReturn(List.of());

            ShoreikinConfigDto dto = new ShoreikinConfigDto();
            dto.setNendo("2026");

            assertThatThrownBy(() -> shoreikinConfigService.calculateShoreikin(dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("交付率が設定されていません。交付率設定画面で登録してください。");
        }
    }
}