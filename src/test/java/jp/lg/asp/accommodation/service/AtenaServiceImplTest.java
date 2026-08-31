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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException;

import jp.lg.asp.accommodation.dto.AtenaDaichoItem;
import jp.lg.asp.accommodation.dto.AtenaSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.impl.AtenaServiceImpl;
import jp.lg.asp.accommodation.util.HashUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtenaServiceImplTest {

    @Mock
    private JichitaiRepository jichitaiRepository;

    @Mock
    private AtenaRepository atenaRepository;

    @Mock
    private HashUtil hashUtil;

    @InjectMocks
    private AtenaServiceImpl atenaService;

    private static final String JICHITAI_CD = "011002";

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
    @DisplayName("searchDaicho メソッドのテスト")
    class SearchDaichoTest {

        @Test
        @DisplayName("正常系：searchedがfalseの場合、リポジトリを呼び出さずに空のリストを返却すること")
        void searchDaicho_searchedFalse_returnsEmptyList() {
            AtenaSearchForm searchForm = new AtenaSearchForm();

            List<AtenaDaichoItem> result = atenaService.searchDaicho(JICHITAI_CD, searchForm, false);

            assertThat(result).isEmpty();
            verify(jichitaiRepository, never()).findById(any());
            verify(atenaRepository, never()).search(any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("正常系：searchedがtrueで、指定した自治体が存在し、検索条件に一致する宛名データが複数件取得できること")
        void searchDaicho_searchedTrue_returnsItems() {
            AtenaSearchForm searchForm = new AtenaSearchForm();
            searchForm.setAtenaNo("123");
            searchForm.setName("山田");
            searchForm.setNameMatchType("prefix");

            Jichitai jichitai = new Jichitai();
            jichitai.setAtenaStNo(BigDecimal.valueOf(100));

            Atena atena1 = new Atena();
            atena1.setAtenaNo(BigDecimal.ONE);
            Atena atena2 = new Atena();
            atena2.setAtenaNo(BigDecimal.valueOf(2));

            when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
            when(atenaRepository.search(
                    eq(JICHITAI_CD),
                    eq("123"),
                    eq("山田%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%")
            )).thenReturn(List.of(atena1, atena2));

            List<AtenaDaichoItem> result = atenaService.searchDaicho(JICHITAI_CD, searchForm, true);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("正常系：個人番号が指定されている場合、hashUtil.sha256でハッシュ化されて検索条件に渡されること")
        void searchDaicho_withKojinNo_hashesAndCallsRepository() {
            AtenaSearchForm searchForm = new AtenaSearchForm();
            String rawKojinNo = "123456789012";
            searchForm.setKojinNo(rawKojinNo);

            String hashedKojinNo = sha256Hex(rawKojinNo);

            Jichitai jichitai = new Jichitai();
            jichitai.setAtenaStNo(BigDecimal.valueOf(100));

            Atena atena = new Atena();
            atena.setAtenaNo(BigDecimal.ONE);

            when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
            when(hashUtil.sha256(rawKojinNo)).thenReturn(hashedKojinNo);
            when(atenaRepository.search(
                    eq(JICHITAI_CD),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq(hashedKojinNo),
                    eq("%")
            )).thenReturn(List.of(atena));

            List<AtenaDaichoItem> result = atenaService.searchDaicho(JICHITAI_CD, searchForm, true);

            assertThat(result).hasSize(1);
            verify(hashUtil, times(1)).sha256(rawKojinNo);
        }

        @Test
        @DisplayName("境界値：自治体マスタが存在しない場合、atenaStNoにnullが設定されて検索結果が返却されること")
        void searchDaicho_jichitaiNotFound_returnsItemsWithNullStNo() {
            AtenaSearchForm searchForm = new AtenaSearchForm();

            Atena atena = new Atena();
            atena.setAtenaNo(BigDecimal.ONE);

            when(jichitaiRepository.findById("999999")).thenReturn(Optional.empty());
            when(atenaRepository.search(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of(atena));

            List<AtenaDaichoItem> result = atenaService.searchDaicho("999999", searchForm, true);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("境界値：検索フォームの各項目がnull、空文字、空白のみの場合、toLikePatternにより'%'に変換されること")
        void searchDaicho_blankOrNullValues_convertedToPercent() {
            AtenaSearchForm searchForm = new AtenaSearchForm();
            searchForm.setName(null);
            searchForm.setNameKana("");
            searchForm.setYubinNo("   ");

            Jichitai jichitai = new Jichitai();
            jichitai.setAtenaStNo(BigDecimal.valueOf(100));

            when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
            when(atenaRepository.search(
                    eq(JICHITAI_CD),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%")
            )).thenReturn(List.of());

            List<AtenaDaichoItem> result = atenaService.searchDaicho(JICHITAI_CD, searchForm, true);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("境界値：個人番号がnull、空文字、空白のみの場合、hashUtilが呼び出されずにnullとして扱われること")
        void searchDaicho_blankKojinNo_skipsHashing() {
            AtenaSearchForm searchForm = new AtenaSearchForm();
            searchForm.setKojinNo("   ");

            Jichitai jichitai = new Jichitai();
            jichitai.setAtenaStNo(BigDecimal.valueOf(100));

            when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
            when(atenaRepository.search(any(), any(), any(), any(), any(), any(), any(), isNull(), any()))
                    .thenReturn(List.of());

            List<AtenaDaichoItem> result = atenaService.searchDaicho(JICHITAI_CD, searchForm, true);

            assertThat(result).isEmpty();
            verify(hashUtil, never()).sha256(any());
        }

        @Test
        @DisplayName("境界値：matchTypeが想定外の値の場合、defaultの部分一致（'%value%'）として扱われること")
        void searchDaicho_unknownMatchType_defaultsToPartial() {
            AtenaSearchForm searchForm = new AtenaSearchForm();
            searchForm.setName("テスト");
            searchForm.setNameMatchType("unknown");

            Jichitai jichitai = new Jichitai();
            jichitai.setAtenaStNo(BigDecimal.valueOf(100));

            when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
            when(atenaRepository.search(
                    eq(JICHITAI_CD),
                    eq("%"),
                    eq("%テスト%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%"),
                    eq("%")
            )).thenReturn(List.of());

            List<AtenaDaichoItem> result = atenaService.searchDaicho(JICHITAI_CD, searchForm, true);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("異常系：jichitaiRepository.findByIdの実行時に例外が発生した場合に例外がスローされること")
        void searchDaicho_jichitaiRepositoryThrowsException_throwsException() {
            AtenaSearchForm searchForm = new AtenaSearchForm();

            when(jichitaiRepository.findById(JICHITAI_CD)).thenThrow(new DataAccessException("DB Error") {});

            assertThatThrownBy(() -> atenaService.searchDaicho(JICHITAI_CD, searchForm, true))
                    .isInstanceOf(DataAccessException.class);
        }

        @Test
        @DisplayName("異常系：atenaRepository.searchの実行時に例外が発生した場合に例外がスローされること")
        void searchDaicho_atenaRepositoryThrowsException_throwsException() {
            AtenaSearchForm searchForm = new AtenaSearchForm();
            Jichitai jichitai = new Jichitai();

            when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
            when(atenaRepository.search(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new DataAccessException("DB Error") {});

            assertThatThrownBy(() -> atenaService.searchDaicho(JICHITAI_CD, searchForm, true))
                    .isInstanceOf(DataAccessException.class);
        }
    }
}