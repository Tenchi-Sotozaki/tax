package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
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
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;
import jp.lg.asp.accommodation.entity.AtenaRenkei;
import jp.lg.asp.accommodation.repository.AtenaRenkeiRepository;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.service.impl.AtenaImportServiceImpl;
import jp.lg.asp.accommodation.util.HashUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtenaImportServiceImplTest {

    @Mock AtenaRepository atenaRepository;
    @Mock AtenaRenkeiRepository atenaRenkeiRepository;
    @Mock HashUtil hashUtil;
    @Mock MultipartFile file;

    @InjectMocks AtenaImportServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String USER_ID = "user01";
    private static final String VALID_HEADER = "宛名番号,個人番号,法人番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\n";

    @BeforeEach
    void setUp() {
        when(atenaRenkeiRepository.findMaxSeqByJichitaiCd(JICHITAI_CD)).thenReturn(BigDecimal.ZERO);
        when(atenaRenkeiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void importCsv_新規登録() throws Exception {
        String csv = VALID_HEADER + "1001,,, テスト太郎,テストタロウ,060-0001,札幌市,011-111-1111\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));
        when(file.getOriginalFilename()).thenReturn("test.csv");
        when(atenaRepository.existsById(any(AtenaId.class))).thenReturn(false);
        when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.empty());
        when(atenaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AtenaRenkei result = service.importCsv(file, JICHITAI_CD, USER_ID);

        assertThat(result.getShinkiKensu()).isEqualTo(BigDecimal.valueOf(1));
        assertThat(result.getKoshinKensu()).isEqualTo(BigDecimal.valueOf(0));
    }

    @Test
    void importCsv_更新登録() throws Exception {
        String csv = VALID_HEADER + "1001,,,テスト太郎,テストタロウ,060-0001,札幌市,011-111-1111\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));
        when(file.getOriginalFilename()).thenReturn("test.csv");
        when(atenaRepository.existsById(any(AtenaId.class))).thenReturn(true);
        when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.of(new Atena()));
        when(atenaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AtenaRenkei result = service.importCsv(file, JICHITAI_CD, USER_ID);

        assertThat(result.getShinkiKensu()).isEqualTo(BigDecimal.valueOf(0));
        assertThat(result.getKoshinKensu()).isEqualTo(BigDecimal.valueOf(1));
    }

    @Test
    void importCsv_空ファイルは例外() throws Exception {
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        assertThatThrownBy(() -> service.importCsv(file, JICHITAI_CD, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CSVファイルが空");
    }

    @Test
    void importCsv_ヘッダー不正は例外() throws Exception {
        String csv = "不正ヘッダー,col2,col3,col4,col5,col6,col7,col8\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));

        assertThatThrownBy(() -> service.importCsv(file, JICHITAI_CD, USER_ID))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void importCsv_データ行の宛名番号空は例外() throws Exception {
        String csv = VALID_HEADER + ",,,テスト太郎,テストタロウ,060-0001,札幌市,011-111-1111\n";
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes("UTF-8")));

        assertThatThrownBy(() -> service.importCsv(file, JICHITAI_CD, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("宛名番号");
    }

    @Test
    void findHistory_履歴一覧取得() {
        List<AtenaRenkei> expected = List.of(new AtenaRenkei());
        when(atenaRenkeiRepository.findByJichitaiCdOrderBySeqDesc(JICHITAI_CD)).thenReturn(expected);

        List<AtenaRenkei> result = service.findHistory(JICHITAI_CD);

        assertThat(result).isEqualTo(expected);
    }
}
