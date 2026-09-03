package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.GassanForm;
import jp.lg.asp.accommodation.dto.GassanForm.FacilityItem;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.GassanServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GassanServiceImplTest {

    @Mock private GassanRepository gassanRepository;
    @Mock private GassanUchiRepository gassanUchiRepository;
    @Mock private AtenaRepository atenaRepository;
    @Mock private TokugimuRepository tokugimuRepository;
    @Mock private JichitaiRepository jichitaiRepository;
    @Mock private JichitaiContext jichitaiContext;

    @InjectMocks
    private GassanServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String GASSAN_SHITEI_NO = "G00001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ヘルパー
    private Tokugimu tokugimuOf(String shiteiNo) {
        Tokugimu t = new Tokugimu();
        t.setJichitaiCd(JICHITAI_CD);
        t.setShiteiNo(shiteiNo);
        t.setShisetsuName("施設" + shiteiNo);
        t.setAtenaNo(BigDecimal.ONE);
        Atena a = new Atena();
        a.setName("テスト宿泊");
        t.setAtena(a);
        return t;
    }

    private GassanUchi gassanUchiOf(String shiteiNo) {
        GassanUchi u = new GassanUchi();
        u.setJichitaiCd(JICHITAI_CD);
        u.setGassanShiteiNo(GASSAN_SHITEI_NO);
        u.setShiteiNo(shiteiNo);
        u.setRno(BigDecimal.ONE);
        return u;
    }

    private Gassan gassanOf(String gassanShiteiNo, BigDecimal rno) {
        Gassan g = new Gassan();
        g.setJichitaiCd(JICHITAI_CD);
        g.setGassanShiteiNo(gassanShiteiNo);
        g.setRno(rno);
        g.setAtenaNo(BigDecimal.ONE);
        g.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        return g;
    }

    private void stubRnoQueries(String gassanShiteiNo) {
        when(gassanRepository.countValidRnoByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, gassanShiteiNo))
                .thenReturn(BigDecimal.valueOf(3));
        when(gassanRepository.findMinRnoByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, gassanShiteiNo))
                .thenReturn(BigDecimal.ONE);
        when(gassanRepository.findPrevRnoByJichitaiCdAndGassanShiteiNo(eq(JICHITAI_CD), eq(gassanShiteiNo), any()))
                .thenReturn(null);
        when(gassanRepository.findNextRnoByJichitaiCdAndGassanShiteiNo(eq(JICHITAI_CD), eq(gassanShiteiNo), any()))
                .thenReturn(BigDecimal.valueOf(2));
        when(gassanRepository.countValidRnoByJichitaiCdAndGassanShiteiNoAndRnoLe(eq(JICHITAI_CD), eq(gassanShiteiNo), any()))
                .thenReturn(BigDecimal.ONE);
    }

    //=====================================================
    // #26 reloadFacilityList
    //=====================================================
    @Test
    @DisplayName("#26 reloadFacilityList 正常系 宛名番号に紐づく施設一覧をフォームに再セット")
    void 確認26_reloadFacilityList_全件unchecked() {
        Tokugimu t1 = tokugimuOf("S001");
        Tokugimu t2 = tokugimuOf("S002");
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(List.of(t1, t2));

        GassanForm form = new GassanForm();
        form.setAtenaNo(BigDecimal.ONE);
        form.setShiteiNoList(null);

        service.reloadFacilityList(form);

        assertThat(form.getFacilityList()).hasSize(2);
        assertThat(form.getFacilityList()).allMatch(f -> !f.isChecked());
    }

    @Test
    @DisplayName("#27 reloadFacilityList 正常系 チェック済み指定番号が checked=true で設定される")
    void 確認27_reloadFacilityList_checkedが設定される() {
        Tokugimu t1 = tokugimuOf("S001");
        Tokugimu t2 = tokugimuOf("S002");
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(List.of(t1, t2));

        GassanForm form = new GassanForm();
        form.setAtenaNo(BigDecimal.ONE);
        form.setShiteiNoList(List.of("S001"));

        service.reloadFacilityList(form);

        FacilityItem s001 = form.getFacilityList().stream()
                .filter(f -> "S001".equals(f.getShiteiNo())).findFirst().orElseThrow();
        FacilityItem s002 = form.getFacilityList().stream()
                .filter(f -> "S002".equals(f.getShiteiNo())).findFirst().orElseThrow();
        assertThat(s001.isChecked()).isTrue();
        assertThat(s002.isChecked()).isFalse();
    }

    //=====================================================
    // #28-#36 getByGassanShiteiNo
    //=====================================================
    @Test
    @DisplayName("#28 getByGassanShiteiNo 正常系 合算指定番号で1件取得")
    void 確認28_getByGassanShiteiNo_基本() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(Collections.emptyList());
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Collections.emptyList());
        Atena atena = new Atena();
        atena.setName("テスト宿泊");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena));
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNo(GASSAN_SHITEI_NO);

        assertThat(result.getGassanShiteiNo()).isEqualTo(GASSAN_SHITEI_NO);
        assertThat(result.getAtenaNo()).isEqualTo(BigDecimal.ONE);
        assertThat(result.getTekiyoStYmd()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(result.getFacilityList()).isEmpty();
    }

    @Test
    @DisplayName("#29 getByGassanShiteiNo 正常系 GassanUchiが存在する場合にcheckedShiteiNosが設定される")
    void 確認29_getByGassanShiteiNo_GassanUchi存在() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(gassanUchiOf("S001"), gassanUchiOf("S002")));
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(List.of(tokugimuOf("S001"), tokugimuOf("S002")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNo(GASSAN_SHITEI_NO);

        assertThat(result.getShiteiNoList()).containsExactly("S001", "S002");
        assertThat(result.getFacilityList()).allMatch(FacilityItem::isChecked);
    }

    @Test
    @DisplayName("#30 getByGassanShiteiNo 正常系 GassanUchiが存在しない場合")
    void 確認30_getByGassanShiteiNo_GassanUchi空() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(Collections.emptyList());
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Collections.emptyList());
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNo(GASSAN_SHITEI_NO);

        assertThat(result.getShiteiNoList()).isEmpty();
        assertThat(result.getDaihyoShiteiNo()).isNull();
    }

    @Test
    @DisplayName("#31 getByGassanShiteiNo 正常系 代表施設がcheckedShiteiNosの先頭に設定される")
    void 確認31_getByGassanShiteiNo_代表施設先頭() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(gassanUchiOf("S001"), gassanUchiOf("S002")));
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(List.of(tokugimuOf("S001"), tokugimuOf("S002")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNo(GASSAN_SHITEI_NO);

        assertThat(result.getDaihyoShiteiNo()).isEqualTo("S001");
        FacilityItem s001 = result.getFacilityList().stream()
                .filter(f -> "S001".equals(f.getShiteiNo())).findFirst().orElseThrow();
        assertThat(s001.isDaihyo()).isTrue();
    }

    @Test
    @DisplayName("#32 getByGassanShiteiNo 正常系 代表施設以外の施設の daihyo が false である")
    void 確認32_getByGassanShiteiNo_非代表施設daihyoFalse() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(gassanUchiOf("S001"), gassanUchiOf("S002")));
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(List.of(tokugimuOf("S001"), tokugimuOf("S002")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNo(GASSAN_SHITEI_NO);

        FacilityItem s002 = result.getFacilityList().stream()
                .filter(f -> "S002".equals(f.getShiteiNo())).findFirst().orElseThrow();
        assertThat(s002.isDaihyo()).isFalse();
    }

    @Test
    @DisplayName("#33 getByGassanShiteiNo 正常系 Atenaが存在する場合にatenaNameが設定される")
    void 確認33_getByGassanShiteiNo_atenaName設定() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(Collections.emptyList());
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Collections.emptyList());
        Atena atena = new Atena();
        atena.setName("テスト宿泊");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena));
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNo(GASSAN_SHITEI_NO);

        assertThat(result.getAtenaName()).isEqualTo("テスト宿泊");
    }

    @Test
    @DisplayName("#34 getByGassanShiteiNo 正常系 Atenaが存在しない場合にatenaNameが空文字になる")
    void 確認34_getByGassanShiteiNo_atenaName空文字() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(Collections.emptyList());
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Collections.emptyList());
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNo(GASSAN_SHITEI_NO);

        assertThat(result.getAtenaName()).isEqualTo("");
    }

    @Test
    @DisplayName("#35 getByGassanShiteiNo 正常系 maxRno・minRno・prevRno・nextRno・currentNo が設定される")
    void 確認35_getByGassanShiteiNo_rno系設定() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(Collections.emptyList());
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Collections.emptyList());
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        when(gassanRepository.countValidRnoByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(BigDecimal.valueOf(3));
        when(gassanRepository.findMinRnoByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(BigDecimal.ONE);
        when(gassanRepository.findPrevRnoByJichitaiCdAndGassanShiteiNo(eq(JICHITAI_CD), eq(GASSAN_SHITEI_NO), any()))
                .thenReturn(null);
        when(gassanRepository.findNextRnoByJichitaiCdAndGassanShiteiNo(eq(JICHITAI_CD), eq(GASSAN_SHITEI_NO), any()))
                .thenReturn(BigDecimal.valueOf(2));
        when(gassanRepository.countValidRnoByJichitaiCdAndGassanShiteiNoAndRnoLe(eq(JICHITAI_CD), eq(GASSAN_SHITEI_NO), any()))
                .thenReturn(BigDecimal.ONE);

        GassanForm result = service.getByGassanShiteiNo(GASSAN_SHITEI_NO);

        assertThat(result.getMaxRno()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(result.getMinRno()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.getPrevRno()).isNull();
        assertThat(result.getNextRno()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(result.getCurrentNo()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("#36 getByGassanShiteiNo 異常系 合算申告が存在しない")
    void 確認36_getByGassanShiteiNo_存在しない() {
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G99999"))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.getByGassanShiteiNo("G99999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("合算申告が見つかりません: G99999");
    }

    //=====================================================
    // #37-#45 getByGassanShiteiNoAndRno
    //=====================================================
    @Test
    @DisplayName("#37 getByGassanShiteiNoAndRno 正常系 合算指定番号・rnoで1件取得（基本）")
    void 確認37_getByGassanShiteiNoAndRno_基本() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        g.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));
        g.setTorokuYmd(LocalDate.of(2024, 1, 1));
        g.setShinkokuYmd(LocalDate.of(2024, 1, 1));
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNoAndRno(JICHITAI_CD, GASSAN_SHITEI_NO, BigDecimal.ONE))
                .thenReturn(Optional.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(Collections.emptyList());
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Collections.emptyList());
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNoAndRno(GASSAN_SHITEI_NO, BigDecimal.ONE);

        assertThat(result.getGassanShiteiNo()).isEqualTo(GASSAN_SHITEI_NO);
        assertThat(result.getRno()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.getTekiyoStYmd()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(result.getTekiyoEdYmd()).isEqualTo(LocalDate.of(2024, 12, 31));
        assertThat(result.getTorokuYmd()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(result.getShinkokuYmd()).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    @Test
    @DisplayName("#38 getByGassanShiteiNoAndRno 正常系 GassanUchiが存在する場合にcheckedShiteiNosが設定される")
    void 確認38_getByGassanShiteiNoAndRno_GassanUchi存在() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNoAndRno(JICHITAI_CD, GASSAN_SHITEI_NO, BigDecimal.ONE))
                .thenReturn(Optional.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(gassanUchiOf("S001"), gassanUchiOf("S002")));
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(List.of(tokugimuOf("S001"), tokugimuOf("S002")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNoAndRno(GASSAN_SHITEI_NO, BigDecimal.ONE);

        assertThat(result.getShiteiNoList()).containsExactly("S001", "S002");
        assertThat(result.getFacilityList()).allMatch(FacilityItem::isChecked);
    }

    @Test
    @DisplayName("#39 getByGassanShiteiNoAndRno 正常系 GassanUchiが存在しない場合")
    void 確認39_getByGassanShiteiNoAndRno_GassanUchi空() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNoAndRno(JICHITAI_CD, GASSAN_SHITEI_NO, BigDecimal.ONE))
                .thenReturn(Optional.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(Collections.emptyList());
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Collections.emptyList());
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNoAndRno(GASSAN_SHITEI_NO, BigDecimal.ONE);

        assertThat(result.getShiteiNoList()).isEmpty();
        assertThat(result.getDaihyoShiteiNo()).isNull();
    }

    @Test
    @DisplayName("#40 getByGassanShiteiNoAndRno 正常系 代表施設がcheckedShiteiNosの先頭に設定される")
    void 確認40_getByGassanShiteiNoAndRno_代表施設先頭() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNoAndRno(JICHITAI_CD, GASSAN_SHITEI_NO, BigDecimal.ONE))
                .thenReturn(Optional.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(gassanUchiOf("S001"), gassanUchiOf("S002")));
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(List.of(tokugimuOf("S001"), tokugimuOf("S002")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNoAndRno(GASSAN_SHITEI_NO, BigDecimal.ONE);

        assertThat(result.getDaihyoShiteiNo()).isEqualTo("S001");
        FacilityItem s001 = result.getFacilityList().stream()
                .filter(f -> "S001".equals(f.getShiteiNo())).findFirst().orElseThrow();
        assertThat(s001.isDaihyo()).isTrue();
    }

    @Test
    @DisplayName("#41 getByGassanShiteiNoAndRno 正常系 代表施設以外の施設の daihyo が false である")
    void 確認41_getByGassanShiteiNoAndRno_非代表施設daihyoFalse() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNoAndRno(JICHITAI_CD, GASSAN_SHITEI_NO, BigDecimal.ONE))
                .thenReturn(Optional.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(gassanUchiOf("S001"), gassanUchiOf("S002")));
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(List.of(tokugimuOf("S001"), tokugimuOf("S002")));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNoAndRno(GASSAN_SHITEI_NO, BigDecimal.ONE);

        FacilityItem s002 = result.getFacilityList().stream()
                .filter(f -> "S002".equals(f.getShiteiNo())).findFirst().orElseThrow();
        assertThat(s002.isDaihyo()).isFalse();
    }

    @Test
    @DisplayName("#42 getByGassanShiteiNoAndRno 正常系 Atenaが存在する場合にatenaNameが設定される")
    void 確認42_getByGassanShiteiNoAndRno_atenaName設定() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNoAndRno(JICHITAI_CD, GASSAN_SHITEI_NO, BigDecimal.ONE))
                .thenReturn(Optional.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(Collections.emptyList());
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Collections.emptyList());
        Atena atena = new Atena();
        atena.setName("テスト宿泊");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena));
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNoAndRno(GASSAN_SHITEI_NO, BigDecimal.ONE);

        assertThat(result.getAtenaName()).isEqualTo("テスト宿泊");
    }

    @Test
    @DisplayName("#43 getByGassanShiteiNoAndRno 正常系 Atenaが存在しない場合にatenaNameが空文字になる")
    void 確認43_getByGassanShiteiNoAndRno_atenaName空文字() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNoAndRno(JICHITAI_CD, GASSAN_SHITEI_NO, BigDecimal.ONE))
                .thenReturn(Optional.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(Collections.emptyList());
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Collections.emptyList());
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        stubRnoQueries(GASSAN_SHITEI_NO);

        GassanForm result = service.getByGassanShiteiNoAndRno(GASSAN_SHITEI_NO, BigDecimal.ONE);

        assertThat(result.getAtenaName()).isEqualTo("");
    }

    @Test
    @DisplayName("#44 getByGassanShiteiNoAndRno 正常系 maxRno・minRno・prevRno・nextRno・currentNo が設定される")
    void 確認44_getByGassanShiteiNoAndRno_rno系設定() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNoAndRno(JICHITAI_CD, GASSAN_SHITEI_NO, BigDecimal.ONE))
                .thenReturn(Optional.of(g));
        when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(Collections.emptyList());
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Collections.emptyList());
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
        when(gassanRepository.countValidRnoByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(BigDecimal.valueOf(3));
        when(gassanRepository.findMinRnoByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(BigDecimal.ONE);
        when(gassanRepository.findPrevRnoByJichitaiCdAndGassanShiteiNo(eq(JICHITAI_CD), eq(GASSAN_SHITEI_NO), any()))
                .thenReturn(null);
        when(gassanRepository.findNextRnoByJichitaiCdAndGassanShiteiNo(eq(JICHITAI_CD), eq(GASSAN_SHITEI_NO), any()))
                .thenReturn(BigDecimal.valueOf(2));
        when(gassanRepository.countValidRnoByJichitaiCdAndGassanShiteiNoAndRnoLe(eq(JICHITAI_CD), eq(GASSAN_SHITEI_NO), any()))
                .thenReturn(BigDecimal.ONE);

        GassanForm result = service.getByGassanShiteiNoAndRno(GASSAN_SHITEI_NO, BigDecimal.ONE);

        assertThat(result.getMaxRno()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(result.getMinRno()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.getPrevRno()).isNull();
        assertThat(result.getNextRno()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(result.getCurrentNo()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("#45 getByGassanShiteiNoAndRno 異常系 合算指定番号・rnoの組み合わせが存在しない")
    void 確認45_getByGassanShiteiNoAndRno_存在しない() {
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNoAndRno(JICHITAI_CD, "G99999", BigDecimal.valueOf(99)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByGassanShiteiNoAndRno("G99999", BigDecimal.valueOf(99)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("合算申告が見つかりません: G99999/rno=99");
    }

    //=====================================================
    // #46-#54 register
    //=====================================================
    @Test
    @DisplayName("#46 register 正常系 新規登録（gassanShiteiNo=null）")
    void 確認46_register_新規登録() {
        GassanForm form = new GassanForm();
        form.setDaihyoShiteiNo("S001");
        form.setShiteiNoList(List.of("S001", "S002"));
        form.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));
        form.setTorokuYmd(LocalDate.of(2024, 1, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 1, 1));
        form.setAtenaNo(BigDecimal.ONE);

        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any(), any()))
                .thenReturn(Collections.emptyList());
        Jichitai jichitai = new Jichitai();
        jichitai.setGassanStChar("900");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
        when(gassanRepository.findMaxGassanShiteiNoByJichitaiCdAndPrefix(JICHITAI_CD, "900"))
                .thenReturn(Optional.of(0));

        String result = service.register(form, null);

        verify(gassanRepository, times(1)).save(any(Gassan.class));
        verify(gassanUchiRepository, times(2)).save(any(GassanUchi.class));
        assertThat(result).isEqualTo("90000001");
    }

    @Test
    @DisplayName("#47 register 正常系 再登録（gassanShiteiNo=非null）")
    void 確認47_register_再登録() {
        GassanForm form = new GassanForm();
        form.setDaihyoShiteiNo("S001");
        form.setShiteiNoList(List.of("S001", "S002"));
        form.setTekiyoStYmd(LocalDate.of(2024, 7, 1));
        form.setTorokuYmd(LocalDate.of(2024, 7, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 7, 1));
        form.setAtenaNo(BigDecimal.ONE);

        Gassan prev = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        prev.setTekiyoEdYmd(LocalDate.of(2024, 6, 30));
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(prev));
        when(gassanRepository.findMaxRnoIncludingDeletedByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(BigDecimal.ONE);

        String result = service.register(form, GASSAN_SHITEI_NO);

        verify(gassanRepository).clearNewFlgByRno(eq(JICHITAI_CD), eq(GASSAN_SHITEI_NO), any());
        verify(gassanRepository, times(1)).save(any(Gassan.class));
        assertThat(result).isEqualTo(GASSAN_SHITEI_NO);
    }

    @Test
    @DisplayName("#48 register 異常系 daihyoShiteiNoがnullかつshiteiNoListがnull")
    void 確認48_register_代表施設null_リストnull() {
        GassanForm form = new GassanForm();
        form.setDaihyoShiteiNo(null);
        form.setShiteiNoList(null);
        form.setTekiyoStYmd(LocalDate.now());

        assertThatThrownBy(() -> service.register(form, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("代表施設を選択してください。");
    }

    @Test
    @DisplayName("#49 register 異常系 daihyoShiteiNoがnullかつshiteiNoListが空")
    void 確認49_register_代表施設null_リスト空() {
        GassanForm form = new GassanForm();
        form.setDaihyoShiteiNo(null);
        form.setShiteiNoList(Collections.emptyList());
        form.setTekiyoStYmd(LocalDate.now());

        assertThatThrownBy(() -> service.register(form, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("代表施設を選択してください。");
    }

    @Test
    @DisplayName("#50 register 異常系 tekiyoEdYmdがtekiyoStYmd以前（同日）")
    void 確認50_register_終了年月が開始年月と同日() {
        GassanForm form = new GassanForm();
        form.setDaihyoShiteiNo("S001");
        form.setShiteiNoList(List.of("S001"));
        form.setTekiyoStYmd(LocalDate.of(2024, 6, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 6, 1));

        assertThatThrownBy(() -> service.register(form, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("適用終了年月は適用開始年月より後の年月を入力してください。");
    }

    @Test
    @DisplayName("#51 register 異常系 tekiyoEdYmdがtekiyoStYmdより前")
    void 確認51_register_終了年月が開始年月より前() {
        GassanForm form = new GassanForm();
        form.setDaihyoShiteiNo("S001");
        form.setShiteiNoList(List.of("S001"));
        form.setTekiyoStYmd(LocalDate.of(2024, 6, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 5, 31));

        assertThatThrownBy(() -> service.register(form, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("適用終了年月は適用開始年月より後の年月を入力してください。");
    }

    @Test
    @DisplayName("#54 register 異常系 再登録時のtekiyoStYmdが前履歴のtekiyoEdYmdより前")
    void 確認54_register_再登録時開始年月が前履歴終了年月以前() {
        GassanForm form = new GassanForm();
        form.setDaihyoShiteiNo("S001");
        form.setShiteiNoList(List.of("S001"));
        form.setTekiyoStYmd(LocalDate.of(2024, 6, 1));
        form.setTorokuYmd(LocalDate.of(2024, 6, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 6, 1));
        form.setAtenaNo(BigDecimal.ONE);

        Gassan prev = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        prev.setTekiyoEdYmd(LocalDate.of(2024, 6, 30));
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(prev));

        assertThatThrownBy(() -> service.register(form, GASSAN_SHITEI_NO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("適用開始年月は前履歴の適用終了年月");
    }

    //=====================================================
    // #55-#58 updateByGassanShiteiNo
    //=====================================================
    @Test
    @DisplayName("#55 updateByGassanShiteiNo 正常系 適用開始・終了年月を更新")
    void 確認55_updateByGassanShiteiNo_正常更新() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        g.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));

        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.of(2024, 2, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));
        form.setTorokuYmd(LocalDate.of(2024, 2, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 2, 1));

        service.updateByGassanShiteiNo(GASSAN_SHITEI_NO, form);

        verify(gassanRepository).save(any(Gassan.class));
        assertThat(g.getTekiyoEdYmd()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test
    @DisplayName("#56 updateByGassanShiteiNo 異常系 合算申告が存在しない")
    void 確認56_updateByGassanShiteiNo_存在しない() {
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "G99999"))
                .thenReturn(Collections.emptyList());

        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));

        assertThatThrownBy(() -> service.updateByGassanShiteiNo("G99999", form))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("#57 updateByGassanShiteiNo 異常系 適用開始・終了年月が逆転")
    void 確認57_updateByGassanShiteiNo_年月逆転() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));

        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.of(2024, 6, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 5, 31));
        form.setTorokuYmd(LocalDate.of(2024, 1, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> service.updateByGassanShiteiNo(GASSAN_SHITEI_NO, form))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("適用終了年月は適用開始年月より後");
    }

    @Test
    @DisplayName("#58 updateByGassanShiteiNo 異常系 rno>1かつ前履歴の終了年月以前の開始年月")
    void 確認58_updateByGassanShiteiNo_前履歴終了年月以前() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.valueOf(2));
        g.setTekiyoStYmd(LocalDate.of(2024, 6, 1));
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));

        Gassan prevGassan = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        prevGassan.setTekiyoEdYmd(LocalDate.of(2024, 6, 30));
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNoAndRno(JICHITAI_CD, GASSAN_SHITEI_NO, BigDecimal.ONE))
                .thenReturn(Optional.of(prevGassan));

        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.of(2024, 6, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));
        form.setTorokuYmd(LocalDate.of(2024, 1, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> service.updateByGassanShiteiNo(GASSAN_SHITEI_NO, form))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("適用開始年月は前履歴の適用終了年月");
    }

    //=====================================================
    // #59-#61 deleteByGassanShiteiNo
    //=====================================================
    @Test
    @DisplayName("#59 deleteByGassanShiteiNo 正常系 最新履歴を論理削除・前履歴のnewFlgを復元")
    void 確認59_deleteByGassanShiteiNo_前履歴あり() {
        Gassan latest = gassanOf(GASSAN_SHITEI_NO, BigDecimal.valueOf(2));
        Gassan prev = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findAllRnoByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(latest, prev));

        service.deleteByGassanShiteiNo(GASSAN_SHITEI_NO);

        verify(gassanRepository).deleteLogicallyByRno(JICHITAI_CD, GASSAN_SHITEI_NO, BigDecimal.valueOf(2));
        verify(gassanRepository).setNewFlgByRno(JICHITAI_CD, GASSAN_SHITEI_NO, BigDecimal.ONE);
    }

    @Test
    @DisplayName("#60 deleteByGassanShiteiNo 正常系 履歴が1件のみの場合")
    void 確認60_deleteByGassanShiteiNo_履歴1件() {
        Gassan latest = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findAllRnoByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(latest));

        service.deleteByGassanShiteiNo(GASSAN_SHITEI_NO);

        verify(gassanRepository).deleteLogicallyByRno(JICHITAI_CD, GASSAN_SHITEI_NO, BigDecimal.ONE);
        verify(gassanRepository, never()).setNewFlgByRno(any(), any(), any());
    }

    @Test
    @DisplayName("#61 deleteByGassanShiteiNo 準正常系 対象レコードが存在しない")
    void 確認61_deleteByGassanShiteiNo_対象なし() {
        when(gassanRepository.findAllRnoByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> service.deleteByGassanShiteiNo(GASSAN_SHITEI_NO));
        verify(gassanRepository, never()).deleteLogicallyByRno(any(), any(), any());
    }

    //=====================================================
    // #62 getFacilitiesByAtenaNo
    //=====================================================
    @Test
    @DisplayName("#62 getFacilitiesByAtenaNo 正常系 宛名番号に紐づく施設一覧を返す")
    void 確認62_getFacilitiesByAtenaNo_正常() {
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(List.of(tokugimuOf("S001"), tokugimuOf("S002")));

        List<FacilityItem> result = service.getFacilitiesByAtenaNo(BigDecimal.ONE);

        verify(tokugimuRepository).findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE);
        assertThat(result).hasSize(2);
    }

    //=====================================================
    // #63-#66 validateNotAlreadyAssigned
    //=====================================================
    @Test
    @DisplayName("#63 validateNotAlreadyAssigned 正常系 未割当の指定番号リスト")
    void 確認63_validateNotAlreadyAssigned_未割当() {
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any(), any()))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> service.validateNotAlreadyAssigned(List.of("S001", "S002"), null));
    }

    @Test
    @DisplayName("#64 validateNotAlreadyAssigned 正常系 指定番号リストがnull")
    void 確認64_validateNotAlreadyAssigned_リストnull() {
        assertDoesNotThrow(() -> service.validateNotAlreadyAssigned(null, null));
    }

    @Test
    @DisplayName("#65 validateNotAlreadyAssigned 正常系 指定番号リストが空")
    void 確認65_validateNotAlreadyAssigned_リスト空() {
        assertDoesNotThrow(() -> service.validateNotAlreadyAssigned(Collections.emptyList(), null));
    }

    @Test
    @DisplayName("#66 validateNotAlreadyAssigned 異常系 既に合算申告に登録済みの指定番号が含まれる")
    void 確認66_validateNotAlreadyAssigned_登録済み() {
        GassanUchi uchi = gassanUchiOf("S001");
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any(), any()))
                .thenReturn(List.of(uchi));

        assertThatThrownBy(() -> service.validateNotAlreadyAssigned(List.of("S001"), null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("以下の指定番号は既に合算申告に登録されています");
    }

    //=====================================================
    // #67-#73 updateByGassanShiteiNo DB例外変換
    //=====================================================
    @Test
    @DisplayName("#67 updateByGassanShiteiNo 異常系 null値制約違反")
    void 確認67_updateByGassanShiteiNo_null制約違反() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanRepository.save(any())).thenAnswer(inv -> { throw new Exception("null value in column violates not-null constraint"); });

        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));
        form.setTorokuYmd(LocalDate.of(2024, 1, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> service.updateByGassanShiteiNo(GASSAN_SHITEI_NO, form))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("必須項目が未入力です。入力内容を確認してください。");
    }

    @Test
    @DisplayName("#68 updateByGassanShiteiNo 異常系 一意制約違反")
    void 確認68_updateByGassanShiteiNo_一意制約違反() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanRepository.save(any())).thenAnswer(inv -> { throw new Exception("duplicate key value violates unique constraint"); });

        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));
        form.setTorokuYmd(LocalDate.of(2024, 1, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> service.updateByGassanShiteiNo(GASSAN_SHITEI_NO, form))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("重複するデータが存在します。入力内容を確認してください。");
    }

    @Test
    @DisplayName("#69 updateByGassanShiteiNo 異常系 外部キー制約違反")
    void 確認69_updateByGassanShiteiNo_外部キー制約違反() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanRepository.save(any())).thenAnswer(inv -> { throw new Exception("foreign key constraint violation"); });

        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));
        form.setTorokuYmd(LocalDate.of(2024, 1, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> service.updateByGassanShiteiNo(GASSAN_SHITEI_NO, form))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("関連するデータが存在しないため、操作できません。");
    }

    @Test
    @DisplayName("#70 updateByGassanShiteiNo 異常系 その他制約違反")
    void 確認70_updateByGassanShiteiNo_その他制約違反() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanRepository.save(any())).thenAnswer(inv -> { throw new Exception("constraint violation occurred"); });

        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));
        form.setTorokuYmd(LocalDate.of(2024, 1, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> service.updateByGassanShiteiNo(GASSAN_SHITEI_NO, form))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("データ制約エラーが発生しました。入力内容を確認してください。");
    }

    @Test
    @DisplayName("#71 updateByGassanShiteiNo 異常系 タイムアウト")
    void 確認71_updateByGassanShiteiNo_タイムアウト() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanRepository.save(any())).thenAnswer(inv -> { throw new Exception("connection timeout exceeded"); });

        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));
        form.setTorokuYmd(LocalDate.of(2024, 1, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> service.updateByGassanShiteiNo(GASSAN_SHITEI_NO, form))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("データベースへのアクセスがタイムアウトしました。しばらく待ってから再度お試しください。");
    }

    @Test
    @DisplayName("#72 updateByGassanShiteiNo 異常系 その他の検査例外")
    void 確認72_updateByGassanShiteiNo_その他例外() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanRepository.save(any())).thenAnswer(inv -> { throw new Exception("some unexpected database error"); });

        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));
        form.setTorokuYmd(LocalDate.of(2024, 1, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> service.updateByGassanShiteiNo(GASSAN_SHITEI_NO, form))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("データベースエラーが発生しました。しばらく待ってから再度お試しください。");
    }

    @Test
    @DisplayName("#73 updateByGassanShiteiNo 異常系 メッセージnullの検査例外")
    void 確認73_updateByGassanShiteiNo_メッセージnull例外() {
        Gassan g = gassanOf(GASSAN_SHITEI_NO, BigDecimal.ONE);
        when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, GASSAN_SHITEI_NO))
                .thenReturn(List.of(g));
        when(gassanRepository.save(any())).thenAnswer(inv -> { throw new Exception((String) null); });

        GassanForm form = new GassanForm();
        form.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        form.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));
        form.setTorokuYmd(LocalDate.of(2024, 1, 1));
        form.setShinkokuYmd(LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> service.updateByGassanShiteiNo(GASSAN_SHITEI_NO, form))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("データベースエラーが発生しました。");
    }
}
