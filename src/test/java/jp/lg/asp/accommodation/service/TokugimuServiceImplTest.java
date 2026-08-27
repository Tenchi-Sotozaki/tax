package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.KyodoJigyoshaRepository;
import jp.lg.asp.accommodation.repository.ShoyushaRepository;
import jp.lg.asp.accommodation.repository.ShunoRirekiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.TokugimuServiceImpl;

@ExtendWith(MockitoExtension.class)
class TokugimuServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock GassanRepository gassanRepository;
    @Mock GassanUchiRepository gassanUchiRepository;
    @Mock ShoyushaRepository shoyushaRepository;
    @Mock KyodoJigyoshaRepository kyodoJigyoshaRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock FukaRepository fukaRepository;
    @Mock ShunoRirekiRepository shunoRirekiRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks TokugimuServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    private Tokugimu buildTokugimu(String shiteiNo) {
        Tokugimu t = new Tokugimu();
        t.setShiteiNo(shiteiNo);
        t.setAtenaNo(BigDecimal.ONE);
        t.setShisetsuName("テスト施設");
        t.setKyokaName("テスト事業者");
        t.setRno(BigDecimal.ONE);
        return t;
    }

    private Atena buildAtena() {
        Atena a = new Atena();
        a.setAtenaNo(BigDecimal.ONE);
        a.setName("テスト事業者");
        return a;
    }

    /**
     * テスト用のSHA-256ハッシュ化ヘルパー
     */
    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void search_emptyForm_returnsAllItems() {
        TokugimuSearchForm form = new TokugimuSearchForm();
        form.setPage(0);
        form.setPageSize(10);

        Tokugimu t = buildTokugimu(SHITEI_NO);
        when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of(buildAtena()));
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of());

        var result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_emptyResult_returnsEmptyPage() {
        TokugimuSearchForm form = new TokugimuSearchForm();
        form.setPage(0);
        form.setPageSize(10);
        when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());

        var result = service.search(form);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void search_withKojinNo_hashesAndCallsRepository() {
        TokugimuSearchForm form = new TokugimuSearchForm();
        form.setPage(0);
        form.setPageSize(10);
        String rawKojinNo = "test_kojin_no_12345";
        form.setKojinNo(rawKojinNo);

        String expectedHashedKojinNo = sha256Hex(rawKojinNo);
        Tokugimu t = buildTokugimu(SHITEI_NO);

        when(tokugimuRepository.findBySearchConditions(
                eq(JICHITAI_CD),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("999"),
                eq(expectedHashedKojinNo), // ハッシュ化された個人番号が渡されること
                isNull()
        )).thenReturn(List.of(t));

        when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of(buildAtena()));
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of());

        var result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(tokugimuRepository).findBySearchConditions(
                eq(JICHITAI_CD),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq("999"),
                eq(expectedHashedKojinNo),
                isNull()
        );
    }

    @Test
    void searchAll_returnsAllItems() {
        TokugimuSearchForm form = new TokugimuSearchForm();

        Tokugimu t = buildTokugimu(SHITEI_NO);
        when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of(buildAtena()));
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of());

        var result = service.searchAll(form);

        assertThat(result).hasSize(1);
    }

    @Test
    void getTokugimuByShiteiNo_found() {
        Tokugimu t = buildTokugimu(SHITEI_NO);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(Optional.of(buildAtena()));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.of(1));
        when(tokugimuRepository.findMinRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.of(1));
        when(shoyushaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());
        when(kyodoJigyoshaRepository.findByJichitaiCdAndShiteiNoAndRno(eq(JICHITAI_CD), eq(SHITEI_NO), any()))
                .thenReturn(List.of());

        TokugimuForm form = service.getTokugimuByShiteiNo(SHITEI_NO);

        assertThat(form.getShiteiNo()).isEqualTo(SHITEI_NO);
    }

    @Test
    void getTokugimuByShiteiNo_notFound_throwsException() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getTokugimuByShiteiNo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void register_noAtenaNo_throwsException() {
        TokugimuForm form = new TokugimuForm();
        form.setAtenaNo(null);

        assertThatThrownBy(() -> service.register(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("宛名番号");
    }

    @Test
    void deleteByShiteiNo_履歴が残らない場合はfalseを返す() {
        Tokugimu t = buildTokugimu(SHITEI_NO);
        t.setDelFlg("0");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(t));
        when(tokugimuRepository.findActiveHistoryByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean remains = service.deleteByShiteiNo(SHITEI_NO);

        assertThat(remains).isFalse();
        assertThat(t.getDelFlg()).isEqualTo("1");
        assertThat(t.getNewFlg()).isEqualTo("0");
    }

    @Test
    void deleteByShiteiNo_履歴が残る場合は最新履歴を最新版に戻す() {
        Tokugimu current = buildTokugimu(SHITEI_NO);
        current.setDelFlg("0");
        current.setRno(BigDecimal.valueOf(2));
        Tokugimu prev = buildTokugimu(SHITEI_NO);
        prev.setDelFlg("0");
        prev.setNewFlg("0");
        prev.setRno(BigDecimal.ONE);

        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(current));
        when(tokugimuRepository.findActiveHistoryByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(prev));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean remains = service.deleteByShiteiNo(SHITEI_NO);

        assertThat(remains).isTrue();
        assertThat(current.getDelFlg()).isEqualTo("1");
        assertThat(prev.getNewFlg()).isEqualTo("1");
    }

    @Test
    void deleteByShiteiNo_notFound_throwsException() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        assertThatThrownBy(() -> service.deleteByShiteiNo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getShiteiNoById_found() {
        Tokugimu t = buildTokugimu(SHITEI_NO);
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(List.of(t));

        assertThat(service.getShiteiNoById(1L)).isEqualTo(SHITEI_NO);
    }

    @Test
    void getShiteiNoById_notFound_throwsException() {
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.getShiteiNoById(99L))
                .isInstanceOf(RuntimeException.class);
    }
    
    @Nested
    @DisplayName("register メソッドのテスト")
    class RegisterTest {

        @Test
        @DisplayName("正常系：必須項目が揃っている場合、新規登録処理が正常に完了すること")
        void success() {
            TokugimuForm form = new TokugimuForm();
            form.setAtenaNo(1L);
            form.setFacilityName("テスト施設");

            Jichitai jichitai = new Jichitai();
            jichitai.setShiteiStChar("000");

            when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
            when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any()))
                    .thenReturn(Optional.of(new Atena()));
            when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(any(), any()))
                    .thenReturn(Optional.of(5));

            service.register(form);

            verify(tokugimuRepository, times(1)).save(any(Tokugimu.class));
        }

        @Test
        @DisplayName("異常系：宛名番号がnullの場合、IllegalArgumentExceptionがスローされること")
        void exception_atenaNoNull() {
            TokugimuForm form = new TokugimuForm();
            form.setAtenaNo(null);

            assertThatThrownBy(() -> service.register(form))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("宛名番号が指定されていません");
        }
    }

    
    @Nested
    @DisplayName("deleteByShiteiNo メソッドのテスト")
    class DeleteByShiteiNoTest {

        @Test
        @DisplayName("正常系：削除対象が存在し、過去の履歴（有効な履歴）が存在する場合、フラグが適切に切り替わりtrueが返ること")
        void success_withHistory() {
            String shiteiNo = "00000001";
            Tokugimu target = new Tokugimu();
            target.setShiteiNo(shiteiNo);
            target.setNewFlg("1");

            Tokugimu history = new Tokugimu();
            history.setShiteiNo(shiteiNo);
            history.setRno(BigDecimal.ONE);
            history.setNewFlg("0");

            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, shiteiNo))
                    .thenReturn(List.of(target));
            when(tokugimuRepository.findActiveHistoryByJichitaiCdAndShiteiNo(JICHITAI_CD, shiteiNo))
                    .thenReturn(List.of(history));

            boolean result = service.deleteByShiteiNo(shiteiNo);

            assertThat(result).isTrue();
            assertThat(target.getDelFlg()).isEqualTo("1");
            assertThat(history.getNewFlg()).isEqualTo("1");
            verify(tokugimuRepository, times(2)).save(any(Tokugimu.class));
        }

        @Test
        @DisplayName("境界値：削除対象はあるが、履歴が他に存在しない場合、delFlgのみ更新されfalseが返ること")
        void boundary_noHistory() {
            String shiteiNo = "00000001";
            Tokugimu target = new Tokugimu();
            target.setShiteiNo(shiteiNo);

            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, shiteiNo))
                    .thenReturn(List.of(target));
            when(tokugimuRepository.findActiveHistoryByJichitaiCdAndShiteiNo(JICHITAI_CD, shiteiNo))
                    .thenReturn(List.of());

            boolean result = service.deleteByShiteiNo(shiteiNo);

            assertThat(result).isFalse();
            assertThat(target.getDelFlg()).isEqualTo("1");
            verify(tokugimuRepository, times(1)).save(any(Tokugimu.class));
        }

        @Test
        @DisplayName("異常系：削除対象の指定番号が見つからない場合、RuntimeExceptionがスローされること")
        void exception_notFound() {
            String shiteiNo = "99999999";
            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, shiteiNo))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.deleteByShiteiNo(shiteiNo))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("削除対象が見つかりません");
        }
    }
    
    @Nested
    @DisplayName("updateByShiteiNo メソッドのテスト")
    class UpdateByShiteiNoTest {

        @Test
        @DisplayName("正常系：既存データの旧レコードの new_flg が 0 になり、インクリメントされた rno で新レコードが保存されること")
        void success() {
            String shiteiNo = "00000001";
            TokugimuForm form = new TokugimuForm();
            form.setAtenaNo(1L);
            form.setFacilityName("更新後施設名");

            Tokugimu oldTokugimu = new Tokugimu();
            oldTokugimu.setShiteiNo(shiteiNo);
            oldTokugimu.setNewFlg("1");
            oldTokugimu.setRno(BigDecimal.ONE);

            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, shiteiNo))
                    .thenReturn(List.of(oldTokugimu));
            when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, shiteiNo))
                    .thenReturn(Optional.of(1));

            service.updateByShiteiNo(shiteiNo, form);

            assertThat(oldTokugimu.getNewFlg()).isEqualTo("0");
            // oldの保存と新レコードの保存で計2回saveが呼ばれる
            verify(tokugimuRepository, times(2)).save(any(Tokugimu.class));
        }

        @Test
        @DisplayName("異常系：更新対象の指定番号が存在しない場合、RuntimeExceptionがスローされること")
        void exception_notFound() {
            String shiteiNo = "99999999";
            TokugimuForm form = new TokugimuForm();

            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, shiteiNo))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.updateByShiteiNo(shiteiNo, form))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("特別徴収義務者が見つかりません");
        }
    }
}