package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.AddressDto;
import jp.lg.asp.accommodation.service.AddressSearchApiService;

@ExtendWith(MockitoExtension.class)
class AddressSearchApiControllerTest {

	@Mock AddressSearchApiService addressSearchApiService;

	@Mock JichitaiContext jichitaiContext;

	@InjectMocks AddressSearchApiController controller;

	@BeforeEach
	void setUp() {
		when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
	}

	@Test
	void search_全条件空は空リスト() {
		when(addressSearchApiService.searchAddresses(eq("011002"), isNull(), isNull(), eq("partial"), isNull(), eq("partial"), isNull(), isNull(), isNull()))
				.thenReturn(List.of());

		List<AddressDto> result = controller.search(null, null, "partial", null, "partial", null, null, null);
		
		assertThat(result).isEmpty();
		verify(addressSearchApiService).searchAddresses(eq("011002"), isNull(), isNull(), eq("partial"), isNull(), eq("partial"), isNull(), isNull(), isNull());
	}

	@Test
	void search_名前で検索() {
		AddressDto dto = new AddressDto();
		dto.setName("テスト太郎");
		dto.setAlreadyRegistered(false);

		when(addressSearchApiService.searchAddresses(eq("011002"), isNull(), eq("テスト"), eq("partial"), isNull(), eq("partial"), isNull(), isNull(), isNull()))
				.thenReturn(List.of(dto));

		List<AddressDto> result = controller.search(null, "テスト", "partial", null, "partial", null, null, null);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("テスト太郎");
		assertThat(result.get(0).isAlreadyRegistered()).isFalse();
	}

	@Test
	void search_宛名番号で検索() {
		AddressDto dto = new AddressDto();
		dto.setName("テスト太郎");
		dto.setAlreadyRegistered(true);
		dto.setGassanShiteiNo("G001");

		when(addressSearchApiService.searchAddresses(eq("011002"), eq("1001"), isNull(), eq("partial"), isNull(), eq("partial"), isNull(), isNull(), isNull()))
				.thenReturn(List.of(dto));

		List<AddressDto> result = controller.search("1001", null, "partial", null, "partial", null, null, null);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).isAlreadyRegistered()).isTrue();
		assertThat(result.get(0).getGassanShiteiNo()).isEqualTo("G001");
	}
}