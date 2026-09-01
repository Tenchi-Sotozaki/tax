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
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Kyugyobi;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.entity.NokigenId;
import jp.lg.asp.accommodation.repository.HolidayRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.NokigenRepository;
import jp.lg.asp.accommodation.service.impl.NokigenServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NokigenServiceImplTest {

    @Mock NokigenRepository nokigenRepository;
    @Mock HolidayRepository holidayRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks NokigenServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ===== findAll =====

    // No.1 正常系: レコードが複数件存在する場合、jichitaiCdで絞り込んだ全件リストを返す
    @Test
    void findAll_レコードが複数件存在する場合_全件リストを返す() {
        Nokigen n1 = new Nokigen();
        Nokigen n2 = new Nokigen();
        when(nokigenRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(n1, n2));

        List<Nokigen> result = service.findAll();

        assertThat(result).hasSize(2);
    }

    // No.2 正常系: レコードが0件の場合、空リストを返す
    @Test
    void findAll_レコードが0件の場合_空リストを返す() {
        when(nokigenRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());

        List<Nokigen> result = service.findAll();

        assertThat(result).isEmpty();
    }

    // ===== findByNendo =====

    // No.3 正常系: 指定年度のレコードが存在する場合、Nokigenを返す
    @Test
    void findByNendo_指定年度のレコードが存在する場合_Nokigenを返す() {
        Nokigen nokigen = new Nokigen();
        when(nokigenRepository.findById(new NokigenId(JICHITAI_CD, "2024"))).thenReturn(Optional.of(nokigen));

        Nokigen result = service.findByNendo("2024");

        assertThat(result).isEqualTo(nokigen);
    }

    // No.4 正常系: 指定年度のレコードが存在しない場合、nullを返す
    @Test
    void findByNendo_指定年度のレコードが存在しない場合_nullを返す() {
        when(nokigenRepository.findById(any())).thenReturn(Optional.empty());

        Nokigen result = service.findByNendo("2024");

        assertThat(result).isNull();
    }

    // ===== existsByNendo =====

    // No.5 正常系: 指定年度のレコードが存在する場合、trueを返す
    @Test
    void existsByNendo_指定年度のレコードが存在する場合_trueを返す() {
        when(nokigenRepository.countByJichitaiCdAndNendo(JICHITAI_CD, "2024")).thenReturn(1L);

        assertThat(service.existsByNendo("2024")).isTrue();
    }

    // No.6 正常系: 指定年度のレコードが存在しない場合、falseを返す
    @Test
    void existsByNendo_指定年度のレコードが存在しない場合_falseを返す() {
        when(nokigenRepository.countByJichitaiCdAndNendo(JICHITAI_CD, "2024")).thenReturn(0L);

        assertThat(service.existsByNendo("2024")).isFalse();
    }

    // No.7 境界値: countが0より大きい（=1）場合、trueを返す
    @Test
    void existsByNendo_countが1の場合_trueを返す() {
        when(nokigenRepository.countByJichitaiCdAndNendo(any(), any())).thenReturn(1L);

        assertThat(service.existsByNendo("2024")).isTrue();
    }

    // ===== save =====

    // No.8 正常系: 全日付フィールドがyyyy-MM-dd形式の場合、yyyyMMdd形式に変換してsaveが呼ばれる
    @Test
    void save_全日付フィールドがyyyy_MM_dd形式の場合_yyyyMMdd形式に変換してsaveが呼ばれる() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNendo("2024");
        nokigen.setNokigen1st("2024-04-30");
        nokigen.setNokigen2nd("2024-05-31");
        nokigen.setNokigen3rd("2024-06-30");
        nokigen.setNokigen4th("2024-07-31");
        nokigen.setNokigen5th("2024-08-31");
        nokigen.setNokigen6th("2024-09-30");
        nokigen.setNokigen7th("2024-10-31");
        nokigen.setNokigen8th("2024-11-30");
        nokigen.setNokigen9th("2024-12-31");
        nokigen.setNokigen10th("2025-01-31");
        nokigen.setNokigen11th("2025-02-28");
        nokigen.setNokigen12th("2025-03-31");
        when(nokigenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Nokigen result = service.save(nokigen);

        assertThat(result.getNokigen1st()).isEqualTo("20240430");
        assertThat(result.getNokigen2nd()).isEqualTo("20240531");
        assertThat(result.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        verify(nokigenRepository).save(nokigen);
    }

    // No.9 正常系: 日付フィールドがnullの場合、空文字に変換される
    @Test
    void save_日付フィールドがnullの場合_空文字に変換される() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNokigen1st(null);
        nokigen.setNokigen2nd("2024-05-31");
        nokigen.setNokigen3rd("2024-06-30");
        nokigen.setNokigen4th("2024-07-31");
        nokigen.setNokigen5th("2024-08-31");
        nokigen.setNokigen6th("2024-09-30");
        nokigen.setNokigen7th("2024-10-31");
        nokigen.setNokigen8th("2024-11-30");
        nokigen.setNokigen9th("2024-12-31");
        nokigen.setNokigen10th("2025-01-31");
        nokigen.setNokigen11th("2025-02-28");
        nokigen.setNokigen12th("2025-03-31");
        when(nokigenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Nokigen result = service.save(nokigen);

        assertThat(result.getNokigen1st()).isEqualTo("");
    }

    // No.10 正常系: 日付フィールドが空文字の場合、空文字に変換される
    @Test
    void save_日付フィールドが空文字の場合_空文字に変換される() {
        Nokigen nokigen = new Nokigen();
        nokigen.setNokigen1st("");
        nokigen.setNokigen2nd("2024-05-31");
        nokigen.setNokigen3rd("2024-06-30");
        nokigen.setNokigen4th("2024-07-31");
        nokigen.setNokigen5th("2024-08-31");
        nokigen.setNokigen6th("2024-09-30");
        nokigen.setNokigen7th("2024-10-31");
        nokigen.setNokigen8th("2024-11-30");
        nokigen.setNokigen9th("2024-12-31");
        nokigen.setNokigen10th("2025-01-31");
        nokigen.setNokigen11th("2025-02-28");
        nokigen.setNokigen12th("2025-03-31");
        when(nokigenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Nokigen result = service.save(nokigen);

        assertThat(result.getNokigen1st()).isEqualTo("");
    }

    // ===== findJichitai =====

    // No.11 正常系: 自治体情報が存在する場合、Jichitaiを返す
    @Test
    void findJichitai_自治体情報が存在する場合_Jichitaiを返す() {
        Jichitai jichitai = new Jichitai();
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        Jichitai result = service.findJichitai(JICHITAI_CD);

        assertThat(result).isEqualTo(jichitai);
    }

    // No.12 正常系: 自治体情報が存在しない場合、nullを返す
    @Test
    void findJichitai_自治体情報が存在しない場合_nullを返す() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());

        Jichitai result = service.findJichitai(JICHITAI_CD);

        assertThat(result).isNull();
    }

    // ===== getPrevDataWithShift =====

    // No.13 正常系: shiftMode="none"の場合、休業日チェックなしで年度を1年進めた日付を返す
    @Test
    void getPrevDataWithShift_shiftModeがnoneの場合_年度を1年進めた日付を返す() {
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(any(), any())).thenReturn(List.of());
        Nokigen prev = new Nokigen();
        prev.setNokigen1st("20230430");

        var result = service.getPrevDataWithShift(prev, "2024", "none");

        assertThat(result.get("nokigen1st")).isEqualTo("2024-04-30");
    }

    // No.14 正常系: shiftMode="monday"で対象日が土曜の場合、翌月曜日にずらした日付を返す
    @Test
    void getPrevDataWithShift_shiftModeがmondayで土曜の場合_翌月曜日にずらした日付を返す() {
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(any(), any())).thenReturn(List.of());
        Nokigen prev = new Nokigen();
        prev.setNokigen1st("20230427"); // 2024-04-27は土曜

        var result = service.getPrevDataWithShift(prev, "2024", "monday");

        assertThat(result.get("nokigen1st")).isEqualTo("2024-04-29"); // 月曜
    }

    // No.15 正常系: shiftMode="friday"で対象日が土曜の場合、前金曜日にずらした日付を返す
    @Test
    void getPrevDataWithShift_shiftModeがfridayで土曜の場合_前金曜日にずらした日付を返す() {
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(any(), any())).thenReturn(List.of());
        Nokigen prev = new Nokigen();
        prev.setNokigen1st("20230427"); // 2024-04-27は土曜

        var result = service.getPrevDataWithShift(prev, "2024", "friday");

        assertThat(result.get("nokigen1st")).isEqualTo("2024-04-26"); // 金曜
    }

    // No.16 正常系: 対象日が休業日（DB登録）の場合、shiftMode="monday"で翌営業日にずらした日付を返す
    @Test
    void getPrevDataWithShift_対象日が休業日の場合_shiftModeがmondayで翌営業日にずらした日付を返す() {
        Kyugyobi holiday = new Kyugyobi();
        holiday.setKyugyobi(LocalDate.of(2024, 4, 30)); // 2024-04-30が休業日（火曜）
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(JICHITAI_CD, "2024")).thenReturn(List.of(holiday));
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(JICHITAI_CD, "2025")).thenReturn(List.of());
        Nokigen prev = new Nokigen();
        prev.setNokigen1st("20230430");

        var result = service.getPrevDataWithShift(prev, "2024", "monday");

        assertThat(result.get("nokigen1st")).isEqualTo("2024-05-01"); // 翌営業日
    }

    // No.17 正常系: 月が1〜3月の場合、年度+1年の日付を返す
    @Test
    void getPrevDataWithShift_月が1月の場合_年度プラス1年の日付を返す() {
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(any(), any())).thenReturn(List.of());
        Nokigen prev = new Nokigen();
        prev.setNokigen1st("20230131"); // 1月

        var result = service.getPrevDataWithShift(prev, "2024", "none");

        assertThat(result.get("nokigen1st")).isEqualTo("2025-01-31"); // 2024+1=2025年
    }

    // No.18 正常系: 月が4月以降の場合、年度そのままの日付を返す
    @Test
    void getPrevDataWithShift_月が4月の場合_年度そのままの日付を返す() {
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(any(), any())).thenReturn(List.of());
        Nokigen prev = new Nokigen();
        prev.setNokigen1st("20230430"); // 4月

        var result = service.getPrevDataWithShift(prev, "2024", "none");

        assertThat(result.get("nokigen1st")).isEqualTo("2024-04-30"); // 2024年
    }

    // No.19 正常系: 自治体の休業日が未登録の場合、番兵レコード（jichitaiCd="99999"）の休業日を使用する
    @Test
    void getPrevDataWithShift_自治体の休業日が未登録の場合_番兵レコードの休業日を使用する() {
        Kyugyobi sentinelHoliday = new Kyugyobi();
        sentinelHoliday.setKyugyobi(LocalDate.of(2024, 4, 30)); // 番兵の休業日（火曜）
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(JICHITAI_CD, "2024")).thenReturn(List.of());
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi("99999", "2024")).thenReturn(List.of(sentinelHoliday));
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(JICHITAI_CD, "2025")).thenReturn(List.of());
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi("99999", "2025")).thenReturn(List.of());
        Nokigen prev = new Nokigen();
        prev.setNokigen1st("20230430");

        var result = service.getPrevDataWithShift(prev, "2024", "monday");

        assertThat(result.get("nokigen1st")).isEqualTo("2024-05-01"); // 番兵の休業日を考慮してずらした日付
    }

    // No.20 異常系: prevのnokigen1stがnullの場合、空文字を返す
    @Test
    void getPrevDataWithShift_nokigen1stがnullの場合_空文字を返す() {
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(any(), any())).thenReturn(List.of());
        Nokigen prev = new Nokigen();
        prev.setNokigen1st(null);

        var result = service.getPrevDataWithShift(prev, "2024", "none");

        assertThat(result.get("nokigen1st")).isEqualTo("");
    }

    // No.21 異常系: prevのnokigen1stが8桁以外の場合、空文字を返す
    @Test
    void getPrevDataWithShift_nokigen1stが8桁以外の場合_空文字を返す() {
        when(holidayRepository.findByJichitaiCdAndNenOrderByKyugyobi(any(), any())).thenReturn(List.of());
        Nokigen prev = new Nokigen();
        prev.setNokigen1st("2023043"); // 7桁

        var result = service.getPrevDataWithShift(prev, "2024", "none");

        assertThat(result.get("nokigen1st")).isEqualTo("");
    }
}
