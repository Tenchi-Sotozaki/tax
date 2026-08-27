package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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
import org.springframework.data.domain.Page;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuListItem;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
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

    @Mock private TokugimuRepository tokugimuRepository;
    @Mock private AtenaRepository atenaRepository;
    @Mock private GassanRepository gassanRepository;
    @Mock private GassanUchiRepository gassanUchiRepository;
    @Mock private ShoyushaRepository shoyushaRepository;
    @Mock private KyodoJigyoshaRepository kyodoJigyoshaRepository;
    @Mock private JichitaiRepository jichitaiRepository;
    @Mock private FukaRepository fukaRepository;
    @Mock private ShunoRirekiRepository shunoRirekiRepository;
    @Mock private JichitaiContext jichitaiContext;

    @InjectMocks
    private TokugimuServiceImpl tokugimuService;

    @BeforeEach
    void setUp() {
        lenient().when(jichitaiContext.getJichitaiCd()).thenReturn("012345");
    }
    
    @Nested
    @DisplayName("search メソッドのテスト")
    class SearchTest {

        @Test
        @DisplayName("正常系：検索条件が空のとき、自治体コードに合致する全件が取得できること")
        void success_emptySearchForm() {
            TokugimuSearchForm form = new TokugimuSearchForm();
            form.setPage(0);
            form.setPageSize(10);

            Tokugimu tokugimu = new Tokugimu();
            tokugimu.setShiteiNo("00000001");
            tokugimu.setAtenaNo(BigDecimal.ONE);
            tokugimu.setKyokaShu("1");

            when(tokugimuRepository.findAllByJichitaiCd("012345")).thenReturn(List.of(tokugimu));
            when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq("012345"), any())).thenReturn(List.of());
            when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq("012345"), any())).thenReturn(List.of());
            when(fukaRepository.findDeclaredByShiteiNoInOrderByShinkokuYmdDesc(any(), any())).thenReturn(List.of());

            Page<TokugimuListItem> result = tokugimuService.search(form);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getShiteiNo()).isEqualTo("00000001");
        }

        @Test
        @DisplayName("境界値：該当件数が0件のとき、空のページが返却されること")
        void boundary_noResults() {
            TokugimuSearchForm form = new TokugimuSearchForm();
            form.setPage(0);
            form.setPageSize(10);

            when(tokugimuRepository.findAllByJichitaiCd("012345")).thenReturn(List.of());

            Page<TokugimuListItem> result = tokugimuService.search(form);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }
    
    @Nested
    @DisplayName("getTokugimuByShiteiNo メソッドのテスト")
    class GetTokugimuByShiteiNoTest {

        @Test
        @DisplayName("正常系：指定番号に該当するデータが存在する場合、Formにマッピングされて返却されること")
        void success() {
            String shiteiNo = "00000001";
            Tokugimu tokugimu = new Tokugimu();
            tokugimu.setShiteiNo(shiteiNo);
            tokugimu.setAtenaNo(BigDecimal.ONE);
            tokugimu.setRno(BigDecimal.ONE);

            Atena atena = new Atena();
            atena.setAtenaNo(BigDecimal.ONE);
            atena.setName("テスト事業者");

            when(tokugimuRepository.findByJichitaiCdAndShiteiNo("012345", shiteiNo))
                    .thenReturn(List.of(tokugimu));
            when(atenaRepository.findByJichitaiCdAndAtenaNo("012345", BigDecimal.ONE))
                    .thenReturn(Optional.of(atena));
            when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo("012345", shiteiNo))
                    .thenReturn(Optional.of(1));
            when(tokugimuRepository.findMinRnoByJichitaiCdAndShiteiNo("012345", shiteiNo))
                    .thenReturn(Optional.of(1));
            when(shoyushaRepository.findByJichitaiCdAndShiteiNo("012345", shiteiNo))
                    .thenReturn(List.of());
           
            TokugimuForm form = tokugimuService.getTokugimuByShiteiNo(shiteiNo);

            assertThat(form).isNotNull();
            assertThat(form.getShiteiNo()).isEqualTo(shiteiNo);
            assertThat(form.getName()).isEqualTo("テスト事業者");
        }

        @Test
        @DisplayName("異常系：指定番号に該当する宿泊施設が存在しない場合、RuntimeExceptionがスローされること")
        void exception_tokugimuNotFound() {
            String shiteiNo = "99999999";
            when(tokugimuRepository.findByJichitaiCdAndShiteiNo("012345", shiteiNo))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> tokugimuService.getTokugimuByShiteiNo(shiteiNo))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("宿泊施設が見つかりません");
        }
    }
    
    @Nested
    @DisplayName("searchAll メソッドのテスト")
    class SearchAllTest {

        @Test
        @DisplayName("正常系：ページングの制限を無視して全件のリストが取得できること")
        void success() {
            TokugimuSearchForm form = new TokugimuSearchForm();
            Tokugimu tokugimu = new Tokugimu();
            tokugimu.setShiteiNo("00000001");
            tokugimu.setAtenaNo(BigDecimal.ONE);
            tokugimu.setKyokaShu("1");

            // search(all) 内で呼ばれるリポジトリのモック
            when(tokugimuRepository.findAllByJichitaiCd("012345")).thenReturn(List.of(tokugimu));
            when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq("012345"), any())).thenReturn(List.of());
            when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq("012345"), any())).thenReturn(List.of());
            when(fukaRepository.findDeclaredByShiteiNoInOrderByShinkokuYmdDesc(any(), any())).thenReturn(List.of());

            List<TokugimuListItem> result = tokugimuService.searchAll(form);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getShiteiNo()).isEqualTo("00000001");
        }
    }

    @Nested
    @DisplayName("getTokugimuByShiteiNoAndRno メソッドのテスト")
    class GetTokugimuByShiteiNoAndRnoTest {

        @Test
        @DisplayName("正常系：指定番号と履歴番号（rno）に一致するデータが存在する場合、Formが返却されること")
        void success() {
            String shiteiNo = "00000001";
            int rno = 2;
            Tokugimu tokugimu = new Tokugimu();
            tokugimu.setShiteiNo(shiteiNo);
            tokugimu.setAtenaNo(BigDecimal.ONE);
            tokugimu.setRno(BigDecimal.valueOf(rno));

            Atena atena = new Atena();
            atena.setAtenaNo(BigDecimal.ONE);
            atena.setName("テスト事業者");

            when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndRno("012345", shiteiNo, BigDecimal.valueOf(rno)))
                    .thenReturn(Optional.of(tokugimu));
            when(atenaRepository.findByJichitaiCdAndAtenaNo("012345", BigDecimal.ONE))
                    .thenReturn(Optional.of(atena));
            when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo("012345", shiteiNo))
                    .thenReturn(Optional.of(2));
            when(tokugimuRepository.findMinRnoByJichitaiCdAndShiteiNo("012345", shiteiNo))
                    .thenReturn(Optional.of(1));
            when(shoyushaRepository.findByJichitaiCdAndShiteiNo("012345", shiteiNo))
                    .thenReturn(List.of());
           
            TokugimuForm form = tokugimuService.getTokugimuByShiteiNoAndRno(shiteiNo, rno);

            assertThat(form).isNotNull();
            assertThat(form.getShiteiNo()).isEqualTo(shiteiNo);
            assertThat(form.getRno()).isEqualTo(rno);
        }

        @Test
        @DisplayName("異常系：指定した rno のデータが存在しない場合、RuntimeExceptionがスローされること")
        void exception_notFound() {
            String shiteiNo = "00000001";
            int rno = 99;
            when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndRno(any(), any(), any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> tokugimuService.getTokugimuByShiteiNoAndRno(shiteiNo, rno))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("宿泊施設が見つかりません");
        }
    }

    @Nested
    @DisplayName("getShiteiNoById メソッドのテスト")
    class GetShiteiNoByIdTest {

        @Test
        @DisplayName("正常系：ID（宛名番号）に紐づく指定番号が存在する場合、その文字列が返却されること")
        void success() {
            Long id = 1L;
            Tokugimu tokugimu = new Tokugimu();
            tokugimu.setShiteiNo("00000001");

            when(tokugimuRepository.findByJichitaiCdAndAtenaNo("012345", BigDecimal.valueOf(id)))
                    .thenReturn(List.of(tokugimu));

            String shiteiNo = tokugimuService.getShiteiNoById(id);

            assertThat(shiteiNo).isEqualTo("00000001");
        }

        @Test
        @DisplayName("異常系：IDに紐づくデータが存在しない場合、RuntimeExceptionがスローされること")
        void exception_notFound() {
            Long id = 999L;
            when(tokugimuRepository.findByJichitaiCdAndAtenaNo(any(), any()))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> tokugimuService.getShiteiNoById(id))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("指定番号が見つかりません");
        }
    }
}
