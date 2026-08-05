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
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;

@ExtendWith(MockitoExtension.class)
class AddressSearchApiControllerTest {

	@Mock
	AtenaRepository atenaRepository;
	@Mock
	GassanRepository gassanRepository;
	@Mock
	JichitaiContext jichitaiContext;

	@InjectMocks
	AddressSearchApiController controller;

	@BeforeEach
	void setUp() {
		when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
	}

	@Test
	void search_全条件空は空リスト() {
		List<AddressDto> result = controller.search(null, null, "partial", null, "partial", null, null, null);
		assertThat(result).isEmpty();
		verifyNoInteractions(atenaRepository);
		verifyNoInteractions(gassanRepository);
	}

	@Test
	void search_名前で検索() {
		Atena atena = new Atena();
		atena.setAtenaNo(BigDecimal.valueOf(1001));
		atena.setName("テスト太郎");
		atena.setNameKana("テストタロウ");
		when(atenaRepository.search(any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(List.of(atena));
		
		when(gassanRepository.findByJichitaiCdAndAtenaNo(eq("011002"), eq(BigDecimal.valueOf(1001))))
				.thenReturn(List.of());

		List<AddressDto> result = controller.search(null, "テスト", "partial", null, "partial", null, null, null);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("テスト太郎");
		assertThat(result.get(0).isAlreadyRegistered()).isFalse();
	}

	@Test
	void search_宛名番号で検索() {
		Atena atena = new Atena();
		atena.setAtenaNo(BigDecimal.valueOf(1001));
		atena.setName("テスト太郎");
		atena.setNameKana("テストタロウ");
		when(atenaRepository.search(any(), eq("1001"), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(List.of(atena));

		Gassan gassan = new Gassan();
		gassan.setGassanShiteiNo("G001");
		when(gassanRepository.findByJichitaiCdAndAtenaNo(eq("011002"), eq(BigDecimal.valueOf(1001))))
				.thenReturn(List.of(gassan));

		List<AddressDto> result = controller.search("1001", null, "partial", null, "partial", null, null, null);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isAlreadyRegistered()).isTrue();
		assertThat(result.get(0).getGassanShiteiNo()).isEqualTo("G001");
	}
}