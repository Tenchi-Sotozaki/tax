package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.MenuDto;
import jp.lg.asp.accommodation.service.GlobalModelService;

@ExtendWith(MockitoExtension.class)
class GlobalModelAdviceTest {

	@Mock GlobalModelService globalModelService;
	@Mock JichitaiContext jichitaiContext;

	@InjectMocks GlobalModelAdvice advice;

	@Test
	void sideMenuTree_空のメニューは空リストを返す() {
		when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
		when(globalModelService.buildSideMenuTree(eq("00001"), any())).thenReturn(List.of());

		assertThat(advice.sideMenuTree()).isEmpty();
	}

	@Test
	void sideMenuTree_サービスが返したリストをそのまま返す() {
		MenuDto dto = new MenuDto();
		dto.setMenuId("lv1");
		when(jichitaiContext.getJichitaiCd()).thenReturn("00001");
		when(globalModelService.buildSideMenuTree(eq("00001"), any())).thenReturn(List.of(dto));

		List<MenuDto> result = advice.sideMenuTree();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getMenuId()).isEqualTo("lv1");
	}

	@Test
	void accessibleScreens_未認証は空セットを返す() {
		when(jichitaiContext.getJichitaiCd()).thenReturn("00001");

		Set<String> result = advice.accessibleScreens();

		assertThat(result).isEmpty();
		verifyNoInteractions(globalModelService);
	}
}
