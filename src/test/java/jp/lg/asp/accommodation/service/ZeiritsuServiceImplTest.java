package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.dto.ZeiritsuListItem;
import jp.lg.asp.accommodation.dto.ZeiritsuSearchForm;
import jp.lg.asp.accommodation.entity.Zeiritsu;
import jp.lg.asp.accommodation.repository.ZeiritsuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.impl.ZeiritsuServiceImpl;

@ExtendWith(MockitoExtension.class)
class ZeiritsuServiceImplTest {

    @Mock ZeiritsuRepository zeiritsuRepository;
    @Mock ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
    @Mock ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;

    @InjectMocks ZeiritsuServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    private Zeiritsu zeiritsu(BigDecimal seq, String fukaKbn, String taishoKbn, String stYm, String edYm) {
        Zeiritsu z = new Zeiritsu();
        z.setJichitaiCd(JICHITAI_CD);
        z.setSeq(seq);
        z.setFukaKbn(fukaKbn);
        z.setTaishoKbn(taishoKbn);
        z.setTekiyoStYm(stYm);
        z.setTekiyoEdYm(edYm);
        z.setDelFlg("0");
        return z;
    }

    private ZeiritsuSearchForm form(String fukaKbn, String taishoKbn, String from, String to) {
        ZeiritsuSearchForm f = new ZeiritsuSearchForm();
        f.setFukaKbn(fukaKbn);
        f.setTaishoKbn(taishoKbn);
        f.setTekiyoYmFrom(from);
        f.setTekiyoYmTo(to);
        return f;
    }

    @BeforeEach
    void setUp() {
        when(zeiritsuRepository.findActiveByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(
                zeiritsu(BigDecimal.ONE,       "1", "1", "202301", "202312"),
                zeiritsu(BigDecimal.valueOf(2), "1", "2", "202401", "202412"),
                zeiritsu(BigDecimal.valueOf(3), "2", "1", "202401", "202412"),
                zeiritsu(BigDecimal.valueOf(4), "2", "2", "202501", null)
        ));
    }

    // --- 全件取得 ---

    @Test
    void search_条件なし_全件返却() {
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, null, null, null));
        assertThat(result).hasSize(4);
    }

    // --- fukaKbn 絞り込み ---

    @Test
    void search_fukaKbn定額_一致レコードのみ返却() {
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form("1", null, null, null));
        assertThat(result).hasSize(2).allMatch(i -> i.getFukaKbn().equals("1"));
    }

    @Test
    void search_fukaKbn定率_一致レコードのみ返却() {
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form("2", null, null, null));
        assertThat(result).hasSize(2).allMatch(i -> i.getFukaKbn().equals("2"));
    }

    // --- taishoKbn 絞り込み ---

    @Test
    void search_taishoKbn市区町村_一致レコードのみ返却() {
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, "1", null, null));
        assertThat(result).hasSize(2).allMatch(i -> i.getTaishoKbn().equals("1"));
    }

    @Test
    void search_taishoKbn都道府県_一致レコードのみ返却() {
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, "2", null, null));
        assertThat(result).hasSize(2).allMatch(i -> i.getTaishoKbn().equals("2"));
    }

    // --- tekiyoYmFrom 絞り込み ---

    @Test
    void search_tekiyoYmFromハイフンあり_ハイフン除去後比較しedYmがfromより前のレコードを除外() {
        // seq=1: edYm=202312 < 202401 → 除外
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, null, "2024-01", null));
        assertThat(result).hasSize(3).noneMatch(i -> i.getSeq().equals(BigDecimal.ONE));
    }

    // --- tekiyoYmTo 絞り込み ---

    @Test
    void search_tekiyoYmToハイフンあり_ハイフン除去後比較しstYmがtoより後のレコードを除外() {
        // seq=4: stYm=202501 > 202412 → 除外
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, null, null, "2024-12"));
        assertThat(result).hasSize(3).noneMatch(i -> i.getSeq().equals(BigDecimal.valueOf(4)));
    }

    // --- tekiyoEdYm=null は tekiyoYmFrom フィルタを通過 ---

    @Test
    void search_tekiyoEdYmNull_tekiyoYmFromフィルタを通過() {
        // seq=4: edYm=null → from フィルタをスキップして結果に含まれる
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, null, "2024-01", null));
        assertThat(result).anyMatch(i -> i.getSeq().equals(BigDecimal.valueOf(4)));
    }

    // --- 複合条件 ---

    @Test
    void search_複合条件_全条件合致レコードのみ返却() {
        // fukaKbn=1, taishoKbn=2, from=2024-01, to=2024-12 → seq=2 のみ
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form("1", "2", "2024-01", "2024-12"));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeq()).isEqualTo(BigDecimal.valueOf(2));
    }

    // --- fukaKbnName マッピング ---

    @Test
    void search_fukaKbnName定額マッピング() {
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form("1", null, null, null));
        assertThat(result).allMatch(i -> i.getFukaKbnName().equals("定額"));
    }

    @Test
    void search_fukaKbnName定率マッピング() {
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form("2", null, null, null));
        assertThat(result).allMatch(i -> i.getFukaKbnName().equals("定率"));
    }

    // --- taishoKbnName マッピング ---

    @Test
    void search_taishoKbnName市区町村マッピング() {
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, "1", null, null));
        assertThat(result).allMatch(i -> i.getTaishoKbnName().equals("市区町村"));
    }

    @Test
    void search_taishoKbnName都道府県マッピング() {
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, "2", null, null));
        assertThat(result).allMatch(i -> i.getTaishoKbnName().equals("都道府県"));
    }

    // --- 0件 ---

    @Test
    void search_対象データ0件_空リスト返却() {
        when(zeiritsuRepository.findActiveByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, null, null, null));
        assertThat(result).isEmpty();
    }

    // --- 境界値 ---

    @Test
    void search_tekiyoYmFromとtekiyoEdYmが同値_境界一致で結果に含まれる() {
        // seq=2: edYm=202412, from=2024-12 → edYm >= from なので含まれる
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, null, "2024-12", null));
        assertThat(result).anyMatch(i -> i.getSeq().equals(BigDecimal.valueOf(2)));
    }

    @Test
    void search_tekiyoYmToとtekiyoStYmが同値_境界一致で結果に含まれる() {
        // seq=4: stYm=202501, to=2025-01 → stYm <= to なので含まれる
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, null, null, "2025-01"));
        assertThat(result).anyMatch(i -> i.getSeq().equals(BigDecimal.valueOf(4)));
    }

    @Test
    void search_tekiyoEdYmがtekiyoYmFromの1ヶ月前_境界外で除外() {
        // seq=1: edYm=202312, from=2024-01 → edYm < from なので除外
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, null, "2024-01", null));
        assertThat(result).noneMatch(i -> i.getSeq().equals(BigDecimal.ONE));
    }

    @Test
    void search_tekiyoStYmがtekiyoYmToの1ヶ月後_境界外で除外() {
        // seq=4: stYm=202501, to=2024-12 → stYm > to なので除外
        List<ZeiritsuListItem> result = service.search(JICHITAI_CD, form(null, null, null, "2024-12"));
        assertThat(result).noneMatch(i -> i.getSeq().equals(BigDecimal.valueOf(4)));
    }
}
