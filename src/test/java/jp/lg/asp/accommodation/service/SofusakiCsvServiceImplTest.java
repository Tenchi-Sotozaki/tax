package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.SofusakiCsvDto;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.SofusakiCsvRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.SofusakiCsvServiceImpl;

@ExtendWith(MockitoExtension.class)
class SofusakiCsvServiceImplTest {

    @Mock SofusakiCsvRepository sofusakiCsvRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks SofusakiCsvServiceImpl service;

    private SofusakiCsvDto dto() {
        SofusakiCsvDto d = new SofusakiCsvDto();
        d.setAtenaNo(new BigDecimal("1"));
        d.setShiteiNo("0001");
        d.setSoufusakiName("山田太郎");
        d.setSoufusakiNameKana("ヤマダタロウ");
        d.setSoufusakiYubinNo("100-0001");
        d.setSoufusakiJusho("東京都千代田区");
        d.setSoufusakiTel("03-1234-5678");
        return d;
    }

    // ── toCsvString ───────────────────────────────────────────────

    @Test
    void toCsvString_ヘッダー行が正しく出力される() {
        String result = service.toCsvString(List.of());

        assertThat(result).startsWith("宛名番号,指定番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\n");
    }

    @Test
    void toCsvString_通常データが正しいCSV形式で出力される() {
        SofusakiCsvDto d = dto();

        String result = service.toCsvString(List.of(d));

        // ヘッダー除いたデータ行を検証
        String dataLine = result.lines().skip(1).findFirst().orElse("");
        assertThat(dataLine).contains("山田太郎");
        assertThat(dataLine).contains("ヤマダタロウ");
        assertThat(dataLine).contains("100-0001");
        assertThat(dataLine).contains("東京都千代田区");
        assertThat(dataLine).contains("03-1234-5678");
    }

    @Test
    void toCsvString_カンマを含むフィールドはダブルクォートで囲まれる() {
        SofusakiCsvDto d = new SofusakiCsvDto();
        d.setSoufusakiJusho("東京都,千代田区");

        String result = service.toCsvString(List.of(d));

        assertThat(result).contains("\"東京都,千代田区\"");
    }

    @Test
    void toCsvString_ダブルクォートを含むフィールドはエスケープされる() {
        SofusakiCsvDto d = new SofusakiCsvDto();
        d.setSoufusakiName("山田\"太郎");

        String result = service.toCsvString(List.of(d));

        assertThat(result).contains("\"山田\"\"太郎\"");
    }

    @Test
    void toCsvString_atenaNoとshiteiNoはExcel数値化防止のため等号付きクォートで出力される() {
        SofusakiCsvDto d = new SofusakiCsvDto();
        d.setAtenaNo(new BigDecimal("12345"));
        d.setShiteiNo("0001");

        String result = service.toCsvString(List.of(d));

        assertThat(result).contains("=\"12345\"");
        assertThat(result).contains("=\"0001\"");
    }

    @Test
    void toCsvString_nullフィールドは空文字として出力される() {
        SofusakiCsvDto d = new SofusakiCsvDto();
        d.setAtenaNo(null);
        d.setShiteiNo(null);
        d.setSoufusakiName(null);
        d.setSoufusakiTel(null);

        String result = service.toCsvString(List.of(d));

        // データ行がカンマ区切りで存在し、nullによる例外が発生しないこと
        String dataLine = result.lines().skip(1).findFirst().orElse("");
        assertThat(dataLine).isNotNull();
        // atenaNo=null → 空文字、shiteiNo=null → 空文字
        assertThat(dataLine).startsWith(",");
    }
}
