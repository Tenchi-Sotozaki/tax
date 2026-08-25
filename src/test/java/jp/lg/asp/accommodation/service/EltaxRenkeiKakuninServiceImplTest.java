package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.repository.EltaxRenkeiRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.NozeiShukiRepository;
import jp.lg.asp.accommodation.repository.ShoyushaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.impl.EltaxRenkeiKakuninServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EltaxRenkeiKakuninServiceImplTest {

    @Mock EltaxRenkeiRepository eltaxRenkeiRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock ShoyushaRepository shoyushaRepository;
    @Mock NozeiShukiRepository nozeiShukiRepository;
    @Mock FukaRepository fukaRepository;
    @Mock FukaUchiRepository fukaUchiRepository;
    @Mock ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
    @Mock ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock JichitaiContext jichitaiContext;
    @Mock MultipartFile file;

    @InjectMocks EltaxRenkeiKakuninServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    @Test
    void preview_空ファイルは例外() throws Exception {
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(file.getOriginalFilename()).thenReturn("test.csv");

        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ファイルの解析に失敗しました");
    }

    @Test
    void preview_不明な手続IDは例外() throws Exception {
        // 手続IDが不明な場合、様式マップが空になりshubetsuが空になる
        String csv = "col1,col2,UNKNOWN_ID,col4\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));
        when(file.getOriginalFilename()).thenReturn("test.csv");

        // shubetsuが空の場合、施設番号インデックスが-1になりshiteiNoが空になる
        // isTokugimuNewがfalseかつshiteiNoが空なので例外が発生する
        assertThatThrownBy(() -> service.preview(file))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void commit_空バイト配列は正常終了() {
        when(eltaxRenkeiRepository.findNextSeq(JICHITAI_CD)).thenReturn(BigDecimal.ONE);
        when(eltaxRenkeiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> service.commit(new byte[0], "test.csv", null, null))
                .doesNotThrowAnyException();

        verify(eltaxRenkeiRepository).save(any(EltaxRenkei.class));
    }

    @Test
    void commit_eLTAX連携管理が保存される() {
        when(eltaxRenkeiRepository.findNextSeq(JICHITAI_CD)).thenReturn(BigDecimal.valueOf(5));
        when(eltaxRenkeiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.commit(new byte[0], "upload.csv", null, null);

        verify(eltaxRenkeiRepository).save(argThat(e ->
                "011002".equals(e.getJichitaiCd()) &&
                "upload.csv".equals(e.getFileName()) &&
                "1".equals(e.getShoriKekka())));
    }
}
