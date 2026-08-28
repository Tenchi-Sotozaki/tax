package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.exception.ResourceNotFoundException;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.impl.AtenaConfigServiceImpl;
import jp.lg.asp.accommodation.util.HashUtil;

@ExtendWith(MockitoExtension.class)
class AtenaConfigServiceImplTest {

    @Mock
    private AtenaRepository atenaRepository;

    @Mock
    private JichitaiRepository jichitaiRepository;

    @Mock
    private HashUtil hashUtil;

    @InjectMocks
    private AtenaConfigServiceImpl atenaConfigService;

    private static final String JICHITAI_CD = "123456";

    @Nested
    @DisplayName("findByAtenaNo メソッドのテスト")
    class FindByAtenaNoTest {

        @Test
        @DisplayName("正常系：指定した自治体コードと宛名番号に該当する宛名情報が正しく取得できること")
        void findByAtenaNo_found() {
            BigDecimal atenaNo = BigDecimal.valueOf(1);
            Atena expectedAtena = new Atena();
            expectedAtena.setJichitaiCd(JICHITAI_CD);
            expectedAtena.setAtenaNo(atenaNo);

            when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, atenaNo))
                    .thenReturn(Optional.of(expectedAtena));

            Atena result = atenaConfigService.findByAtenaNo(JICHITAI_CD, atenaNo);

            assertThat(result).isNotNull();
            assertThat(result.getJichitaiCd()).isEqualTo(JICHITAI_CD);
            assertThat(result.getAtenaNo()).isEqualTo(atenaNo);
        }

        @Test
        @DisplayName("異常系：指定した自治体コードと宛名番号に該当する宛名情報が存在しない場合、例外がスローされること")
        void findByAtenaNo_notFound_throwsException() {
            BigDecimal atenaNo = BigDecimal.valueOf(999);

            when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, atenaNo))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> atenaConfigService.findByAtenaNo(JICHITAI_CD, atenaNo))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("宛名が見つかりません。");
        }
    }
}