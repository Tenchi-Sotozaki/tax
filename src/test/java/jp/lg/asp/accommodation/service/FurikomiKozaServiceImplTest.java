package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.FurikomiKozaDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.FurikomiKoza;
import jp.lg.asp.accommodation.entity.FurikomiKozaId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FurikomiKozaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.FurikomiKozaServiceImpl;

@ExtendWith(MockitoExtension.class)
class FurikomiKozaServiceImplTest {

    @Mock FurikomiKozaRepository furikomiKozaRepository;
    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks FurikomiKozaServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    // ===== getFurikomiKoza =====

    // No.1 正常系: 振込先口座・特別徴収義務者・宛名が存在する場合、viewモードで表示される
    @Test
    void getFurikomiKoza_振込先口座と特別徴収義務者と宛名が存在する場合_viewモードで返す() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShisetsuName("テスト施設");
        tokugimu.setAtenaNo(BigDecimal.ONE);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
                .thenReturn(Optional.of(tokugimu));

        Atena atena = new Atena();
        atena.setName("テスト事業者");
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
                .thenReturn(Optional.of(atena));

        FurikomiKoza koza = new FurikomiKoza();
        koza.setBankCd("0001");
        koza.setVersion(1);
        when(furikomiKozaRepository.findById(new FurikomiKozaId(JICHITAI_CD, SHITEI_NO)))
                .thenReturn(Optional.of(koza));

        FurikomiKozaDto result = service.getFurikomiKoza(SHITEI_NO);

        assertThat(result.getMode()).isEqualTo("view");
        assertThat(result.isExists()).isTrue();
        assertThat(result.getBankCd()).isEqualTo("0001");
        assertThat(result.getShisetsuName()).isEqualTo("テスト施設");
        assertThat(result.getName()).isEqualTo("テスト事業者");
        assertThat(result.getVersion()).isEqualTo(1);
    }

    // No.2 正常系: 振込先口座なしの場合、createモードで表示される
    @Test
    void getFurikomiKoza_振込先口座なしの場合_createモードで返す() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.empty());

        FurikomiKozaDto result = service.getFurikomiKoza(SHITEI_NO);

        assertThat(result.getMode()).isEqualTo("create");
        assertThat(result.isExists()).isFalse();
        assertThat(result.getBankCd()).isNull();
    }

    // No.3 異常系: 特別徴収義務者が存在せず口座がある場合、shisetsuName・nameはnull
    @Test
    void getFurikomiKoza_特別徴収義務者なしで口座がある場合_shisetsuNameとnameがnull() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        FurikomiKoza koza = new FurikomiKoza();
        koza.setBankCd("0001");
        koza.setVersion(2);
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.of(koza));

        FurikomiKozaDto result = service.getFurikomiKoza(SHITEI_NO);

        assertThat(result.getShisetsuName()).isNull();
        assertThat(result.getName()).isNull();
        assertThat(result.getBankCd()).isEqualTo("0001");
    }

    // No.4 異常系: 特別徴収義務者が存在して宛名が存在しない場合、nameはnull
    @Test
    void getFurikomiKoza_特別徴収義務者ありで宛名なしの場合_nameがnull() {
        Tokugimu tokugimu = new Tokugimu();
        tokugimu.setShisetsuName("テスト施設");
        tokugimu.setAtenaNo(BigDecimal.valueOf(99));
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.of(tokugimu));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.empty());

        FurikomiKoza koza = new FurikomiKoza();
        koza.setBankCd("0001");
        koza.setVersion(1);
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.of(koza));

        FurikomiKozaDto result = service.getFurikomiKoza(SHITEI_NO);

        assertThat(result.getShisetsuName()).isEqualTo("テスト施設");
        assertThat(result.getName()).isNull();
    }

    // No.5 正常系: shiteiNo(指定番号)がDTOに正しくセットされる
    @Test
    void getFurikomiKoza_shiteiNoがDTOに正しくセットされる() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.empty());

        FurikomiKozaDto result = service.getFurikomiKoza("12345678");

        assertThat(result.getShiteiNo()).isEqualTo("12345678");
    }

    // ===== createFurikomiKoza =====

    // No.6 正常系: 全フィールドを正常に入力して登録した場合、viewモード・version=1で返す
    @Test
    void createFurikomiKoza_全フィールド正常登録_viewモードとversion1で返す() {
        when(furikomiKozaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setBankCd("0001");
        dto.setBankName("テスト銀行");
        dto.setBranchCd("001");
        dto.setBranchName("本店");
        dto.setShumoku("1");
        dto.setKozaNo("1234567");
        dto.setMeigi("テスト名義");

        FurikomiKozaDto result = service.createFurikomiKoza(dto);

        assertThat(result.getMode()).isEqualTo("view");
        assertThat(result.isExists()).isTrue();
        assertThat(result.getVersion()).isEqualTo(1);
    }

    // No.7 正常系: saveに渡るエンティティのjichitaiCd・shiteiNoが正しい
    @Test
    void createFurikomiKoza_saveに渡るエンティティのjichitaiCdとshiteiNoが正しい() {
        ArgumentCaptor<FurikomiKoza> captor = ArgumentCaptor.forClass(FurikomiKoza.class);
        when(furikomiKozaRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);

        service.createFurikomiKoza(dto);

        assertThat(captor.getValue().getJichitaiCd()).isEqualTo(JICHITAI_CD);
        assertThat(captor.getValue().getShiteiNo()).isEqualTo(SHITEI_NO);
    }

    // No.8 正常系: saveに渡るエンティティの全フィールドがDTOと一致する
    @Test
    void createFurikomiKoza_saveに渡るエンティティの全フィールドがDTOと一致する() {
        ArgumentCaptor<FurikomiKoza> captor = ArgumentCaptor.forClass(FurikomiKoza.class);
        when(furikomiKozaRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setBankCd("0001");
        dto.setBankName("テスト銀行");
        dto.setBranchCd("001");
        dto.setBranchName("本店");
        dto.setShumoku("1");
        dto.setKozaNo("1234567");
        dto.setMeigi("テスト名義");

        service.createFurikomiKoza(dto);

        FurikomiKoza saved = captor.getValue();
        assertThat(saved.getBankCd()).isEqualTo(dto.getBankCd());
        assertThat(saved.getBankName()).isEqualTo(dto.getBankName());
        assertThat(saved.getBranchCd()).isEqualTo(dto.getBranchCd());
        assertThat(saved.getBranchName()).isEqualTo(dto.getBranchName());
        assertThat(saved.getShumoku()).isEqualTo(dto.getShumoku());
        assertThat(saved.getKozaNo()).isEqualTo(dto.getKozaNo());
        assertThat(saved.getMeigi()).isEqualTo(dto.getMeigi());
    }

    // ===== updateFurikomiKoza =====

    // No.9 正常系: version一致する場合、全フィールドを更新してviewモードで返す
    @Test
    void updateFurikomiKoza_version一致する場合_全フィールド更新してviewモードで返す() {
        FurikomiKoza existing = new FurikomiKoza();
        existing.setVersion(1);
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.of(existing));
        when(furikomiKozaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setVersion(1);
        dto.setBankCd("0002");
        dto.setBankName("新銀行");
        dto.setBranchCd("002");
        dto.setBranchName("新支店");
        dto.setShumoku("2");
        dto.setKozaNo("7654321");
        dto.setMeigi("新名義");

        FurikomiKozaDto result = service.updateFurikomiKoza(dto);

        assertThat(result.getMode()).isEqualTo("view");
        assertThat(existing.getBankCd()).isEqualTo("0002");
        assertThat(existing.getBankName()).isEqualTo("新銀行");
        assertThat(existing.getBranchCd()).isEqualTo("002");
        assertThat(existing.getBranchName()).isEqualTo("新支店");
        assertThat(existing.getShumoku()).isEqualTo("2");
        assertThat(existing.getKozaNo()).isEqualTo("7654321");
        assertThat(existing.getMeigi()).isEqualTo("新名義");
    }

    // No.10 正常系: 更新後のdto.versionがexistingのversionを引き継ぐ
    @Test
    void updateFurikomiKoza_更新後のdtoVersionがexistingのversionを引き継ぐ() {
        FurikomiKoza existing = new FurikomiKoza();
        existing.setVersion(3);
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.of(existing));
        when(furikomiKozaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setVersion(3);

        FurikomiKozaDto result = service.updateFurikomiKoza(dto);

        assertThat(result.getVersion()).isEqualTo(3);
    }

    // No.11 正常系: saveが1回呼ばれる
    @Test
    void updateFurikomiKoza_saveが1回呼ばれる() {
        FurikomiKoza existing = new FurikomiKoza();
        existing.setVersion(1);
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.of(existing));
        when(furikomiKozaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setVersion(1);

        service.updateFurikomiKoza(dto);

        verify(furikomiKozaRepository, times(1)).save(existing);
    }

    // No.12 境界値: version=0で一致、レコードが正常に更新される
    @Test
    void updateFurikomiKoza_version0で一致する場合_正常に更新される() {
        FurikomiKoza existing = new FurikomiKoza();
        existing.setVersion(0);
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.of(existing));
        when(furikomiKozaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setVersion(0);

        FurikomiKozaDto result = service.updateFurikomiKoza(dto);

        assertThat(result.getMode()).isEqualTo("view");
    }

    // No.13 境界値: version=Integer.MAX_VALUEで一致、レコードが正常に更新される
    @Test
    void updateFurikomiKoza_versionMaxValueで一致する場合_正常に更新される() {
        FurikomiKoza existing = new FurikomiKoza();
        existing.setVersion(Integer.MAX_VALUE);
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.of(existing));
        when(furikomiKozaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setVersion(Integer.MAX_VALUE);

        FurikomiKozaDto result = service.updateFurikomiKoza(dto);

        assertThat(result.getMode()).isEqualTo("view");
    }

    // No.14 異常系: 対象レコードがない場合、RuntimeExceptionが投げられる
    @Test
    void updateFurikomiKoza_対象レコードなしの場合_RuntimeExceptionをスロー() {
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.empty());

        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setVersion(1);

        assertThatThrownBy(() -> service.updateFurikomiKoza(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("更新対象の振込先口座情報が見つかりません");
    }

    // No.15 異常系: versionが不一致の場合、RuntimeExceptionが投げられる
    @Test
    void updateFurikomiKoza_version不一致の場合_RuntimeExceptionをスロー() {
        FurikomiKoza existing = new FurikomiKoza();
        existing.setVersion(2);
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.of(existing));

        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setVersion(1);

        assertThatThrownBy(() -> service.updateFurikomiKoza(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("他のユーザー");
    }

    // No.16 異常系: dto.version=nullの場合、NullPointerExceptionが投げられる
    @Test
    void updateFurikomiKoza_dtoVersionがnullの場合_NullPointerExceptionをスロー() {
        FurikomiKoza existing = new FurikomiKoza();
        existing.setVersion(1);
        when(furikomiKozaRepository.findById(any())).thenReturn(Optional.of(existing));

        FurikomiKozaDto dto = new FurikomiKozaDto();
        dto.setShiteiNo(SHITEI_NO);
        dto.setVersion(null);

        assertThatThrownBy(() -> service.updateFurikomiKoza(dto))
                .isInstanceOf(NullPointerException.class);
    }
}
