package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

/**
 * 特別徴収義務者状況照会（ACCOMMODATION_TAX-361）の Service 単体テスト。
 *
 * DBには接続せず、リポジトリと自治体コンテキストをモックに差し替えて
 * RptStatusServiceImpl のロジックのみを検証する。
 *
 * 検証の中心は次の3点。
 *   1. 帳票マスタの自治体フィルタ（アプリ側で絞っている）
 *   2. 帳票の発行実績がある特別徴収義務者だけを一覧に残すこと
 *   3. 氏名・施設名の一致条件から LIKE パターンを組み立てること
 */
@ExtendWith(MockitoExtension.class)
class RptStatusServiceImplTest {

    @Mock JichitaiContext jichitaiContext;
    @Mock ReportsRepository reportsRepository;
    @Mock RptStatusRepository rptStatusRepository;
    @Mock TokugimuRepository tokugimuRepository;

    @InjectMocks RptStatusServiceImpl service;

    private static final String JICHITAI_CD = "01100";
    private static final String SHITEI_NO = "00100001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ===================================================================
    // テストデータ
    // ===================================================================

    private Reports reports(String jichitaiCd, String rptId) {
        Reports r = new Reports();
        r.setJichitaiCd(jichitaiCd);
        r.setRptId(rptId);
        r.setRptName("帳票" + rptId);
        return r;
    }

    private RptStatus rptStatus(String shiteiNo, String rptId, LocalDateTime createDt) {
        RptStatus s = new RptStatus();
        s.setJichitaiCd(JICHITAI_CD);
        s.setShiteiNo(shiteiNo);
        s.setRptId(rptId);
        s.setCreateDt(createDt);
        return s;
    }

    private Tokugimu tokugimu(String shiteiNo, String atenaName) {
        Tokugimu t = new Tokugimu();
        t.setJichitaiCd(JICHITAI_CD);
        t.setShiteiNo(shiteiNo);
        t.setShisetsuName("ホテルA 札幌");
        t.setAtenaNo(new BigDecimal("1001"));
        if (atenaName != null) {
            Atena a = new Atena();
            a.setName(atenaName);
            t.setAtena(a);
        }
        return t;
    }

    /** 検索条件を素通しで返すようにする */
    private void givenTokugimu(List<Tokugimu> list) {
        when(tokugimuRepository.findBySearchConditions(
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(list);
    }

    private RptStatusSearchForm searchForm() {
        return new RptStatusSearchForm();
    }

    // ===================================================================
    // findAllReports — 帳票マスタ
    // ===================================================================

    @Test
    void findAllReports_自分の自治体の帳票だけを返す() {
        when(reportsRepository.findAll()).thenReturn(List.of(
                reports(JICHITAI_CD, "RPT0000001"),
                reports("01202", "RPT0000002"),
                reports(JICHITAI_CD, "RPT0000003")));

        assertThat(service.findAllReports())
                .extracting(Reports::getRptId)
                .containsExactly("RPT0000001", "RPT0000003");
    }

    @Test
    void findAllReports_該当が無ければ空リストになる() {
        when(reportsRepository.findAll()).thenReturn(List.of(reports("01202", "RPT0000002")));

        assertThat(service.findAllReports()).isEmpty();
    }

    // ===================================================================
    // search — 一覧
    // ===================================================================

    @Test
    void search_発行実績がある特別徴収義務者だけが残る() {
        givenTokugimu(List.of(tokugimu(SHITEI_NO, "株式会社ホテルA"),
                              tokugimu("00100002", "株式会社ホテルB")));
        when(rptStatusRepository.findByJichitaiCd(JICHITAI_CD))
                .thenReturn(List.of(rptStatus(SHITEI_NO, "RPT0000001", LocalDateTime.of(2026, 4, 1, 10, 0))));

        List<RptStatusListItem> items = service.search(searchForm());

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getShiteiNo()).isEqualTo(SHITEI_NO);
    }

    @Test
    void search_帳票IDと発行日時がマップに載る() {
        givenTokugimu(List.of(tokugimu(SHITEI_NO, "株式会社ホテルA")));
        when(rptStatusRepository.findByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(
                rptStatus(SHITEI_NO, "RPT0000001", LocalDateTime.of(2026, 4, 1, 10, 0)),
                rptStatus(SHITEI_NO, "RPT0000002", LocalDateTime.of(2026, 4, 2, 11, 30))));

        RptStatusListItem item = service.search(searchForm()).get(0);

        assertThat(item.getRptStatusMap())
                .containsEntry("RPT0000001", LocalDateTime.of(2026, 4, 1, 10, 0))
                .containsEntry("RPT0000002", LocalDateTime.of(2026, 4, 2, 11, 30));
        assertThat(item.getShisetsuName()).isEqualTo("ホテルA 札幌");
        assertThat(item.getName()).isEqualTo("株式会社ホテルA");
    }

    /** 宛名が未連携でも一覧は落とさず、氏名を空文字にする */
    @Test
    void search_宛名が無ければ氏名は空文字になる() {
        givenTokugimu(List.of(tokugimu(SHITEI_NO, null)));
        when(rptStatusRepository.findByJichitaiCd(JICHITAI_CD))
                .thenReturn(List.of(rptStatus(SHITEI_NO, "RPT0000001", LocalDateTime.of(2026, 4, 1, 10, 0))));

        assertThat(service.search(searchForm()).get(0).getName()).isEmpty();
    }

    @Test
    void search_発行実績が1件も無ければ空リストになる() {
        givenTokugimu(List.of(tokugimu(SHITEI_NO, "株式会社ホテルA")));
        when(rptStatusRepository.findByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());

        assertThat(service.search(searchForm())).isEmpty();
    }

    // ===================================================================
    // search — 検索条件から LIKE パターンへの変換
    // ===================================================================

    @Test
    void search_部分一致なら前後にワイルドカードが付く() {
        givenTokugimu(List.of());
        RptStatusSearchForm form = searchForm();
        form.setName("ホテル");
        form.setNameMatchType("partial");

        service.search(form);

        verify(tokugimuRepository).findBySearchConditions(
                eq(JICHITAI_CD), any(), eq("ホテル"), eq("%ホテル%"), any(), any(), any(), any(), any());
    }

    @Test
    void search_前方一致なら後ろだけワイルドカードが付く() {
        givenTokugimu(List.of());
        RptStatusSearchForm form = searchForm();
        form.setName("ホテル");
        form.setNameMatchType("prefix");

        service.search(form);

        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), any(), eq("ホテル%"), any(), any(), any(), any(), any());
    }

    @Test
    void search_完全一致ならワイルドカードは付かない() {
        givenTokugimu(List.of());
        RptStatusSearchForm form = searchForm();
        form.setName("ホテル");
        form.setNameMatchType("exact");

        service.search(form);

        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), any(), eq("ホテル"), any(), any(), any(), any(), any());
    }

    @Test
    void search_検索値が空ならLIKEパターンはnullになる() {
        givenTokugimu(List.of());
        RptStatusSearchForm form = searchForm();
        form.setName("");
        form.setShisetsuName(null);

        service.search(form);

        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), any(), isNull(), any(), isNull(), any(), any(), any());
    }

    /** 許可種は "999"（全件）で固定されている */
    @Test
    void search_許可種は固定値で渡される() {
        givenTokugimu(List.of());

        service.search(searchForm());

        verify(tokugimuRepository).findBySearchConditions(
                any(), any(), any(), any(), any(), any(), eq("999"), any(), any());
    }
}
