package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.servlet.http.HttpSession;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TokureiTekiyoForm;
import jp.lg.asp.accommodation.dto.TokureiTekiyoHistoryDto;
import jp.lg.asp.accommodation.entity.TokureiTekiyo;
import jp.lg.asp.accommodation.repository.TokureiTekiyoRepository;
import jp.lg.asp.accommodation.service.impl.TokureiTekiyoServiceImpl;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokureiTekiyoServiceImplTest {

    @Mock TokureiTekiyoRepository tekiyoNozeiShukiRepository;
    @Mock JichitaiContext jichitaiContext;
    @Mock HttpSession session;
    @InjectMocks TokureiTekiyoServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(SHITEI_NO);
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(dto);
    }

    private TokureiTekiyo entity(int rno, LocalDate st, LocalDate ed) {
        TokureiTekiyo t = new TokureiTekiyo();
        t.setRno(rno);
        t.setJichitaiCd(JICHITAI_CD);
        t.setShiteiNo(SHITEI_NO);
        t.setTekiyoStYmd(st);
        t.setTekiyoEdYmd(ed);
        t.setDelFlg("0");
        return t;
    }

    // ========== getHistories ==========

    @Test
    void getHistories_1件存在_DTOに変換されて返却() {
        TokureiTekiyo t = entity(1, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 9, 30));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(t));

        List<TokureiTekiyoHistoryDto> result = service.getHistories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIdx()).isEqualTo(1);
        assertThat(result.get(0).getTekiyoStMonth()).isEqualTo("2024年04月");
        assertThat(result.get(0).getTekiyoEdMonth()).isEqualTo("2024年09月");
    }

    @Test
    void getHistories_複数件存在_全件DTOに変換されて返却() {
        TokureiTekiyo t1 = entity(1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31));
        TokureiTekiyo t2 = entity(2, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 9, 30));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(t1, t2));

        List<TokureiTekiyoHistoryDto> result = service.getHistories();

        assertThat(result).hasSize(2);
    }

    @Test
    void getHistories_日付がnull_表示文字列が空文字で返却() {
        TokureiTekiyo t = entity(1, null, null);
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(t));

        List<TokureiTekiyoHistoryDto> result = service.getHistories();

        assertThat(result.get(0).getTekiyoStMonth()).isEmpty();
        assertThat(result.get(0).getTekiyoEdMonth()).isEmpty();
    }

    @Test
    void getHistories_レコードなし_空リストを返す() {
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThat(service.getHistories()).isEmpty();
    }

    @Test
    void getHistories_セッションなし_IllegalStateExceptionをスロー() {
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(null);

        assertThatThrownBy(() -> service.getHistories())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("指定番号がセッションに存在しません。");
    }

    // ========== getForView ==========

    @Test
    void getForView_日付設定済み_フォームに変換されて返却() {
        TokureiTekiyo t = entity(1, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 9, 30));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(t));

        TokureiTekiyoForm form = service.getForView(1);

        assertThat(form.getRno()).isEqualTo(1);
        assertThat(form.getTekiyoStMonth()).isEqualTo("2024-04");
        assertThat(form.getTekiyoEdMonth()).isEqualTo("2024-09");
        assertThat(form.getShiteiNo()).isNull();
        assertThat(form.getObligorName()).isNull();
        assertThat(form.getFacilityName()).isNull();
    }

    @Test
    void getForView_日付がnull_月フィールドがnullのまま返却() {
        TokureiTekiyo t = entity(1, null, null);
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(t));

        TokureiTekiyoForm form = service.getForView(1);

        assertThat(form.getTekiyoStMonth()).isNull();
        assertThat(form.getTekiyoEdMonth()).isNull();
    }

    @Test
    void getForView_存在しないrno_IllegalStateExceptionをスロー() {
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getForView(99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("指定されたレコードが見つかりません。");
    }

    @Test
    void getForView_セッションなし_IllegalStateExceptionをスロー() {
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(null);

        assertThatThrownBy(() -> service.getForView(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("指定番号がセッションに存在しません。");
    }

    // ========== getForRegister ==========

    @Test
    void getForRegister_初期化されたフォームが返却() {
        TokureiTekiyoForm form = service.getForRegister();

        assertThat(form).isNotNull();
        assertThat(form.getRno()).isNull();
        assertThat(form.getTekiyoStMonth()).isNull();
    }

    // ========== save ==========

    @Test
    void save_既存レコードなし_rno1でdelFlg0のエンティティが保存() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-10");
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());
        when(tekiyoNozeiShukiRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(0);
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.save(form);

        verify(tekiyoNozeiShukiRepository).save(argThat(e ->
                e.getRno() == 1 && "0".equals(e.getDelFlg())));
    }

    @Test
    void save_既存レコードあり重複なし_maxRnoプラス1で採番されて保存() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2025-01");
        TokureiTekiyo existing = entity(3, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(existing));
        when(tekiyoNozeiShukiRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(3);
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.save(form);

        verify(tekiyoNozeiShukiRepository).save(argThat(e ->
                e.getRno() == 4 && "0".equals(e.getDelFlg())));
    }

    @Test
    void save_適用開始年月がnull_IllegalStateExceptionをスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth(null);

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用開始年月は必須です。");
    }

    @Test
    void save_開始が終了より後_IllegalStateExceptionをスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-06");
        form.setTekiyoEdMonth("2024-03");

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用開始年月が適用終了年月より後になっています。");
    }

    @Test
    void save_期間重複_IllegalStateExceptionをスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-04");
        form.setTekiyoEdMonth("2024-09");
        TokureiTekiyo existing = entity(1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用期間が既存のレコードと重複しています。");
    }

    @Test
    void save_セッションなし_IllegalStateExceptionをスロー() {
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(null);

        assertThatThrownBy(() -> service.save(new TokureiTekiyoForm()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("指定番号がセッションに存在しません。");
    }

    // ========== update ==========

    @Test
    void update_正常_tekiyoStYmdとtekiyoEdYmdが更新されて保存() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-10");
        form.setTekiyoEdMonth("2025-03");
        TokureiTekiyo entity = entity(1, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 9, 30));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(entity));
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1, form);

        assertThat(entity.getTekiyoStYmd()).isEqualTo(LocalDate.of(2024, 10, 1));
        assertThat(entity.getTekiyoEdYmd()).isEqualTo(LocalDate.of(2025, 3, 31));
        verify(tekiyoNozeiShukiRepository).save(entity);
    }

    @Test
    void update_存在しないrno_IllegalStateExceptionをスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-10");
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.update(99, form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("更新対象のレコードが見つかりません。");
    }

    @Test
    void update_適用開始年月がnull_IllegalStateExceptionをスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth(null);

        assertThatThrownBy(() -> service.update(1, form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用開始年月は必須です。");
    }

    @Test
    void update_開始が終了より後_IllegalStateExceptionをスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-06");
        form.setTekiyoEdMonth("2024-03");

        assertThatThrownBy(() -> service.update(1, form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用開始年月が適用終了年月より後になっています。");
    }

    @Test
    void update_自レコード除外後に重複_IllegalStateExceptionをスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-04");
        form.setTekiyoEdMonth("2024-09");
        TokureiTekiyo self = entity(1, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 9, 30));
        TokureiTekiyo other = entity(2, LocalDate.of(2024, 5, 1), LocalDate.of(2024, 8, 31));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(self, other));

        assertThatThrownBy(() -> service.update(1, form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用期間が既存のレコードと重複しています。");
    }

    @Test
    void update_セッションなし_IllegalStateExceptionをスロー() {
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(null);

        assertThatThrownBy(() -> service.update(1, new TokureiTekiyoForm()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("指定番号がセッションに存在しません。");
    }

    // ========== delete ==========

    @Test
    void delete_正常_delFlgが1に設定されて保存() {
        TokureiTekiyo entity = entity(1, LocalDate.of(2024, 4, 1), null);
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(entity));
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.delete(1);

        assertThat(entity.getDelFlg()).isEqualTo("1");
        verify(tekiyoNozeiShukiRepository).save(entity);
    }

    @Test
    void delete_存在しないrno_IllegalStateExceptionをスロー() {
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.delete(99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("削除対象のレコードが見つかりません。");
    }

    @Test
    void delete_セッションなし_IllegalStateExceptionをスロー() {
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("指定番号がセッションに存在しません。");
    }

    // ========== resolveShiteiNo ==========

    @Test
    void resolveShiteiNo_セッションに指定番号あり_指定番号が返却() {
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> service.getHistories());
        verify(session).getAttribute(SessionHelper.SHITEI_GASSAN_KEY);
    }

    @Test
    void resolveShiteiNo_セッションに指定番号なし_IllegalStateExceptionをスロー() {
        when(session.getAttribute(SessionHelper.SHITEI_GASSAN_KEY)).thenReturn(null);

        assertThatThrownBy(() -> service.getHistories())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("指定番号がセッションに存在しません。");
    }

    // ========== validate ==========

    @Test
    void validate_edYmdがstYmd以降_例外がスローされない() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-04");
        form.setTekiyoEdMonth("2024-09");
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());
        when(tekiyoNozeiShukiRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(0);
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() -> service.save(form));
    }

    @Test
    void validate_stYmdがnull_IllegalStateExceptionをスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth(null);

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用開始年月は必須です。");
    }

    @Test
    void validate_stYmdがedYmdより後_IllegalStateExceptionをスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-09");
        form.setTekiyoEdMonth("2024-04");

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用開始年月が適用終了年月より後になっています。");
    }

    // ========== checkOverlap ==========

    @Test
    void checkOverlap_save経由_既存レコードと重複しない_例外がスローされない() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2025-01");
        form.setTekiyoEdMonth("2025-06");
        TokureiTekiyo existing = entity(1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(existing));
        when(tekiyoNozeiShukiRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(1);
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() -> service.save(form));
    }

    @Test
    void checkOverlap_update経由_自レコードがexcludeRnoで除外され重複しない_例外がスローされない() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-04");
        form.setTekiyoEdMonth("2024-09");
        TokureiTekiyo self = entity(1, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 9, 30));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(self));
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() -> service.update(1, form));
    }

    @Test
    void checkOverlap_save経由_既存レコードと重複_IllegalStateExceptionをスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-04");
        form.setTekiyoEdMonth("2024-09");
        TokureiTekiyo existing = entity(1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用期間が既存のレコードと重複しています。");
    }

    @Test
    void checkOverlap_save経由_終了年月がnull無期限で既存と重複_IllegalStateExceptionをスロー() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-04");
        form.setTekiyoEdMonth(null);
        TokureiTekiyo existing = entity(1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用期間が既存のレコードと重複しています。");
    }

    // ========== toFirstDay ==========

    @Test
    void toFirstDay_yyyy_MM形式_該当月の1日のLocalDateが返却() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-04");
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());
        when(tekiyoNozeiShukiRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(0);
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.save(form);

        verify(tekiyoNozeiShukiRepository).save(argThat(e ->
                LocalDate.of(2024, 4, 1).equals(e.getTekiyoStYmd())));
    }

    @Test
    void toFirstDay_nullまたは空文字_nullが返却() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth(null);

        assertThatThrownBy(() -> service.save(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("適用開始年月は必須です。");
    }

    // ========== toLastDay ==========

    @Test
    void toLastDay_yyyy_MM形式_該当月の末日のLocalDateが返却() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-02");
        form.setTekiyoEdMonth("2024-02");
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());
        when(tekiyoNozeiShukiRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(0);
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.save(form);

        verify(tekiyoNozeiShukiRepository).save(argThat(e ->
                LocalDate.of(2024, 2, 29).equals(e.getTekiyoEdYmd())));
    }

    @Test
    void toLastDay_nullまたは空文字_nullが返却() {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setTekiyoStMonth("2024-04");
        form.setTekiyoEdMonth(null);
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());
        when(tekiyoNozeiShukiRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(0);
        when(tekiyoNozeiShukiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.save(form);

        verify(tekiyoNozeiShukiRepository).save(argThat(e -> e.getTekiyoEdYmd() == null));
    }
}
