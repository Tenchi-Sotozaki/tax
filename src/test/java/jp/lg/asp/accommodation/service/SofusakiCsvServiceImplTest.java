package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
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

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.SofusakiCsvDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.ReportsLog;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.SofusakiCsvRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.SofusakiCsvServiceImpl;

/**
 * 送付先情報CSV出力 単体テスト（サービス）
 *
 * <p>チェックリスト「送付先情報CSV出力_単体テストチェックリスト.xlsx」の #6〜#26 に1対1で対応する。
 * チェックリストはあるべき仕様で書かれている。テストが通るように期待値を実装へ寄せないこと。</p>
 */
@ExtendWith(MockitoExtension.class)
class SofusakiCsvServiceImplTest {

    @Mock SofusakiCsvRepository sofusakiCsvRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks SofusakiCsvServiceImpl service;

    private static final String JICHITAI_CD = "01100";
    private static final String HEADER = "宛名番号,指定番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\n";

    @BeforeEach
    void setUp() {
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ------------------------------------------------------------------
    // テストデータ生成
    // ------------------------------------------------------------------

    private ReportsLog log(String shiteiNo, String rptId, LocalDateTime opeDt) {
        ReportsLog log = new ReportsLog();
        log.setJichitaiCd(JICHITAI_CD);
        log.setSeq(1L);
        log.setShiteiNo(shiteiNo);
        log.setRptId(rptId);
        log.setOpeDt(opeDt);
        return log;
    }

    private Tokugimu tokugimu(BigDecimal atenaNo, String shiteiNo) {
        Tokugimu t = new Tokugimu();
        t.setJichitaiCd(JICHITAI_CD);
        t.setAtenaNo(atenaNo);
        t.setShiteiNo(shiteiNo);
        return t;
    }

    private Atena atena(String name, String nameKana, String yubinNo, String jusho, String tel1) {
        Atena a = new Atena();
        a.setJichitaiCd(JICHITAI_CD);
        a.setName(name);
        a.setNameKana(nameKana);
        a.setYubinNo(yubinNo);
        a.setJusho(jusho);
        a.setTel1(tel1);
        return a;
    }

    /** findPrintedLogs をスタブする */
    private void stubLogs(ReportsLog... logs) {
        when(sofusakiCsvRepository.findPrintedLogs(eq(JICHITAI_CD), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(logs));
    }

    /** findPrintedLogsWithRptName をスタブする（row = {rptId, rptName}） */
    private void stubRptNames(Object[]... rows) {
        when(sofusakiCsvRepository.findPrintedLogsWithRptName(eq(JICHITAI_CD), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(rows));
    }

    private SofusakiCsvDto csvDto(BigDecimal atenaNo, String shiteiNo, String name, String nameKana,
            String yubinNo, String jusho, String tel) {
        SofusakiCsvDto dto = new SofusakiCsvDto();
        dto.setAtenaNo(atenaNo);
        dto.setShiteiNo(shiteiNo);
        dto.setSoufusakiName(name);
        dto.setSoufusakiNameKana(nameKana);
        dto.setSoufusakiYubinNo(yubinNo);
        dto.setSoufusakiJusho(jusho);
        dto.setSoufusakiTel(tel);
        return dto;
    }

    // ==================================================================
    // findAll
    // ==================================================================

    @Test
    @DisplayName("#6 findAll 正常系 ログ1件・特別徴収義務者あり・宛名ありの場合：DTOが1件返る")
    void findAll_ログ1件で1件のDTOが返る() {
        LocalDateTime opeDt = LocalDateTime.of(2026, 8, 1, 10, 0);
        stubLogs(log("0001", "R01", opeDt));
        stubRptNames(new Object[] { "R01", "特別徴収義務者指定通知" });

        Tokugimu t = tokugimu(BigDecimal.ONE, "0001");
        t.setSoufusakiName("山田太郎");
        t.setSoufusakiNameKana("ヤマダタロウ");
        t.setSoufusakiYubinNo("1000001");
        t.setSoufusakiJusho("東京都千代田区1-1");
        t.setSoufusakiTel("03-1234-5678");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "0001")).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena("宛名太郎", "アテナタロウ", "1000002", "東京都港区2-2", "03-9999-9999")));

        List<SofusakiCsvDto> result = service.findAll();

        assertThat(result).hasSize(1);
        SofusakiCsvDto dto = result.get(0);
        assertThat(dto.getAtenaNo()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(dto.getShiteiNo()).isEqualTo("0001");
        assertThat(dto.getSoufusakiName()).isEqualTo("山田太郎");
        assertThat(dto.getSoufusakiNameKana()).isEqualTo("ヤマダタロウ");
        assertThat(dto.getSoufusakiYubinNo()).isEqualTo("1000001");
        assertThat(dto.getSoufusakiJusho()).isEqualTo("東京都千代田区1-1");
        assertThat(dto.getSoufusakiTel()).isEqualTo("03-1234-5678");
        assertThat(dto.getRptName()).isEqualTo("特別徴収義務者指定通知");
        assertThat(dto.getOpeDt()).isEqualTo(opeDt);
    }

    @Test
    @DisplayName("#7 findAll 正常系 対象ログが0件の場合：空リストを返す")
    void findAll_対象ログ0件は空リスト() {
        stubLogs();
        stubRptNames();

        List<SofusakiCsvDto> result = service.findAll();

        assertThat(result).isEmpty();
        verify(tokugimuRepository, never()).findByJichitaiCdAndShiteiNo(any(), any());
        verify(atenaRepository, never()).findByJichitaiCdAndAtenaNo(any(), any());
    }

    @Test
    @DisplayName("#8 findAll 異常系 指定番号に紐づく特別徴収義務者が存在しない場合：その行はスキップされる")
    void findAll_特別徴収義務者なしはスキップ() {
        stubLogs(log("9999", "R01", LocalDateTime.now()));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "9999")).thenReturn(List.of());

        List<SofusakiCsvDto> result = service.findAll();

        assertThat(result).isEmpty();
        verify(atenaRepository, never()).findByJichitaiCdAndAtenaNo(any(), any());
    }

    @Test
    @DisplayName("#9 findAll 正常系 同一指定番号で複数件返る場合：先頭の1件のみ使用する")
    void findAll_複数件は先頭のみ使用() {
        stubLogs(log("0001", "R01", LocalDateTime.now()));
        Tokugimu t1 = tokugimu(BigDecimal.ONE, "0001");
        Tokugimu t2 = tokugimu(BigDecimal.valueOf(2), "0001");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "0001")).thenReturn(List.of(t1, t2));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena("宛名太郎", null, null, null, null)));

        List<SofusakiCsvDto> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAtenaNo()).isEqualByComparingTo(BigDecimal.ONE);
        verify(atenaRepository, times(1)).findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE);
    }

    @Test
    @DisplayName("#10 findAll 正常系 送付先が未設定（null・空文字・空白）の場合：宛名の値で補完される")
    void findAll_送付先未設定は宛名で補完される() {
        stubLogs(log("0001", "R01", LocalDateTime.now()));
        Tokugimu t = tokugimu(BigDecimal.ONE, "0001");
        t.setSoufusakiName(null);
        t.setSoufusakiNameKana("");
        t.setSoufusakiYubinNo(" ");
        t.setSoufusakiJusho(null);
        t.setSoufusakiTel(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "0001")).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena("宛名太郎", "アテナタロウ", "1000002", "東京都港区2-2", "03-9999-9999")));

        SofusakiCsvDto dto = service.findAll().get(0);

        assertThat(dto.getSoufusakiName()).isEqualTo("宛名太郎");
        assertThat(dto.getSoufusakiNameKana()).isEqualTo("アテナタロウ");
        assertThat(dto.getSoufusakiYubinNo()).isEqualTo("1000002");
        assertThat(dto.getSoufusakiJusho()).isEqualTo("東京都港区2-2");
        assertThat(dto.getSoufusakiTel()).isEqualTo("03-9999-9999");
    }

    @Test
    @DisplayName("#11 findAll 正常系 送付先が設定済みの場合：宛名の値では上書きされない")
    void findAll_送付先設定済みは上書きされない() {
        stubLogs(log("0001", "R01", LocalDateTime.now()));
        Tokugimu t = tokugimu(BigDecimal.ONE, "0001");
        t.setSoufusakiName("送付先太郎");
        t.setSoufusakiTel("03-1234-5678");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "0001")).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena("宛名太郎", null, null, null, "03-9999-9999")));

        SofusakiCsvDto dto = service.findAll().get(0);

        assertThat(dto.getSoufusakiName()).isEqualTo("送付先太郎");
        assertThat(dto.getSoufusakiTel()).isEqualTo("03-1234-5678");
    }

    @Test
    @DisplayName("#12 findAll 異常系 宛名が存在しない場合：補完されずnullのままとなる")
    void findAll_宛名なしはnullのまま() {
        stubLogs(log("0001", "R01", LocalDateTime.now()));
        Tokugimu t = tokugimu(BigDecimal.ONE, "0001");
        t.setSoufusakiName(null);
        t.setSoufusakiNameKana(null);
        t.setSoufusakiYubinNo(null);
        t.setSoufusakiJusho(null);
        t.setSoufusakiTel(null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "0001")).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());

        List<SofusakiCsvDto> result = service.findAll();

        assertThat(result).hasSize(1);
        SofusakiCsvDto dto = result.get(0);
        assertThat(dto.getSoufusakiName()).isNull();
        assertThat(dto.getSoufusakiNameKana()).isNull();
        assertThat(dto.getSoufusakiYubinNo()).isNull();
        assertThat(dto.getSoufusakiJusho()).isNull();
        assertThat(dto.getSoufusakiTel()).isNull();
    }

    @Test
    @DisplayName("#13 findAll 正常系 指定番号に前後空白がある場合：除去して設定される")
    void findAll_指定番号の前後空白は除去される() {
        stubLogs(log("0001  ", "R01", LocalDateTime.now()));
        Tokugimu t = tokugimu(BigDecimal.ONE, "0001  ");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "0001  ")).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());

        assertThat(service.findAll().get(0).getShiteiNo()).isEqualTo("0001");
    }

    @Test
    @DisplayName("#14 findAll 正常系 指定番号がnullの場合：nullのまま設定される")
    void findAll_指定番号nullはnullのまま() {
        stubLogs(log(null, "R01", LocalDateTime.now()));
        Tokugimu t = tokugimu(BigDecimal.ONE, null);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, null)).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());

        assertThat(service.findAll().get(0).getShiteiNo()).isNull();
    }

    @Test
    @DisplayName("#15 findAll 正常系 帳票IDが帳票名リストに存在しない場合：帳票名は空文字となる")
    void findAll_帳票ID未一致は空文字() {
        stubLogs(log("0001", "R01", LocalDateTime.now()));
        stubRptNames(new Object[] { "R99", "別の帳票" });
        stubTokugimuAndAtena();

        assertThat(service.findAll().get(0).getRptName()).isEmpty();
    }

    @Test
    @DisplayName("#16 findAll 正常系 帳票IDに前後空白がある場合：除去して突き合わせる")
    void findAll_帳票IDの前後空白を除去して突き合わせる() {
        stubLogs(log("0001", "R01 ", LocalDateTime.now()));
        stubRptNames(new Object[] { "R01 ", "特別徴収義務者指定通知" });
        stubTokugimuAndAtena();

        assertThat(service.findAll().get(0).getRptName()).isEqualTo("特別徴収義務者指定通知");
    }

    @Test
    @DisplayName("#17 findAll 正常系 帳票IDが重複する場合：先に取得した行の帳票名を採用する")
    void findAll_帳票ID重複は先勝ち() {
        stubLogs(log("0001", "R01", LocalDateTime.now()));
        stubRptNames(new Object[] { "R01", "通知A" }, new Object[] { "R01", "通知B" });
        stubTokugimuAndAtena();

        assertThat(service.findAll().get(0).getRptName()).isEqualTo("通知A");
    }

    @Test
    @DisplayName("#18 findAll 正常系 帳票名がnullの場合：空文字として設定される")
    void findAll_帳票名nullは空文字() {
        stubLogs(log("0001", "R01", LocalDateTime.now()));
        stubRptNames(new Object[] { "R01", null });
        stubTokugimuAndAtena();

        assertThat(service.findAll().get(0).getRptName()).isEmpty();
    }

    @Test
    @DisplayName("#19 findAll 異常系 帳票IDがnullの行は帳票名リストから除外される")
    void findAll_帳票IDnullの行は除外される() {
        stubLogs(log("0001", "R01", LocalDateTime.now()));
        stubRptNames(new Object[] { null, "通知A" }, new Object[] { "R01", "通知B" });
        stubTokugimuAndAtena();

        assertThat(service.findAll().get(0).getRptName()).isEqualTo("通知B");
    }

    @Test
    @DisplayName("#20 findAll 正常系 抽出期間：現在日時の2週間前がリポジトリに渡される")
    void findAll_抽出期間は現在日時の2週間前() {
        stubLogs();
        stubRptNames();

        service.findAll();

        LocalDateTime expected = LocalDateTime.now().minusWeeks(2);

        ArgumentCaptor<LocalDateTime> logsCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sofusakiCsvRepository, times(1)).findPrintedLogs(eq(JICHITAI_CD), logsCaptor.capture());
        assertThat(logsCaptor.getValue()).isCloseTo(expected, within(5, ChronoUnit.SECONDS));

        ArgumentCaptor<LocalDateTime> nameCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sofusakiCsvRepository, times(1))
                .findPrintedLogsWithRptName(eq(JICHITAI_CD), nameCaptor.capture());
        assertThat(nameCaptor.getValue()).isCloseTo(expected, within(5, ChronoUnit.SECONDS));
    }

    /** #15〜#19 で共通の特別徴収義務者・宛名スタブ */
    private void stubTokugimuAndAtena() {
        Tokugimu t = tokugimu(BigDecimal.ONE, "0001");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "0001")).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.empty());
    }

    // ==================================================================
    // toCsvString
    // ==================================================================

    @Test
    @DisplayName("#21 toCsvString 正常系 ヘッダー行が正しく出力される")
    void toCsvString_ヘッダー行のみ() {
        String csv = service.toCsvString(List.of());

        assertThat(csv).isEqualTo(HEADER);
        // BOMはコントローラ側で付与するため、サービスの戻り値には含まれないこと
        assertThat(csv.charAt(0)).isNotEqualTo('\uFEFF');
    }

    @Test
    @DisplayName("#22 toCsvString 正常系 通常データが正しくCSV形式で出力される")
    void toCsvString_通常データ() {
        SofusakiCsvDto dto = csvDto(new BigDecimal("1"), "0001", "山田太郎", "ヤマダタロウ",
                "1000001", "東京都千代田区1-1", "03-1234-5678");

        String csv = service.toCsvString(List.of(dto));

        assertThat(csv).isEqualTo(HEADER
                + "=\"1\",=\"0001\",山田太郎,ヤマダタロウ,1000001,東京都千代田区1-1,03-1234-5678\n");
    }

    @Test
    @DisplayName("#23 toCsvString 正常系 カンマを含むフィールドはダブルクォートで囲まれる")
    void toCsvString_カンマ入りはクォート囲み() {
        SofusakiCsvDto dto = csvDto(new BigDecimal("1"), "0001", "山田太郎", "ヤマダタロウ",
                "1000001", "東京都,千代田区", "03-1234-5678");

        String dataLine = service.toCsvString(List.of(dto)).substring(HEADER.length());

        assertThat(dataLine).isEqualTo(
                "=\"1\",=\"0001\",山田太郎,ヤマダタロウ,1000001,\"東京都,千代田区\",03-1234-5678\n");
    }

    @Test
    @DisplayName("#24 toCsvString 正常系 ダブルクォートを含むフィールドはエスケープされる")
    void toCsvString_ダブルクォートはエスケープされる() {
        SofusakiCsvDto dto = csvDto(new BigDecimal("1"), "0001", "山田\"太郎", "ヤマダタロウ",
                "1000001", "東京都千代田区1-1", "03-1234-5678");

        String dataLine = service.toCsvString(List.of(dto)).substring(HEADER.length());

        assertThat(dataLine).isEqualTo(
                "=\"1\",=\"0001\",\"山田\"\"太郎\",ヤマダタロウ,1000001,東京都千代田区1-1,03-1234-5678\n");
    }

    @Test
    @DisplayName("#25 toCsvString 正常系 atenaNoとshiteiNoはExcel数値化防止のため=付きクォートで出力される")
    void toCsvString_宛名番号と指定番号は数値化防止形式() {
        SofusakiCsvDto dto = csvDto(new BigDecimal("12345"), "0001", null, null, null, null, null);

        String dataLine = service.toCsvString(List.of(dto)).substring(HEADER.length());
        String[] columns = dataLine.split(",", -1);

        assertThat(columns[0]).isEqualTo("=\"12345\"");
        assertThat(columns[1]).isEqualTo("=\"0001\"");
    }

    @Test
    @DisplayName("#26 toCsvString 正常系 nullフィールドは空文字として出力される")
    void toCsvString_nullは空文字() {
        SofusakiCsvDto dto = csvDto(null, null, null, null, null, null, null);

        String dataLine = service.toCsvString(List.of(dto)).substring(HEADER.length());
        String[] columns = dataLine.replace("\n", "").split(",", -1);

        assertThat(columns).as("カンマ区切りの列数は7列のまま維持されること").hasSize(7);
        assertThat(columns[0]).as("宛名番号列も空文字となること").isEmpty();
        assertThat(columns[2]).as("氏名列が空文字で出力されること").isEmpty();
        assertThat(columns[6]).as("電話番号列が空文字で出力されること").isEmpty();
    }
}
