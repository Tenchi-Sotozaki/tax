package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


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
import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.entity.NokigenId;
import jp.lg.asp.accommodation.entity.TokureiTekiyo;
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
    private static final String NENDO = "2024";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // -----------------------------------------------------------------------
    // ヘルパー
    // -----------------------------------------------------------------------

    private Jichitai jichitaiWith(String nendoStMonth, String nozeiShuki) {
        Jichitai j = new Jichitai();
        j.setNendoStMonth(nendoStMonth);
        j.setNozeiShuki(nozeiShuki);
        return j;
    }

    private Nokigen nokigenWith(String v1, String v2, String v3, String v4, String v5, String v6,
                                String v7, String v8, String v9, String v10, String v11, String v12) {
        Nokigen n = new Nokigen();
        n.setNokigen1st(v1);   n.setNokigen2nd(v2);   n.setNokigen3rd(v3);
        n.setNokigen4th(v4);   n.setNokigen5th(v5);   n.setNokigen6th(v6);
        n.setNokigen7th(v7);   n.setNokigen8th(v8);   n.setNokigen9th(v9);
        n.setNokigen10th(v10); n.setNokigen11th(v11); n.setNokigen12th(v12);
        return n;
    }

    private Nokigen fullNokigen() {
        return nokigenWith(
            "20240430", "20240531", "20240630",
            "20240731", "20240831", "20240930",
            "20241031", "20241130", "20241231",
            "20250131", "20250228", "20250331");
    }

    private void stubDaichoBase(String nendoStMonth, String nozeiShuki) {
        when(jichitaiRepository.findById(JICHITAI_CD))
                .thenReturn(Optional.of(jichitaiWith(nendoStMonth, nozeiShuki)));
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of());
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, NENDO)))
                .thenReturn(Optional.empty());
        when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());
    }

    private Fuka fukaAt(String taishoYm, int rno, long totalZeigaku) {
        Fuka f = new Fuka();
        f.setTaishoYm(taishoYm);
        f.setRno(rno);
        f.setNendo(NENDO);
        f.setKibetsu(1);
        f.setTotalZeigaku(totalZeigaku);
        return f;
    }

    // -----------------------------------------------------------------------
    // getDaichoData
    // -----------------------------------------------------------------------

    // TC-01: 自治体マスタに年度開始月が未設定 → デフォルト値3が使われてitemsが正常に生成される
    @Test
    void getDaichoData_年度開始月未設定_デフォルト値3が使われItemsが正常に生成される() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of());
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, NENDO)))
                .thenReturn(Optional.empty());
        when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        // nendoStMonth=3(デフォルト): kibetsu1=3月
        assertThat(result.getItems()).hasSize(12);
        assertThat(result.getItems().get(0).getDisplayNengetsu()).contains("3月");
    }

    // TC-02: 同一taishoYmにrnoが異なる複数あり → rnoが大きい方のデータが採用される
    @Test
    void getDaichoData_createFukaMap_同一taishoYmに複数rno_大きい方が採用される() {
        Fuka f1 = fukaAt("202403", 1, 1000L);
        Fuka f2 = fukaAt("202403", 2, 5000L);
        stubDaichoBase("3", "1");
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of(f1, f2));
        when(shunoRirekiRepository.sumNonyugaku(eq(JICHITAI_CD), eq(SHITEI_NO), any(), any()))
                .thenReturn(0L);

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        assertThat(result.getItems().get(0).isShinkokuZumi()).isTrue();
        assertThat(result.getItems().get(0).getTotalZeigaku()).isEqualTo(5000L);
    }

    // TC-03: 該当年度の賦課データなし → 12件のitemsが返り、全件shinkokuZumi=false
    @Test
    void getDaichoData_buildDaichoItem_賦課データなし_12件全てshinkokuZumiFalse() {
        stubDaichoBase("3", "1");

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        assertThat(result.getItems()).hasSize(12);
        assertThat(result.getItems()).allMatch(item -> !item.isShinkokuZumi());
    }

    // TC-04: 該当年度の賦課データあり → 対応するitemsのshinkokuZumi=true
    @Test
    void getDaichoData_buildDaichoItem_賦課データあり_shinkokuZumiTrue() {
        stubDaichoBase("3", "1");
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of(fukaAt("202403", 1, 1000L)));
        when(shunoRirekiRepository.sumNonyugaku(eq(JICHITAI_CD), eq(SHITEI_NO), any(), any()))
                .thenReturn(0L);

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        assertThat(result.getItems().get(0).isShinkokuZumi()).isTrue();
    }

    // TC-05: 申告済みで納入金額が賦課税額以上 → nonyuStatus="paid"
    @Test
    void getDaichoData_buildDaichoItem_納入金額が賦課税額以上_nonyuStatusPaid() {
        stubDaichoBase("3", "1");
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of(fukaAt("202403", 1, 1000L)));
        when(shunoRirekiRepository.sumNonyugaku(JICHITAI_CD, SHITEI_NO, NENDO, 1))
                .thenReturn(1000L);

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        assertThat(result.getItems().get(0).getNonyuStatus()).isEqualTo("paid");
    }

    // TC-06: 申告済みで納入金額が賦課税額より少なく0より大きい → nonyuStatus="partial"
    @Test
    void getDaichoData_buildDaichoItem_納入金額が賦課税額未満かつ0超_nonyuStatusPartial() {
        stubDaichoBase("3", "1");
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of(fukaAt("202403", 1, 1000L)));
        when(shunoRirekiRepository.sumNonyugaku(JICHITAI_CD, SHITEI_NO, NENDO, 1))
                .thenReturn(500L);

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        assertThat(result.getItems().get(0).getNonyuStatus()).isEqualTo("partial");
    }

    // TC-07: 申告済みで納入金額が0 → nonyuStatus="unpaid"
    @Test
    void getDaichoData_buildDaichoItem_納入金額0_nonyuStatusUnpaid() {
        stubDaichoBase("3", "1");
        when(fukaRepository.findByJichitaiCdAndShiteiNoAndNendoOrderByKibetsuAsc(JICHITAI_CD, SHITEI_NO, NENDO))
                .thenReturn(List.of(fukaAt("202403", 1, 1000L)));
        when(shunoRirekiRepository.sumNonyugaku(JICHITAI_CD, SHITEI_NO, NENDO, 1))
                .thenReturn(0L);

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        assertThat(result.getItems().get(0).getNonyuStatus()).isEqualTo("unpaid");
    }

    // TC-08: defaultShukiが四半期 → tekiyoListの内容に関わらずshuki=3として3期ごとの納入期限がセット
    @Test
    void getDaichoData_resolveShuki_defaultShukiが四半期_tekiyoListに関わらずshuki3() {
        stubDaichoBase("3", "3");
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, NENDO)))
                .thenReturn(Optional.of(fullNokigen()));

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        // shuki=3: kibetsu1,2,3 → ordinal=3 → nokigen3rd="20240630"
        assertThat(result.getItems().get(0).getDisplayKigen()).isEqualTo("2024年6月30日");
        assertThat(result.getItems().get(1).getDisplayKigen()).isEqualTo("2024年6月30日");
        assertThat(result.getItems().get(2).getDisplayKigen()).isEqualTo("2024年6月30日");
        // kibetsu4,5,6 → ordinal=6 → nokigen6th="20240930"
        assertThat(result.getItems().get(3).getDisplayKigen()).isEqualTo("2024年9月30日");
    }

    // TC-09: tekiyoListが空かつdefaultShukiが月次 → shuki=1として各期別の納入期限がセット
    @Test
    void getDaichoData_resolveShuki_tekiyoList空かつdefaultShuki月次_shuki1() {
        stubDaichoBase("3", "1");
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, NENDO)))
                .thenReturn(Optional.of(fullNokigen()));

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        // shuki=1: kibetsu1 → ordinal=1 → nokigen1st="20240430"
        assertThat(result.getItems().get(0).getDisplayKigen()).isEqualTo("2024年4月30日");
        assertThat(result.getItems().get(1).getDisplayKigen()).isEqualTo("2024年5月31日");
    }

    // TC-10: 適用納税周期が有効期間内かつdefaultShukiが月次 → shuki=3として3期ごとの納入期限がセット
    @Test
    void getDaichoData_resolveShuki_適用納税周期が有効期間内かつdefaultShuki月次_shuki3() {
        TokureiTekiyo tekiyo = new TokureiTekiyo();
        tekiyo.setTekiyoStYmd(LocalDate.of(2024, 1, 1));
        tekiyo.setTekiyoEdYmd(LocalDate.of(2024, 12, 31));
        stubDaichoBase("3", "1");
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tekiyo));
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, NENDO)))
                .thenReturn(Optional.of(fullNokigen()));

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        // shuki=3: kibetsu1,2,3 → ordinal=3 → nokigen3rd="20240630"
        assertThat(result.getItems().get(0).getDisplayKigen()).isEqualTo("2024年6月30日");
        assertThat(result.getItems().get(1).getDisplayKigen()).isEqualTo("2024年6月30日");
        assertThat(result.getItems().get(2).getDisplayKigen()).isEqualTo("2024年6月30日");
    }

    // TC-11: 適用納税周期が有効期間外かつdefaultShukiが月次 → shuki=1として各期別の納入期限がセット
    @Test
    void getDaichoData_resolveShuki_適用納税周期が有効期間外かつdefaultShuki月次_shuki1() {
        TokureiTekiyo tekiyo = new TokureiTekiyo();
        tekiyo.setTekiyoStYmd(LocalDate.of(2020, 1, 1));
        tekiyo.setTekiyoEdYmd(LocalDate.of(2020, 12, 31));
        stubDaichoBase("3", "1");
        when(tekiyoNozeiShukiRepository.findActiveByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(tekiyo));
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, NENDO)))
                .thenReturn(Optional.of(fullNokigen()));

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        // shuki=1: kibetsu1 → ordinal=1 → nokigen1st="20240430"
        assertThat(result.getItems().get(0).getDisplayKigen()).isEqualTo("2024年4月30日");
        assertThat(result.getItems().get(1).getDisplayKigen()).isEqualTo("2024年5月31日");
    }

    // TC-12: nokigenがnull → 全itemsのdisplayKigenが空文字
    @Test
    void getDaichoData_createNonyuKigenString_nokigenNull_全itemsのdisplayKigenが空文字() {
        stubDaichoBase("3", "1");
        // nokigenRepository.findById → empty のままなので nokigen=null

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        assertThat(result.getItems()).allMatch(item -> "".equals(item.getDisplayKigen()));
    }

    // TC-13: nokigenあり・納税周期が月次 → 各期別のdisplayKigenに対応する納入期限がセット
    @Test
    void getDaichoData_createNonyuKigenString_nokigenあり_月次_各期別に期限セット() {
        stubDaichoBase("3", "1");
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, NENDO)))
                .thenReturn(Optional.of(fullNokigen()));

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        assertThat(result.getItems().get(0).getDisplayKigen()).isEqualTo("2024年4月30日");
        assertThat(result.getItems().get(1).getDisplayKigen()).isEqualTo("2024年5月31日");
        assertThat(result.getItems().get(11).getDisplayKigen()).isEqualTo("2025年3月31日");
    }

    // TC-14: nokigenあり・納税周期が四半期 → 3期ごとにまとめた納入期限がdisplayKigenにセット
    @Test
    void getDaichoData_createNonyuKigenString_nokigenあり_四半期_3期ごとに期限セット() {
        stubDaichoBase("3", "3");
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, NENDO)))
                .thenReturn(Optional.of(fullNokigen()));

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        // kibetsu1,2,3 → ordinal=3 → nokigen3rd="20240630"
        assertThat(result.getItems().get(0).getDisplayKigen()).isEqualTo("2024年6月30日");
        assertThat(result.getItems().get(1).getDisplayKigen()).isEqualTo("2024年6月30日");
        assertThat(result.getItems().get(2).getDisplayKigen()).isEqualTo("2024年6月30日");
        // kibetsu4,5,6 → ordinal=6 → nokigen6th="20240930"
        assertThat(result.getItems().get(3).getDisplayKigen()).isEqualTo("2024年9月30日");
    }

    // TC-15: nokigenあり・対応するnokigenValueがnullまたは8桁未満 → 該当期別のdisplayKigenが空文字
    @Test
    void getDaichoData_createNonyuKigenString_nokigenValueがnullまたは8桁未満_空文字() {
        stubDaichoBase("3", "1");
        Nokigen nokigen = new Nokigen();
        nokigen.setNokigen1st(null);   // null
        nokigen.setNokigen2nd("2024"); // 4桁（8桁未満）
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, NENDO)))
                .thenReturn(Optional.of(nokigen));

        FukaDaichoForm result = service.getDaichoData(SHITEI_NO, NENDO);

        assertThat(result.getItems().get(0).getDisplayKigen()).isEqualTo("");
        assertThat(result.getItems().get(1).getDisplayKigen()).isEqualTo("");
    }

    // -----------------------------------------------------------------------
    // getExistingNendoList
    // -----------------------------------------------------------------------

    // TC-16: fukaListが空 → 空リストが返る
    @Test
    void getExistingNendoList_fukaListが空_空リストが返る() {
        when(fukaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());

        assertThat(service.getExistingNendoList(SHITEI_NO)).isEmpty();
    }

    // TC-17: 有効データのみ → 年度リストが返る
    @Test
    void getExistingNendoList_有効データのみ_年度リストが返る() {
        Fuka f1 = new Fuka(); f1.setNendo("2023"); f1.setNewFlg("1"); f1.setDelFlg("0");
        Fuka f2 = new Fuka(); f2.setNendo("2024"); f2.setNewFlg("1"); f2.setDelFlg("0");
        when(fukaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(f1, f2));

        List<Integer> result = service.getExistingNendoList(SHITEI_NO);

        assertThat(result).containsExactly(2023, 2024);
    }

    // TC-18: 有効・無効データ混在 → 無効データが除外された年度リストが返る
    @Test
    void getExistingNendoList_有効無効混在_無効データが除外された年度リストが返る() {
        Fuka valid  = new Fuka(); valid.setNendo("2024");  valid.setNewFlg("1");  valid.setDelFlg("0");
        Fuka deleted = new Fuka(); deleted.setNendo("2023"); deleted.setNewFlg("1"); deleted.setDelFlg("1");
        Fuka oldFlg  = new Fuka(); oldFlg.setNendo("2022");  oldFlg.setNewFlg("0");  oldFlg.setDelFlg("0");
        when(fukaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(valid, deleted, oldFlg));

        List<Integer> result = service.getExistingNendoList(SHITEI_NO);

        assertThat(result).containsExactly(2024);
    }

    // -----------------------------------------------------------------------
    // isGassanTargetMonth
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // isShiteiNoGassanTargetMonth
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // resolveGassanTekiyoPeriod
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // getNendoStMonth
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // getExistingNendoList (No.82-84 はTC-16〜TC-18と同内容のため割愛)
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // isAlreadyRegistered
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // isAlreadyRegisteredByKibetsu
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // calculateTax
    // -----------------------------------------------------------------------

    // No.67 定額制の場合、宿泊数×（市税率+県税率）を返す
    @Test
    void calculateTax_定額制_宿泊数に市税率プラス県税率を掛けた値を返す() {
        long result = service.calculateTax("1", 3L, java.math.BigDecimal.valueOf(200), java.math.BigDecimal.valueOf(100));
        assertThat(result).isEqualTo(900L);
    }

    // No.68 定率制の場合、宿泊料金×税率/100（端数切り捨て）を返す
    @Test
    void calculateTax_定率制_宿泊料金に税率を掛けて100で割り端数切り捨て() {
        long result = service.calculateTax("2", 15000L, java.math.BigDecimal.valueOf(2.0), null);
        assertThat(result).isEqualTo(300L);
    }

    // No.69 cityRate・kenRateがnullの場合、0を返す
    @Test
    void calculateTax_cityRateとkenRateがnull_0を返す() {
        long result = service.calculateTax("1", 3L, null, null);
        assertThat(result).isEqualTo(0L);
    }

}
