package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;
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
import jp.lg.asp.accommodation.dto.JichitaiConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.impl.JichitaiConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JichitaiConfigServiceImplTest {

    @Mock JichitaiRepository jichitaiRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks JichitaiConfigServiceImpl service;

    private static final String JICHITAI_CD = "011002";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ===== findById =====

    // No.1 正常系: 自治体コードが存在する場合、Optional.of(Jichitai)を返す
    @Test
    void findById_自治体コードが存在する場合_OptionalOfJichitaiを返す() {
        Jichitai jichitai = new Jichitai();
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        Optional<Jichitai> result = service.findById(JICHITAI_CD);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(jichitai);
    }

    // No.2 正常系: 自治体コードが存在しない場合、Optional.emptyを返す
    @Test
    void findById_自治体コードが存在しない場合_OptionalEmptyを返す() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());

        Optional<Jichitai> result = service.findById(JICHITAI_CD);

        assertThat(result).isEmpty();
    }

    // ===== getCurrentJichitai =====

    // No.3 正常系: 自治体情報が存在する場合、Jichitaiを返す
    @Test
    void getCurrentJichitai_自治体情報が存在する場合_Jichitaiを返す() {
        Jichitai jichitai = new Jichitai();
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        Jichitai result = service.getCurrentJichitai();

        assertThat(result).isEqualTo(jichitai);
    }

    // No.4 異常系: 自治体情報が存在しない場合、例外をスローする
    @Test
    void getCurrentJichitai_自治体情報が存在しない場合_例外をスローする() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentJichitai())
                .isInstanceOf(NoSuchElementException.class);
    }

    // ===== getJichitaiConfigDto =====

    // No.5 正常系: 自治体情報が存在する場合、全フィールドが設定されたDtoを返す
    @Test
    void getJichitaiConfigDto_自治体情報が存在する場合_全フィールドが設定されたDtoを返す() {
        Jichitai jichitai = new Jichitai();
        jichitai.setJichitaiCd(JICHITAI_CD);
        jichitai.setName("札幌市");
        jichitai.setKbnName("市");
        jichitai.setNendoStMonth("4");
        jichitai.setNozeiShuki("1");
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        JichitaiConfigDto result = service.getJichitaiConfigDto();

        assertThat(result.getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(result.getName()).isEqualTo("札幌市");
        assertThat(result.getKbnName()).isEqualTo("市");
        assertThat(result.getNendoStMonth()).isEqualTo("4");
    }

    // No.6 正常系: 自治体のnendoStMonthがnullの場合、デフォルト値"3"が設定される
    @Test
    void getJichitaiConfigDto_nendoStMonthがnullの場合_デフォルト値3が設定される() {
        Jichitai jichitai = new Jichitai();
        jichitai.setJichitaiCd(JICHITAI_CD);
        jichitai.setNendoStMonth(null);
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        JichitaiConfigDto result = service.getJichitaiConfigDto();

        assertThat(result.getNendoStMonth()).isEqualTo("3");
    }

    // No.7 正常系: 自治体情報が存在しない場合、nendoStMonth="3"のデフォルトDtoを返す
    @Test
    void getJichitaiConfigDto_自治体情報が存在しない場合_デフォルトDtoを返す() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());

        JichitaiConfigDto result = service.getJichitaiConfigDto();

        assertThat(result.getNendoStMonth()).isEqualTo("3");
        assertThat(result.getJichitaiCd()).isNull();
    }

    // No.8 異常系: jichitaiCdがnullの場合、nendoStMonth="3"のデフォルトDtoを返す
    @Test
    void getJichitaiConfigDto_jichitaiCdがnullの場合_デフォルトDtoを返す() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(null);

        JichitaiConfigDto result = service.getJichitaiConfigDto();

        assertThat(result.getNendoStMonth()).isEqualTo("3");
        assertThat(result.getJichitaiCd()).isNull();
    }

    // ===== save =====

    // No.9 正常系: 自治体コードが変更されない場合、既存レコードを更新してsaveが呼ばれる
    @Test
    void save_自治体コードが変更されない場合_既存レコードを更新してsaveが呼ばれる() {
        Jichitai jichitai = new Jichitai();
        jichitai.setJichitaiCd(JICHITAI_CD);
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        JichitaiConfigDto form = new JichitaiConfigDto();
        form.setJichitaiCd(JICHITAI_CD);
        form.setName("札幌市");

        service.save(JICHITAI_CD, form);

        verify(jichitaiRepository).save(jichitai);
        verify(jichitaiRepository, never()).delete(any());
    }

    // No.10 正常系: 自治体コードが変更された場合、新レコードをsaveして旧レコードをdeleteする
    @Test
    void save_自治体コードが変更された場合_新レコードをsaveして旧レコードをdeleteする() {
        Jichitai jichitai = new Jichitai();
        jichitai.setJichitaiCd(JICHITAI_CD);
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        JichitaiConfigDto form = new JichitaiConfigDto();
        form.setJichitaiCd("011003");
        form.setName("札幌市");

        service.save(JICHITAI_CD, form);

        verify(jichitaiRepository).save(argThat(j -> "011003".equals(j.getJichitaiCd())));
        verify(jichitaiRepository).delete(jichitai);
    }

    // No.11 異常系: 自治体コードが存在しない場合、例外をスローする
    @Test
    void save_自治体コードが存在しない場合_例外をスローする() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());

        JichitaiConfigDto form = new JichitaiConfigDto();
        form.setJichitaiCd(JICHITAI_CD);

        assertThatThrownBy(() -> service.save(JICHITAI_CD, form))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ===== saveJichitaiConfig =====

    // No.12 正常系: 自治体コードが既存の場合、既存レコードを更新してsaveが呼ばれる
    @Test
    void saveJichitaiConfig_自治体コードが既存の場合_既存レコードを更新してsaveが呼ばれる() {
        Jichitai jichitai = new Jichitai();
        jichitai.setJichitaiCd(JICHITAI_CD);
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));

        JichitaiConfigDto form = new JichitaiConfigDto();
        form.setJichitaiCd(JICHITAI_CD);
        form.setName("札幌市");
        form.setUserId("user01");

        service.saveJichitaiConfig(form);

        verify(jichitaiRepository).save(jichitai);
        assertThat(jichitai.getName()).isEqualTo("札幌市");
        assertThat(jichitai.getUserName()).isEqualTo("user01");
    }

    // No.13 正常系: 自治体コードが存在しない場合、新規レコードを作成してsaveが呼ばれる
    @Test
    void saveJichitaiConfig_自治体コードが存在しない場合_新規レコードを作成してsaveが呼ばれる() {
        when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());

        JichitaiConfigDto form = new JichitaiConfigDto();
        form.setJichitaiCd(JICHITAI_CD);
        form.setName("札幌市");
        form.setUserId("user01");

        service.saveJichitaiConfig(form);

        verify(jichitaiRepository).save(argThat(j -> JICHITAI_CD.equals(j.getJichitaiCd())));
    }
}
