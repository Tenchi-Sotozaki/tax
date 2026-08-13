package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.entity.ReportsLog;
import jp.lg.asp.accommodation.entity.RptStatus;
import jp.lg.asp.accommodation.repository.ReportsLogRepository;
import jp.lg.asp.accommodation.repository.RptStatusRepository;
import jp.lg.asp.accommodation.service.impl.KofuKetteiTsuchiShinseiReportsServiceImpl;

/**
 * 交付金 決定通知書・交付申請書 PDF生成（ACCOMMODATION_TAX-354 / 355）の単体テスト。
 *
 * 印刷対象の組み合わせによる分岐と、帳票ログ・状況ステータスの保存を検証する。
 * PDF生成そのものは JasperReports を実際に動かしているため、他の単体テストより時間がかかる。
 */
@ExtendWith(MockitoExtension.class)
class KofuKetteiTsuchiShinseiReportsServiceImplTest {

    @Mock ReportsLogRepository reportsLogRepository;
    @Mock RptStatusRepository rptStatusRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks KofuKetteiTsuchiShinseiReportsServiceImpl service;

    private static final String JICHITAI_CD = "01100";
    private static final String SHITEI_NO = "00100001";

    @BeforeEach
    void setUp() {
        // 印刷対象が無いケースでは saveLog まで到達しないため lenient
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
        lenient().when(reportsLogRepository.findNextSeq(JICHITAI_CD)).thenReturn(1L);
        lenient().when(rptStatusRepository.findByJichitaiCdAndShiteiNoAndRptId(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    /** 決定通知書・交付申請書のどちらも印刷対象にした入力 */
    private KofuKetteiTsuchiShinseiDto fullDto() {
        KofuKetteiTsuchiShinseiDto dto = new KofuKetteiTsuchiShinseiDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setNendo("2026");
        dto.setHakkoYoshiki("様式第1号");
        dto.setCityName("札幌市");
        dto.setJorei("札幌市宿泊税条例第5条");
        dto.setShisetsuJusho("札幌市中央区北2条西2丁目");
        dto.setShisetsuName("ホテルA 札幌");
        dto.setNonyugaku("1,000,000");
        dto.setKofugaku("20,000");
        dto.setKofuJoken("交付条件テスト");
        dto.setTokuName("株式会社ホテルA");
        dto.setHakkoYmd("2026年4月1日");
        dto.setKofuYmd("2026年4月30日");
        dto.setHakkoJorei("札幌市宿泊税条例施行規則");
        dto.setOperation(ReportsConstants.SOUSA_PDF);
        return dto;
    }

    private void assertPdf(byte[] pdf) {
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    private List<ReportsLog> savedLogs() {
        ArgumentCaptor<ReportsLog> captor = ArgumentCaptor.forClass(ReportsLog.class);
        verify(reportsLogRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    // ===================================================================
    // generatekofuKetteiTsuchiShinseiPdf — 単票発行
    // ===================================================================

    @Test
    void 単票_印刷対象が未選択なら例外を投げる() {
        KofuKetteiTsuchiShinseiDto dto = fullDto();
        dto.setKetteiTsuchi(false);
        dto.setShinsei(false);

        assertThatThrownBy(() -> service.generatekofuKetteiTsuchiShinseiPdf(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("印刷対象が選択されていません");

        verify(reportsLogRepository, never()).save(any());
    }

    @Test
    void 単票_決定通知書のみ選択すると帳票ログが1件だけ残る() {
        KofuKetteiTsuchiShinseiDto dto = fullDto();
        dto.setKetteiTsuchi(true);
        dto.setShinsei(false);

        assertPdf(service.generatekofuKetteiTsuchiShinseiPdf(dto));

        List<ReportsLog> logs = savedLogs();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getRptId()).isEqualTo(ReportsConstants.KOFU_KETTEI_TSUCHI);
    }

    @Test
    void 単票_交付申請書のみ選択すると帳票ログが1件だけ残る() {
        KofuKetteiTsuchiShinseiDto dto = fullDto();
        dto.setKetteiTsuchi(false);
        dto.setShinsei(true);

        assertPdf(service.generatekofuKetteiTsuchiShinseiPdf(dto));

        List<ReportsLog> logs = savedLogs();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getRptId()).isEqualTo(ReportsConstants.KOFU_SHINSEI);
    }

    @Test
    void 単票_両方選択すると帳票ログが2件残る() {
        assertPdf(service.generatekofuKetteiTsuchiShinseiPdf(fullDto()));

        assertThat(savedLogs())
                .extracting(ReportsLog::getRptId)
                .containsExactly(ReportsConstants.KOFU_KETTEI_TSUCHI, ReportsConstants.KOFU_SHINSEI);
    }

    @Test
    void 単票_帳票ログに自治体コードと操作種別と指定番号が記録される() {
        KofuKetteiTsuchiShinseiDto dto = fullDto();
        dto.setShinsei(false);
        dto.setOperation(ReportsConstants.SOUSA_PRINT);

        service.generatekofuKetteiTsuchiShinseiPdf(dto);

        ReportsLog log = savedLogs().get(0);
        assertThat(log.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(log.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(log.getSousa()).isEqualTo(ReportsConstants.SOUSA_PRINT);
        assertThat(log.getSeq()).isEqualTo(1L);
        assertThat(log.getOpeDt()).isNotNull();
        // 認証情報が無い実行環境では anonymous になる
        assertThat(log.getOpeUser()).isNotNull();
    }

    @Test
    void 単票_状況ステータスが新規なら登録される() {
        KofuKetteiTsuchiShinseiDto dto = fullDto();
        dto.setShinsei(false);

        service.generatekofuKetteiTsuchiShinseiPdf(dto);

        ArgumentCaptor<RptStatus> captor = ArgumentCaptor.forClass(RptStatus.class);
        verify(rptStatusRepository).save(captor.capture());
        RptStatus sts = captor.getValue();
        assertThat(sts.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(sts.getShiteiNo()).isEqualTo(SHITEI_NO);
        assertThat(sts.getRptId()).isEqualTo(ReportsConstants.KOFU_KETTEI_TSUCHI);
        assertThat(sts.getCreateDt()).isNotNull();
    }

    @Test
    void 単票_状況ステータスが既存なら同じインスタンスが更新される() {
        RptStatus existing = new RptStatus();
        when(rptStatusRepository.findByJichitaiCdAndShiteiNoAndRptId(
                JICHITAI_CD, SHITEI_NO, ReportsConstants.KOFU_KETTEI_TSUCHI))
                .thenReturn(Optional.of(existing));
        KofuKetteiTsuchiShinseiDto dto = fullDto();
        dto.setShinsei(false);

        service.generatekofuKetteiTsuchiShinseiPdf(dto);

        verify(rptStatusRepository).save(same(existing));
    }

    /** ログ保存の失敗は握りつぶされ、PDF生成は継続する */
    @Test
    void 単票_帳票ログの保存に失敗してもPDFは返る() {
        when(reportsLogRepository.findNextSeq(JICHITAI_CD)).thenThrow(new RuntimeException("DB接続エラー"));
        KofuKetteiTsuchiShinseiDto dto = fullDto();
        dto.setShinsei(false);

        assertPdf(service.generatekofuKetteiTsuchiShinseiPdf(dto));

        verify(reportsLogRepository, never()).save(any());
    }

    // ===================================================================
    // generateBulkPdf — 一括発行
    // ===================================================================

    @Test
    void 一括_リストがnullなら例外を投げる() {
        assertThatThrownBy(() -> service.generateBulkPdf(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("帳票データがありません");
    }

    @Test
    void 一括_リストが空なら例外を投げる() {
        assertThatThrownBy(() -> service.generateBulkPdf(List.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("帳票データがありません");
    }

    @Test
    void 一括_全件が印刷対象外なら例外を投げる() {
        KofuKetteiTsuchiShinseiDto dto = fullDto();
        dto.setKetteiTsuchi(false);
        dto.setShinsei(false);

        assertThatThrownBy(() -> service.generateBulkPdf(List.of(dto)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("印刷対象がありません");

        verify(reportsLogRepository, never()).save(any());
    }

    @Test
    void 一括_2件ぶんを結合したPDFが返り帳票ログが件数分残る() {
        KofuKetteiTsuchiShinseiDto dto1 = fullDto();
        dto1.setShinsei(false);
        KofuKetteiTsuchiShinseiDto dto2 = fullDto();
        dto2.setShiteiNo("00100002");
        dto2.setShinsei(false);

        assertPdf(service.generateBulkPdf(List.of(dto1, dto2)));

        assertThat(savedLogs())
                .extracting(ReportsLog::getShiteiNo)
                .containsExactly(SHITEI_NO, "00100002");
    }
}
