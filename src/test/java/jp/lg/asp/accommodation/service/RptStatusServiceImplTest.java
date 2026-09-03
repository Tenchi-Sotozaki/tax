package jp.lg.asp.accommodation.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.RptStatusListItem;
import jp.lg.asp.accommodation.dto.RptStatusSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Reports;
import jp.lg.asp.accommodation.entity.RptStatus;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.ReportsRepository;
import jp.lg.asp.accommodation.repository.RptStatusRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.RptStatusServiceImpl;
import jp.lg.asp.accommodation.util.HashUtil;

@ExtendWith(MockitoExtension.class)
class RptStatusServiceImplTest {

    @InjectMocks
    private RptStatusServiceImpl service;

    @Mock
    private JichitaiContext jichitaiContext;

    @Mock
    private ReportsRepository reportsRepository;

    @Mock
    private RptStatusRepository rptStatusRepository;

    @Mock
    private TokugimuRepository tokugimuRepository;

    @Mock
    private HashUtil hashUtil;

    // =====================================================================
    // ヘルパー
    // =====================================================================

    private Reports reports(String rptId) {
        Reports r = new Reports();
        r.setRptId(rptId);
        return r;
    }

    private RptStatus rptStatus(String shiteiNo, String rptId, LocalDateTime createDt) {
        RptStatus s = new RptStatus();
        s.setShiteiNo(shiteiNo);
        s.setRptId(rptId);
        s.setCreateDt(createDt);
        return s;
    }

    private Tokugimu tokugimu(String shiteiNo, String shisetsuName, Atena atena) {
        Tokugimu t = new Tokugimu();
        t.setShiteiNo(shiteiNo);
        t.setShisetsuName(shisetsuName);
        t.setAtena(atena);
        return t;
    }

    private Atena atena(String name) {
        Atena a = new Atena();
        a.setName(name);
        return a;
    }

    private RptStatusSearchForm emptyForm() {
        return new RptStatusSearchForm();
    }

    // =====================================================================
    // #8 findAllReports 正常系
    // =====================================================================

    @Test
    @DisplayName("#8 findAllReports 正常系 帳票マスタの全件を返す")
    void findAllReports_帳票マスタの全件を返す() {
        when(reportsRepository.findAll()).thenReturn(List.of(
                reports("R001"),
                reports("R002"),
                reports("R003")));

        List<Reports> result = service.findAllReports();

        assertEquals(3, result.size());
        assertEquals("R001", result.get(0).getRptId());
        assertEquals("R003", result.get(2).getRptId());
    }

    // =====================================================================
    // #9 findAllReports 正常系
    // =====================================================================

    @Test
    @DisplayName("#9 findAllReports 正常系 2件返る場合")
    void findAllReports_2件返る() {
        when(reportsRepository.findAll()).thenReturn(List.of(
                reports("R001"),
                reports("R002")));

        List<Reports> result = service.findAllReports();

        assertEquals(2, result.size());
        assertEquals("R001", result.get(0).getRptId());
        assertEquals("R002", result.get(1).getRptId());
    }

    // =====================================================================
    // #10 findAllReports 異常系
    // =====================================================================

    @Test
    @DisplayName("#10 findAllReports 異常系 帳票マスタが0件の場合")
    void findAllReports_帳票マスタが0件() {
        when(reportsRepository.findAll()).thenReturn(List.of());

        List<Reports> result = service.findAllReports();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // =====================================================================
    // #12 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#12 search 正常系 検索条件に合致した特別徴収義務者と発行日時が組み立てられる")
    void search_検索結果1件と発行日時が組み立てられる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(tokugimu("S001", "ホテルA", atena("山田太郎"))));
        when(rptStatusRepository.findByJichitaiCd("01100"))
                .thenReturn(List.of(rptStatus("S001", "R001", LocalDateTime.of(2026, 4, 1, 10, 0))));

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setShiteiNo("S001");
        form.setNameMatchType("partial");
        form.setShisetsuNameMatchType("partial");

        List<RptStatusListItem> result = service.search(form);

        assertEquals(1, result.size());
        assertEquals("S001", result.get(0).getShiteiNo());
        assertEquals("山田太郎", result.get(0).getName());
        assertEquals("ホテルA", result.get(0).getShisetsuName());
        assertEquals(LocalDateTime.of(2026, 4, 1, 10, 0), result.get(0).getRptStatusMap().get("R001"));
    }

    // =====================================================================
    // #13 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#13 search 正常系 1件の義務者に複数帳票の発行実績がある場合")
    void search_1件の義務者に複数帳票の発行実績() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(tokugimu("S001", "ホテルA", atena("山田太郎"))));
        when(rptStatusRepository.findByJichitaiCd("01100"))
                .thenReturn(List.of(
                        rptStatus("S001", "R001", LocalDateTime.of(2026, 4, 1, 10, 0)),
                        rptStatus("S001", "R002", LocalDateTime.of(2026, 4, 2, 11, 0))));

        List<RptStatusListItem> result = service.search(emptyForm());

        assertEquals(2, result.get(0).getRptStatusMap().size());
        assertEquals(LocalDateTime.of(2026, 4, 1, 10, 0), result.get(0).getRptStatusMap().get("R001"));
        assertEquals(LocalDateTime.of(2026, 4, 2, 11, 0), result.get(0).getRptStatusMap().get("R002"));
    }

    // =====================================================================
    // #14 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#14 search 正常系 発行実績が無い義務者は空のマップになる")
    void search_発行実績が無い義務者は空マップ() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(tokugimu("S001", "ホテルA", atena("山田太郎"))));
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        List<RptStatusListItem> result = service.search(emptyForm());

        assertEquals(1, result.size());
        assertNotNull(result.get(0).getRptStatusMap());
        assertTrue(result.get(0).getRptStatusMap().isEmpty());
    }

    // =====================================================================
    // #15 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#15 search 正常系 発行状況に該当義務者以外のデータが混ざっている場合")
    void search_他義務者の発行実績が混入しない() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(tokugimu("S001", "ホテルA", atena("山田太郎"))));
        when(rptStatusRepository.findByJichitaiCd("01100"))
                .thenReturn(List.of(rptStatus("S002", "R001", LocalDateTime.of(2026, 4, 1, 10, 0))));

        List<RptStatusListItem> result = service.search(emptyForm());

        assertEquals(1, result.size());
        assertTrue(result.get(0).getRptStatusMap().isEmpty());
    }

    // =====================================================================
    // #16 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#16 search 異常系 宛名が紐づいていない場合")
    void search_宛名がnullの場合はname空文字() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(tokugimu("S001", "ホテルA", null)));
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        List<RptStatusListItem> result = service.search(emptyForm());

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getName());
    }

    // =====================================================================
    // #17 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#17 search 異常系 該当する特別徴収義務者が0件の場合")
    void search_義務者が0件の場合は空リスト() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(rptStatusRepository.findByJichitaiCd("01100"))
                .thenReturn(List.of(rptStatus("S001", "R001", LocalDateTime.of(2026, 4, 1, 10, 0))));

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setShiteiNo("X999");

        List<RptStatusListItem> result = service.search(form);

        assertTrue(result.isEmpty());
    }

    // =====================================================================
    // #18 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#18 search 正常系 複数件の並び順が検索結果のとおり保たれる")
    void search_並び順が保たれる() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(
                        tokugimu("S001", "ホテルA", atena("山田太郎")),
                        tokugimu("S002", "ホテルB", atena("佐藤花子"))));
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        List<RptStatusListItem> result = service.search(emptyForm());

        assertEquals(2, result.size());
        assertEquals("S001", result.get(0).getShiteiNo());
        assertEquals("S002", result.get(1).getShiteiNo());
    }

    // =====================================================================
    // #19 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#19 search 正常系 個人番号を指定した場合はハッシュ化して検索される")
    void search_個人番号はハッシュ化して検索される() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(hashUtil.sha256("123456789012")).thenReturn("abc123");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        ArgumentCaptor<String> kojinNoCaptor = ArgumentCaptor.forClass(String.class);

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setKojinNo("123456789012");

        service.search(form);

        verify(hashUtil, times(1)).sha256("123456789012");
        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), any(), any(), any(), any(), any(), kojinNoCaptor.capture(), any());
        assertEquals("abc123", kojinNoCaptor.getValue());
    }

    // =====================================================================
    // #20 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#20 search 異常系 個人番号が null の場合はハッシュ化しない")
    void search_個人番号がnullの場合はハッシュ化しない() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setKojinNo(null);

        service.search(form);

        verify(hashUtil, never()).sha256(any());
    }

    @Test
    @DisplayName("#20 search 異常系 個人番号が空文字の場合はハッシュ化しない")
    void search_個人番号が空文字の場合はハッシュ化しない() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setKojinNo("");

        service.search(form);

        verify(hashUtil, never()).sha256(any());
    }

    // =====================================================================
    // #21 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#21 search 正常系 許可種別には固定値 \"999\" が渡る")
    void search_許可種別に固定値999が渡る() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        ArgumentCaptor<String> kyokaShuCaptor = ArgumentCaptor.forClass(String.class);

        service.search(emptyForm());

        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), any(), any(), any(), any(), kyokaShuCaptor.capture(), any(), any());
        assertEquals("999", kyokaShuCaptor.getValue());
    }

    // =====================================================================
    // #22 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#22 search 正常系 氏名の一致区分 partial：LIKE が \"%山田%\" になる")
    void search_氏名一致区分partial() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        ArgumentCaptor<String> namePatternCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setName("山田");
        form.setNameMatchType("partial");

        service.search(form);

        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), nameCaptor.capture(), namePatternCaptor.capture(), any(), any(), any(), any(), any());
        assertEquals("%山田%", namePatternCaptor.getValue());
        assertEquals("山田", nameCaptor.getValue());
    }

    // =====================================================================
    // #23 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#23 search 正常系 氏名の一致区分 prefix：LIKE が \"山田%\" になる")
    void search_氏名一致区分prefix() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        ArgumentCaptor<String> namePatternCaptor = ArgumentCaptor.forClass(String.class);

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setName("山田");
        form.setNameMatchType("prefix");

        service.search(form);

        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), any(), namePatternCaptor.capture(), any(), any(), any(), any(), any());
        assertEquals("山田%", namePatternCaptor.getValue());
    }

    // =====================================================================
    // #24 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#24 search 正常系 氏名の一致区分 exact：LIKE が \"山田\" になる")
    void search_氏名一致区分exact() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        ArgumentCaptor<String> namePatternCaptor = ArgumentCaptor.forClass(String.class);

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setName("山田");
        form.setNameMatchType("exact");

        service.search(form);

        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), any(), namePatternCaptor.capture(), any(), any(), any(), any(), any());
        assertEquals("山田", namePatternCaptor.getValue());
    }

    // =====================================================================
    // #25 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#25 search 異常系 氏名が null の場合はパターンが null になる")
    void search_氏名がnullの場合はパターンがnull() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        ArgumentCaptor<String> namePatternCaptor = ArgumentCaptor.forClass(String.class);

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setName(null);

        service.search(form);

        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), any(), namePatternCaptor.capture(), any(), any(), any(), any(), any());
        assertNull(namePatternCaptor.getValue());
    }

    @Test
    @DisplayName("#25 search 異常系 氏名が空文字の場合はパターンが null になる")
    void search_氏名が空文字の場合はパターンがnull() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        ArgumentCaptor<String> namePatternCaptor = ArgumentCaptor.forClass(String.class);

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setName("");

        service.search(form);

        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), any(), namePatternCaptor.capture(), any(), any(), any(), any(), any());
        assertNull(namePatternCaptor.getValue());
    }

    // =====================================================================
    // #26 search 正常系
    // =====================================================================

    @Test
    @DisplayName("#26 search 正常系 施設名の一致区分も同様に反映される")
    void search_施設名一致区分prefix() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        ArgumentCaptor<String> shisetsuNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> shisetsuNamePatternCaptor = ArgumentCaptor.forClass(String.class);

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setShisetsuName("ホテル");
        form.setShisetsuNameMatchType("prefix");

        service.search(form);

        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), any(), any(), shisetsuNameCaptor.capture(), shisetsuNamePatternCaptor.capture(), any(), any(), any());
        assertEquals("ホテル%", shisetsuNamePatternCaptor.getValue());
        assertEquals("ホテル", shisetsuNameCaptor.getValue());
    }

    // =====================================================================
    // #27 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#27 search 異常系 氏名の一致区分が null の場合は部分一致として扱われる")
    void search_氏名一致区分がnullの場合は部分一致() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(tokugimuRepository.findBySearchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(rptStatusRepository.findByJichitaiCd("01100")).thenReturn(List.of());

        ArgumentCaptor<String> namePatternCaptor = ArgumentCaptor.forClass(String.class);

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setName("山田");
        form.setNameMatchType(null);

        service.search(form);

        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), any(), namePatternCaptor.capture(), any(), any(), any(), any(), any());
        assertEquals("%山田%", namePatternCaptor.getValue());
    }

    // =====================================================================
    // #28 search 異常系
    // =====================================================================

    @Test
    @DisplayName("#28 search 異常系 ハッシュ化に失敗した場合は例外が伝播する")
    void search_ハッシュ化に失敗した場合は例外が伝播する() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("01100");
        when(hashUtil.sha256("123456789012")).thenThrow(new RuntimeException("ハッシュ化に失敗しました"));

        RptStatusSearchForm form = new RptStatusSearchForm();
        form.setKojinNo("123456789012");

        assertThrows(RuntimeException.class, () -> service.search(form));

        verify(tokugimuRepository, never()).findBySearchConditions(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
