package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.HolidayConfigForm;
import jp.lg.asp.accommodation.entity.Kyugyobi;
import jp.lg.asp.accommodation.repository.HolidayRepository;
import jp.lg.asp.accommodation.service.impl.HolidayConfigServiceImpl;

/**
 * 休業日設定 照会/編集（ACCOMMODATION_TAX-380 / 388）の Service 単体テスト。
 *
 * DBには接続せず、リポジトリと自治体コンテキストをモックに差し替えて
 * HolidayConfigServiceImpl のロジックのみを検証する。
 *
 * このクラスの肝は「番兵レコード」の扱い。
 * 自治体が休業日を1件も登録していない状態と、
 * 「休業日ゼロで登録済み」の状態を区別するために、
 * 日付 0001-01-01 のレコードを1件だけ入れる作りになっている。
 * 自治体コード 99999 は全自治体共通のテンプレート。
 */
@ExtendWith(MockitoExtension.class)
class HolidayConfigServiceImplTest {

    @Mock HolidayRepository holidayRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks HolidayConfigServiceImpl service;

    private static final String JICHITAI_CD = "01100";
    private static final String TEMPLATE_CD = "99999";
    private static final String NEN = "2026";

    @BeforeEach
    void setUp() {
        // getInitialHolidays は自治体コードを見ないため lenient にしておく
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ===================================================================
    // テストデータ
    // ===================================================================

    private Kyugyobi kyugyobi(String jichitaiCd, LocalDate date) {
        Kyugyobi k = new Kyugyobi();
        k.setJichitaiCd(jichitaiCd);
        k.setNen(NEN);
        k.setKyugyobi(date);
        return k;
    }

    /** save() に渡された Kyugyobi をすべて取り出す */
    private List<Kyugyobi> savedEntities() {
        ArgumentCaptor<Kyugyobi> captor = ArgumentCaptor.forClass(Kyugyobi.class);
        verify(holidayRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private HolidayConfigForm form(List<String> holidayDts) {
        HolidayConfigForm f = new HolidayConfigForm();
        f.setNendo(NEN);
        f.setHolidayDts(holidayDts);
        return f;
    }

    // ===================================================================
    // findByNendo — 照会
    // ===================================================================

    @Test
    void findByNendo_登録済みなら自治体自身の休業日を返す() {
        when(holidayRepository.existsByJichitaiCdAndNen(JICHITAI_CD, NEN)).thenReturn(true);
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(JICHITAI_CD, NEN))
                .thenReturn(List.of(kyugyobi(JICHITAI_CD, LocalDate.of(2026, 1, 1)),
                                    kyugyobi(JICHITAI_CD, LocalDate.of(2026, 5, 3))));

        HolidayConfigForm form = service.findByNendo(NEN);

        assertThat(form.getNendo()).isEqualTo(NEN);
        assertThat(form.getHolidayDts()).containsExactly("20260101", "20260503");
        verify(holidayRepository, never()).findByJichitaiCdAndNenOrderByKyugyobi(eq(TEMPLATE_CD), any());
    }

    @Test
    void findByNendo_未登録なら共通テンプレートの休業日を返す() {
        when(holidayRepository.existsByJichitaiCdAndNen(JICHITAI_CD, NEN)).thenReturn(false);
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(TEMPLATE_CD, NEN))
                .thenReturn(List.of(kyugyobi(TEMPLATE_CD, LocalDate.of(2026, 1, 1))));

        HolidayConfigForm form = service.findByNendo(NEN);

        assertThat(form.getHolidayDts()).containsExactly("20260101");
        verify(holidayRepository, never()).findByJichitaiCdAndNenOrderByKyugyobi(eq(JICHITAI_CD), any());
    }

    /** 番兵は「休業日ゼロで登録済み」の目印なので、一覧には出さない */
    @Test
    void findByNendo_番兵レコードは一覧から除外される() {
        when(holidayRepository.existsByJichitaiCdAndNen(JICHITAI_CD, NEN)).thenReturn(true);
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(JICHITAI_CD, NEN))
                .thenReturn(List.of(kyugyobi(JICHITAI_CD, LocalDate.of(1, 1, 1))));

        HolidayConfigForm form = service.findByNendo(NEN);

        assertThat(form.getHolidayDts()).isEmpty();
    }

    @Test
    void findByNendo_該当が無ければ空リストになる() {
        when(holidayRepository.existsByJichitaiCdAndNen(JICHITAI_CD, NEN)).thenReturn(true);
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(JICHITAI_CD, NEN))
                .thenReturn(List.of());

        HolidayConfigForm form = service.findByNendo(NEN);

        assertThat(form.getNendo()).isEqualTo(NEN);
        assertThat(form.getHolidayDts()).isEmpty();
    }

    // ===================================================================
    // save — 更新
    // ===================================================================

    @Test
    void save_既存を削除してから登録する() {
        service.save(form(List.of("20260101")));

        InOrder inOrder = inOrder(holidayRepository);
        inOrder.verify(holidayRepository).deleteByJichitaiCdAndNen(JICHITAI_CD, NEN);
        inOrder.verify(holidayRepository).save(any(Kyugyobi.class));
    }

    @Test
    void save_休業日ぶんのレコードが保存される() {
        service.save(form(List.of("20260101", "20260503")));

        List<Kyugyobi> saved = savedEntities();
        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(k -> {
            assertThat(k.getJichitaiCd()).isEqualTo(JICHITAI_CD);
            assertThat(k.getNen()).isEqualTo(NEN);
        });
        assertThat(saved).extracting(Kyugyobi::getKyugyobi)
                .containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 3));
    }

    /** 休業日ゼロを「登録済み」として残すため、番兵を1件だけ入れる */
    @Test
    void save_休業日が空なら番兵レコードを1件だけ入れる() {
        service.save(form(List.of()));

        List<Kyugyobi> saved = savedEntities();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getKyugyobi()).isEqualTo(LocalDate.of(1, 1, 1));
        assertThat(saved.get(0).getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(saved.get(0).getNen()).isEqualTo(NEN);
    }

    @Test
    void save_休業日がnullでも番兵レコードを入れる() {
        service.save(form(null));

        assertThat(savedEntities()).hasSize(1);
    }

    // ===================================================================
    // findNendoList — 年の選択肢
    // ===================================================================

    @Test
    void findNendoList_自治体に登録があればそれを返す() {
        when(holidayRepository.findDistinctNenByJichitaiCd(JICHITAI_CD))
                .thenReturn(List.of("2025", "2026"));

        assertThat(service.findNendoList()).containsExactly("2025", "2026");
        verify(holidayRepository, never()).findDistinctNenByJichitaiCd(TEMPLATE_CD);
    }

    @Test
    void findNendoList_自治体に登録が無ければ共通テンプレートにフォールバックする() {
        when(holidayRepository.findDistinctNenByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());
        when(holidayRepository.findDistinctNenByJichitaiCd(TEMPLATE_CD)).thenReturn(List.of("2026"));

        assertThat(service.findNendoList()).containsExactly("2026");
    }

    // ===================================================================
    // getInitialHolidays — 初期化ボタン
    // ===================================================================

    @Test
    void getInitialHolidays_自治体に関わらず共通テンプレートから引く() {
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(TEMPLATE_CD, NEN))
                .thenReturn(List.of(kyugyobi(TEMPLATE_CD, LocalDate.of(2026, 1, 1)),
                                    kyugyobi(TEMPLATE_CD, LocalDate.of(2026, 1, 2))));

        assertThat(service.getInitialHolidays(NEN)).containsExactly("20260101", "20260102");
    }
}
