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
import jp.lg.asp.accommodation.dto.KyodoJigyoshaDto;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.KyodoJigyosha;
import jp.lg.asp.accommodation.entity.Shoyusha;
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

    private static final String JICHITAI_CD = "012345";
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

    @Nested
    @DisplayName("search メソッドのテスト")
    class SearchTest {

        @Test
        @DisplayName("正常系：空のフォームの場合、すべてのアイテムが返ること")
        void search_emptyForm_returnsAllItems() {
            TokugimuSearchForm form = new TokugimuSearchForm();
            form.setPage(0);
            form.setPageSize(10);

            Tokugimu t = buildTokugimu(SHITEI_NO);
            when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(t));
            when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of(buildAtena()));
            when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any(), any())).thenReturn(List.of());

            var result = service.search(form);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("正常系：結果が空の場合、空のページが返ること")
        void search_emptyResult_returnsEmptyPage() {
            TokugimuSearchForm form = new TokugimuSearchForm();
            form.setPage(0);
            form.setPageSize(10);
            when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());

            var result = service.search(form);

            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("正常系：個人番号が指定されている場合、ハッシュ化されてリポジトリが呼び出されること")
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
                    eq(expectedHashedKojinNo),
                    isNull()
            )).thenReturn(List.of(t));

            when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of(buildAtena()));
            when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any(), any())).thenReturn(List.of());

            var result = service.search(form);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("正常系：合算指定番号プレフィックスで始まる場合、t_gassanから検索されること")
        void search_withGassanShiteiNo_callsGassanRepository() {
            TokugimuSearchForm form = new TokugimuSearchForm();
            form.setPage(0);
            form.setPageSize(10);
            form.setShiteiNo("90000001");

            Jichitai jichitai = new Jichitai();
            jichitai.setGassanStChar("900");
            when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
            when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "90000001"))
                    .thenReturn(List.of(new jp.lg.asp.accommodation.entity.Gassan()));
            
            Tokugimu t = buildTokugimu(SHITEI_NO);
            when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, "90000001"))
                    .thenAnswer(inv -> {
                        GassanUchi gu = new GassanUchi();
                        gu.setShiteiNo(SHITEI_NO);
                        return List.of(gu);
                    });
            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                    .thenReturn(List.of(t));
            when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of(buildAtena()));
            when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any(), any())).thenReturn(List.of());

            var result = service.search(form);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("searchAll メソッドのテスト")
    class SearchAllTest {

        @Test
        @DisplayName("正常系：すべてのアイテムが返ること")
        void searchAll_returnsAllItems() {
            TokugimuSearchForm form = new TokugimuSearchForm();

            Tokugimu t = buildTokugimu(SHITEI_NO);
            when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(t));
            when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of(buildAtena()));
            when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any(), any())).thenReturn(List.of());

            var result = service.searchAll(form);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getTokugimuByShiteiNo メソッドのテスト")
    class GetTokugimuByShiteiNoTest {

        @Test
        @DisplayName("正常系：指定番号に該当するデータが存在する場合、フォームが返ること")
        void getTokugimuByShiteiNo_found() {
            Tokugimu t = buildTokugimu(SHITEI_NO);
            t.setYukaMenseki(BigDecimal.valueOf(100));
            t.setChijoKai(BigDecimal.valueOf(2));
            t.setChikaKai(BigDecimal.valueOf(1));
            t.setKyakushitsuSu(BigDecimal.valueOf(10));
            t.setShuyoSu(BigDecimal.valueOf(20));
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
        @DisplayName("異常系：指定番号に該当するデータが存在しない場合、例外がスローされること")
        void getTokugimuByShiteiNo_notFound_throwsException() {
            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

            assertThatThrownBy(() -> service.getTokugimuByShiteiNo(SHITEI_NO))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("異常系：宛名が存在しない場合、例外がスローされること")
        void getTokugimuByShiteiNo_atenaNotFound_throwsException() {
            Tokugimu t = buildTokugimu(SHITEI_NO);
            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(t));
            when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTokugimuByShiteiNo(SHITEI_NO))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("getTokugimuByShiteiNoAndRno メソッドのテスト")
    class GetTokugimuByShiteiNoAndRnoTest {

        @Test
        @DisplayName("正常系：指定番号とrnoに該当するデータが存在する場合、フォームが返ること")
        void success() {
            Tokugimu t = buildTokugimu(SHITEI_NO);
            when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndRno(eq(JICHITAI_CD), eq(SHITEI_NO), any()))
                    .thenReturn(Optional.of(t));
            when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(Optional.of(buildAtena()));
            when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.of(1));
            when(tokugimuRepository.findMinRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.of(1));
            when(shoyushaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());
            when(kyodoJigyoshaRepository.findByJichitaiCdAndShiteiNoAndRno(eq(JICHITAI_CD), eq(SHITEI_NO), any()))
                    .thenReturn(List.of());

            TokugimuForm form = service.getTokugimuByShiteiNoAndRno(SHITEI_NO, 1);

            assertThat(form.getShiteiNo()).isEqualTo(SHITEI_NO);
        }

        @Test
        @DisplayName("異常系：指定番号とrnoに該当するデータが存在しない場合、例外がスローされること")
        void notFound() {
            when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndRno(eq(JICHITAI_CD), eq(SHITEI_NO), any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTokugimuByShiteiNoAndRno(SHITEI_NO, 1))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("getShiteiNoById メソッドのテスト")
    class GetShiteiNoByIdTest {

        @Test
        @DisplayName("正常系：IDに該当する指定番号が取得できること")
        void getShiteiNoById_found() {
            Tokugimu t = buildTokugimu(SHITEI_NO);
            when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(List.of(t));

            assertThat(service.getShiteiNoById(1L)).isEqualTo(SHITEI_NO);
        }

        @Test
        @DisplayName("異常系：IDに該当する指定番号が存在しない場合、例外がスローされること")
        void getShiteiNoById_notFound_throwsException() {
            when(tokugimuRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(List.of());

            assertThatThrownBy(() -> service.getShiteiNoById(99L))
                    .isInstanceOf(RuntimeException.class);
        }
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
            form.setFloorArea("100");
            form.setAboveGroundFloor("2");
            form.setBasementFloor("1");
            form.setRoomCount("10");
            form.setCapacity("20");
            form.setOwnerName("所有者");
            form.setOwnerNameKana("ショユシャ");
            form.setOwnerAddressNo("123-4567");
            form.setOwnerAddress("住所");
            form.setOwnerPhone("090-0000-0000");

            KyodoJigyoshaDto kDto = new KyodoJigyoshaDto();
            kDto.setKyodoName("共同事業者");
            form.setKyodoList(List.of(kDto));

            Jichitai jichitai = new Jichitai();
            jichitai.setShiteiStChar("000");

            when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
            when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any()))
                    .thenReturn(Optional.of(new Atena()));
            when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(any(), any()))
                    .thenReturn(Optional.of(5));

            service.register(form);

            verify(tokugimuRepository, times(1)).save(any(Tokugimu.class));
            verify(shoyushaRepository, times(1)).save(any(Shoyusha.class));
            verify(kyodoJigyoshaRepository, times(1)).save(any(KyodoJigyosha.class));
        }

        @Test
        @DisplayName("正常系：所有者情報がすべて空の場合、所有者が登録されずに処理が進むこと")
        void register_emptyOwner_skipsOwnerSave() {
            TokugimuForm form = new TokugimuForm();
            form.setAtenaNo(1L);
            form.setFacilityName("テスト施設");
            form.setOwnerName(null);
            form.setOwnerNameKana("");
            form.setOwnerAddressNo("");
            form.setOwnerAddress("");
            form.setOwnerPhone("");

            Jichitai jichitai = new Jichitai();
            jichitai.setShiteiStChar("000");

            when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
            when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any()))
                    .thenReturn(Optional.of(new Atena()));
            when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(any(), any()))
                    .thenReturn(Optional.of(1));

            service.register(form);

            verify(shoyushaRepository, never()).save(any(Shoyusha.class));
            verify(tokugimuRepository, times(1)).save(any(Tokugimu.class));
        }

        @Test
        @DisplayName("正常系：共同事業者リストに名前が空のデータが含まれる場合、それらがスキップされて登録されること")
        void register_kyodoListWithEmptyName_skipsEmptyItems() {
            TokugimuForm form = new TokugimuForm();
            form.setAtenaNo(1L);
            form.setFacilityName("テスト施設");
            form.setOwnerName("所有者あり");

            KyodoJigyoshaDto emptyDto = new KyodoJigyoshaDto();
            emptyDto.setKyodoName("");

            KyodoJigyoshaDto validDto = new KyodoJigyoshaDto();
            validDto.setKyodoName("有効な共同事業者");

            form.setKyodoList(List.of(emptyDto, validDto));

            Jichitai jichitai = new Jichitai();
            jichitai.setShiteiStChar("000");

            when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
            when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any()))
                    .thenReturn(Optional.of(new Atena()));
            when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(any(), any()))
                    .thenReturn(Optional.of(1));

            service.register(form);

            verify(kyodoJigyoshaRepository, times(1)).save(any(KyodoJigyosha.class));
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

        @Test
        @DisplayName("異常系：宛名が存在しない場合、IllegalArgumentExceptionがスローされること")
        void exception_atenaNotFound() {
            TokugimuForm form = new TokugimuForm();
            form.setAtenaNo(1L);

            when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.register(form))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
    
    @Nested
    @DisplayName("deleteByShiteiNo メソッドのテスト")
    class DeleteByShiteiNoTest {

        @Test
        @DisplayName("正常系：削除対象が存在し、過去の履歴（有効な履歴）が存在する場合、フラグが適切に切り替わりtrueが返ること")
        void success_withHistory() {
            Tokugimu target = new Tokugimu();
            target.setShiteiNo(SHITEI_NO);
            target.setNewFlg("1");

            Tokugimu history = new Tokugimu();
            history.setShiteiNo(SHITEI_NO);
            history.setRno(BigDecimal.ONE);
            history.setNewFlg("0");

            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                    .thenReturn(List.of(target));
            when(tokugimuRepository.findActiveHistoryByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                    .thenReturn(List.of(history));

            boolean result = service.deleteByShiteiNo(SHITEI_NO);

            assertThat(result).isTrue();
            assertThat(target.getDelFlg()).isEqualTo("1");
            assertThat(history.getNewFlg()).isEqualTo("1");
            verify(tokugimuRepository, times(2)).save(any(Tokugimu.class));
        }

        @Test
        @DisplayName("境界値：削除対象はあるが、履歴が他に存在しない場合、delFlgのみ更新されfalseが返ること")
        void boundary_noHistory() {
            Tokugimu target = new Tokugimu();
            target.setShiteiNo(SHITEI_NO);

            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                    .thenReturn(List.of(target));
            when(tokugimuRepository.findActiveHistoryByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                    .thenReturn(List.of());

            boolean result = service.deleteByShiteiNo(SHITEI_NO);

            assertThat(result).isFalse();
            assertThat(target.getDelFlg()).isEqualTo("1");
            verify(tokugimuRepository, times(1)).save(any(Tokugimu.class));
        }

        @Test
        @DisplayName("異常系：削除対象の指定番号が見つからない場合、RuntimeExceptionがスローされること")
        void exception_notFound() {
            String invalidShiteiNo = "99999999";
            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, invalidShiteiNo))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.deleteByShiteiNo(invalidShiteiNo))
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
            TokugimuForm form = new TokugimuForm();
            form.setAtenaNo(1L);
            form.setFacilityName("更新後施設名");
            form.setBusinessStatusFlg(true);

            Tokugimu oldTokugimu = new Tokugimu();
            oldTokugimu.setShiteiNo(SHITEI_NO);
            oldTokugimu.setNewFlg("1");
            oldTokugimu.setRno(BigDecimal.ONE);

            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                    .thenReturn(List.of(oldTokugimu));
            when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                    .thenReturn(Optional.of(1));

            service.updateByShiteiNo(SHITEI_NO, form);

            assertThat(oldTokugimu.getNewFlg()).isEqualTo("0");
            verify(tokugimuRepository, times(2)).save(any(Tokugimu.class));
        }

        @Test
        @DisplayName("正常系：更新時に所有者情報と共同事業者情報が正しく保存されること")
        void update_withOwnerAndKyodoList_success() {
            TokugimuForm form = new TokugimuForm();
            form.setAtenaNo(1L);
            form.setFacilityName("更新後施設名");
            form.setOwnerName("更新後の所有者");
            
            KyodoJigyoshaDto kDto = new KyodoJigyoshaDto();
            kDto.setKyodoName("更新後の共同事業者");
            form.setKyodoList(List.of(kDto));

            Tokugimu oldTokugimu = new Tokugimu();
            oldTokugimu.setShiteiNo(SHITEI_NO);
            oldTokugimu.setNewFlg("1");
            oldTokugimu.setRno(BigDecimal.ONE);

            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                    .thenReturn(List.of(oldTokugimu));
            when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                    .thenReturn(Optional.of(1));

            service.updateByShiteiNo(SHITEI_NO, form);

            verify(shoyushaRepository, times(1)).save(any(Shoyusha.class));
            verify(kyodoJigyoshaRepository, times(1)).save(any(KyodoJigyosha.class));
        }

        @Test
        @DisplayName("異常系：更新対象の指定番号が存在しない場合、RuntimeExceptionがスローされること")
        void exception_notFound() {
            String invalidShiteiNo = "99999999";
            TokugimuForm form = new TokugimuForm();

            when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, invalidShiteiNo))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.updateByShiteiNo(invalidShiteiNo, form))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("特別徴収義務者が見つかりません");
        }
    }
}