package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.AddressDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.repository.AtenaRepository;

@ExtendWith(MockitoExtension.class)
class AddressSearchApiControllerTest {

    @Mock AtenaRepository atenaRepository;
    @Mock JichitaiContext jichitaiContext;

    @InjectMocks AddressSearchApiController controller;

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
    }

    @Test
    void search_全条件空は空リスト() {
        List<AddressDto> result = controller.search(null, null, null, null, null, null);
        assertThat(result).isEmpty();
        verifyNoInteractions(atenaRepository);
    }

    @Test
    void search_宛名番号不正は空リスト() {
        List<AddressDto> result = controller.search("abc", null, null, null, null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void search_名前で検索() {
        Atena atena = new Atena();
        atena.setAtenaNo(BigDecimal.valueOf(1001));
        atena.setName("テスト太郎");
        atena.setNameKana("テストタロウ");
        when(atenaRepository.searchByAnyField(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(atena));

        List<AddressDto> result = controller.search(null, "テスト", null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("テスト太郎");
    }

    @Test
    void search_宛名番号で検索() {
        Atena atena = new Atena();
        atena.setAtenaNo(BigDecimal.valueOf(1001));
        atena.setName("テスト太郎");
        atena.setNameKana("テストタロウ");
        when(atenaRepository.searchByAnyField(any(), eq(BigDecimal.valueOf(1001)), any(), any(), any(), any(), any()))
                .thenReturn(List.of(atena));

        List<AddressDto> result = controller.search("1001", null, null, null, null, null);

        assertThat(result).hasSize(1);
    }
}
