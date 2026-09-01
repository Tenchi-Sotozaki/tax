package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.ChoshuGenboRepository;
import jp.lg.asp.accommodation.repository.ChoshuGenboUchiRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.NokigenRepository;
import jp.lg.asp.accommodation.repository.ShunoRirekiRepository;
import jp.lg.asp.accommodation.repository.TokureiTekiyoRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.impl.FukaServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FukaServiceImplTest {

    @Mock FukaRepository fukaRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock ZeiritsuRepository zeiritsuRepository;
    @Mock ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
    @Mock ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;
    @Mock FukaUchiRepository fukaUchiRepository;
    @Mock ChoshuGenboRepository choshuGenboRepository;
    @Mock ChoshuGenboUchiRepository choshuGenboUchiRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock TokureiTekiyoRepository tekiyoNozeiShukiRepository;
    @Mock NokigenRepository nokigenRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock ShunoRirekiRepository shunoRirekiRepository;
    @Mock GassanRepository gassanRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks FukaServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ===== calculateTax (No.67-69) =====

    // No.67 定額制の場合、宿泊数×（市税率+県税率）を返す
    @Test
    void calculateTax_定額制_宿泊数に市税率プラス県税率を掛けた値を返す() {
        long result = service.calculateTax("1", 3L, BigDecimal.valueOf(200), BigDecimal.valueOf(100));
        assertThat(result).isEqualTo(900L);
    }

    // No.68 定率制の場合、宿泊料金×税率/100（端数切り捨て）を返す
    @Test
    void calculateTax_定率制_宿泊料金に税率を掛けて100で割り端数切り捨て() {
        long result = service.calculateTax("2", 15000L, BigDecimal.valueOf(2.0), null);
        assertThat(result).isEqualTo(300L);
    }

    // No.69 cityRate・kenRateがnullの場合、0を返す
    @Test
    void calculateTax_cityRateとkenRateがnull_0を返す() {
        long result = service.calculateTax("1", 3L, null, null);
        assertThat(result).isEqualTo(0L);
    }

    @Test
    void calculateTax_teiritsu_calculatesCorrectly() {
        long result = service.calculateTax("2", 10000L, BigDecimal.valueOf(10), null);
        assertThat(result).isEqualTo(1000L);
    }

    @Test
    void calculateTax_teiritsu_truncatesDecimal() {
        long result = service.calculateTax("2", 999L, BigDecimal.valueOf(10), null);
        assertThat(result).isEqualTo(99L);
    }

    @Test
    void calculateTax_teigaku_multipliesRateByCount() {
        long result = service.calculateTax("1", 5L, BigDecimal.valueOf(200), BigDecimal.valueOf(100));
        assertThat(result).isEqualTo(1500L);
    }

    @Test
    void calculateTax_nullRates_treatedAsZero() {
        long result = service.calculateTax("2", 10000L, null, null);
        assertThat(result).isEqualTo(0L);
    }

    // ===== isAlreadyRegistered (No.63-64) =====

    // No.63 申告データが存在する場合、trueを返す
    @Test
    void isAlreadyRegistered_申告データあり_trueを返す() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
        when(fukaRepository.findLatestByNendoAndKibetsu(eq(JICHITAI_CD), eq(SHITEI_NO), any(), anyInt()))
                .thenReturn(List.of(new Fuka()));

        assertThat(service.isAlreadyRegistered(SHITEI_NO, "202404")).isTrue();
    }

    // No.64 申告データが存在しない場合、falseを返す
    @Test
    void isAlreadyRegistered_申告データなし_falseを返す() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
        when(fukaRepository.findLatestByNendoAndKibetsu(eq(JICHITAI_CD), eq(SHITEI_NO), any(), anyInt()))
                .thenReturn(List.of());

        assertThat(service.isAlreadyRegistered(SHITEI_NO, "202404")).isFalse();
    }

    @Test
    void isAlreadyRegistered_existingData_returnsTrue() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, SHITEI_NO, "2023", 12))
                .thenReturn(List.of(new Fuka()));

        assertThat(service.isAlreadyRegistered(SHITEI_NO, "202402")).isTrue();
    }

    @Test
    void isAlreadyRegistered_noData_returnsFalse() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, SHITEI_NO, "2024", 2))
                .thenReturn(List.of());

        assertThat(service.isAlreadyRegistered(SHITEI_NO, "202404")).isFalse();
    }

    // ===== isAlreadyRegisteredByKibetsu (No.65-66) =====

    // No.65 申告データが存在する場合、trueを返す
    @Test
    void isAlreadyRegisteredByKibetsu_申告データあり_trueを返す() {
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, SHITEI_NO, "2024", 1))
                .thenReturn(List.of(new Fuka()));

        assertThat(service.isAlreadyRegisteredByKibetsu(SHITEI_NO, "2024", 1)).isTrue();
    }

    // No.66 申告データが存在しない場合、falseを返す
    @Test
    void isAlreadyRegisteredByKibetsu_申告データなし_falseを返す() {
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, SHITEI_NO, "2024", 1))
                .thenReturn(List.of());

        assertThat(service.isAlreadyRegisteredByKibetsu(SHITEI_NO, "2024", 1)).isFalse();
    }

    @Test
    void isAlreadyRegisteredByKibetsu_existingData_returnsTrue() {
        when(fukaRepository.findLatestByNendoAndKibetsu(JICHITAI_CD, SHITEI_NO, "2024", 3))
                .thenReturn(List.of(new Fuka()));

        assertThat(service.isAlreadyRegisteredByKibetsu(SHITEI_NO, "2024", 3)).isTrue();
    }

    // ===== isGassanTargetMonth (No.70-72) =====

    // No.70 対象日が合算適用期間内の場合、trueを返す
    @Test
    void isGassanTargetMonth_適用期間内_trueを返す() {
        Gassan gassan = new Gassan();
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "901001"))
                .thenReturn(List.of(gassan));

        assertThat(service.isGassanTargetMonth("901001", LocalDate.of(2024, 4, 1))).isTrue();
    }

    // No.71 対象日が合算適用期間外の場合、falseを返す
    @Test
    void isGassanTargetMonth_適用期間外_falseを返す() {
        Gassan gassan = new Gassan();
        gassan.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        gassan.setTekiyoEdYmd(LocalDate.of(2024, 3, 31));
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "901001"))
                .thenReturn(List.of(gassan));

        assertThat(service.isGassanTargetMonth("901001", LocalDate.of(2024, 4, 1))).isFalse();
    }

    // No.72 gassanShiteiNoがnullの場合、falseを返す
    @Test
    void isGassanTargetMonth_gassanShiteiNoがnull_falseを返す() {
        assertThat(service.isGassanTargetMonth(null, LocalDate.of(2024, 4, 1))).isFalse();
    }

    // ===== isShiteiNoGassanTargetMonth (No.73-74) =====

    // No.73 対象日が合算適用期間内の場合、trueを返す
    @Test
    void isShiteiNoGassanTargetMonth_適用期間内_trueを返す() {
        Gassan gassan = new Gassan();
        when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(gassan));

        assertThat(service.isShiteiNoGassanTargetMonth(SHITEI_NO, LocalDate.of(2024, 4, 1))).isTrue();
    }

    // No.74 shiteiNoがnullの場合、falseを返す
    @Test
    void isShiteiNoGassanTargetMonth_shiteiNoがnull_falseを返す() {
        assertThat(service.isShiteiNoGassanTargetMonth(null, LocalDate.of(2024, 4, 1))).isFalse();
    }

    // ===== resolveGassanTekiyoPeriod (No.75-79) =====

    // No.75 開始日のみ設定の場合、"yyyy年M月以降"を返す
    @Test
    void resolveGassanTekiyoPeriod_開始日のみ_yyyy年M月以降を返す() {
        Gassan gassan = new Gassan();
        gassan.setTekiyoStYmd(LocalDate.of(2024, 4, 1));
        when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(gassan));

        String result = service.resolveGassanTekiyoPeriod(SHITEI_NO, LocalDate.of(2024, 4, 1));

        assertThat(result).isEqualTo("2024年4月以降");
    }

    // No.76 終了日のみ設定の場合、"yyyy年M月まで"を返す
    @Test
    void resolveGassanTekiyoPeriod_終了日のみ_yyyy年M月までを返す() {
        Gassan gassan = new Gassan();
        gassan.setTekiyoEdYmd(LocalDate.of(2024, 6, 30));
        when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(gassan));

        String result = service.resolveGassanTekiyoPeriod(SHITEI_NO, LocalDate.of(2024, 4, 1));

        assertThat(result).isEqualTo("2024年6月まで");
    }

    // No.77 開始日・終了日両方設定の場合、"yyyy年M月～yyyy年M月"を返す
    @Test
    void resolveGassanTekiyoPeriod_開始日終了日両方_yyyy年M月からyyyy年M月を返す() {
        Gassan gassan = new Gassan();
        gassan.setTekiyoStYmd(LocalDate.of(2024, 4, 1));
        gassan.setTekiyoEdYmd(LocalDate.of(2024, 6, 30));
        when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(gassan));

        String result = service.resolveGassanTekiyoPeriod(SHITEI_NO, LocalDate.of(2024, 4, 1));

        assertThat(result).isEqualTo("2024年4月～2024年6月");
    }

    // No.78 対象日が期間外の場合、nullを返す
    @Test
    void resolveGassanTekiyoPeriod_対象日が期間外_nullを返す() {
        Gassan gassan = new Gassan();
        gassan.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        gassan.setTekiyoEdYmd(LocalDate.of(2024, 3, 31));
        when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(gassan));

        String result = service.resolveGassanTekiyoPeriod(SHITEI_NO, LocalDate.of(2024, 4, 1));

        assertThat(result).isNull();
    }

    // No.79 shiteiNoがnullの場合、nullを返す
    @Test
    void resolveGassanTekiyoPeriod_shiteiNoがnull_nullを返す() {
        String result = service.resolveGassanTekiyoPeriod(null, LocalDate.of(2024, 4, 1));
        assertThat(result).isNull();
    }

    // ===== getNendoStMonth (No.80-81) =====

    // No.80 自治体情報が存在する場合、nendoStMonthを返す
    @Test
    void getNendoStMonth_自治体情報あり_nendoStMonthを返す() {
        Jichitai jichitai = new Jichitai();
        jichitai.setNendoStMonth("4");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        assertThat(service.getNendoStMonth()).isEqualTo(4);
    }

    // No.81 自治体情報が存在しない場合、デフォルト値3を返す
    @Test
    void getNendoStMonth_自治体情報なし_デフォルト値3を返す() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());

        assertThat(service.getNendoStMonth()).isEqualTo(3);
    }

    // ===== getExistingNendoList (No.82-84) =====

    // No.82 有効な賦課データが存在する場合、年度リストを昇順で返す
    @Test
    void getExistingNendoList_有効データあり_年度リストを昇順で返す() {
        Fuka f1 = new Fuka(); f1.setNendo("2023"); f1.setNewFlg("1"); f1.setDelFlg("0");
        Fuka f2 = new Fuka(); f2.setNendo("2024"); f2.setNewFlg("1"); f2.setDelFlg("0");
        when(fukaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(f2, f1));

        List<Integer> result = service.getExistingNendoList(SHITEI_NO);

        assertThat(result).containsExactly(2023, 2024);
    }

    // No.83 賦課データが存在しない場合、空リストを返す
    @Test
    void getExistingNendoList_データなし_空リストを返す() {
        when(fukaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThat(service.getExistingNendoList(SHITEI_NO)).isEmpty();
    }

    // No.84 newFlg="0"のデータのみの場合、空リストを返す
    @Test
    void getExistingNendoList_newFlgが0のみ_空リストを返す() {
        Fuka f = new Fuka(); f.setNendo("2024"); f.setNewFlg("0"); f.setDelFlg("0");
        when(fukaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(f));

        assertThat(service.getExistingNendoList(SHITEI_NO)).isEmpty();
    }
}
