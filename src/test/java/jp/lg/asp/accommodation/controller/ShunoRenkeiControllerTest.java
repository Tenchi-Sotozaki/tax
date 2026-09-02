package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ui.Model;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShunoDto;
import jp.lg.asp.accommodation.service.ShunoRenkeiService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShunoRenkeiControllerTest {

	@Mock
	private JichitaiContext jichitaiContext;

	@Mock
	private ScreenAccessChecker accessChecker;

	@Mock
	private ShunoRenkeiService shunoRenkeiService;

	@Mock
	private Model model;

	@InjectMocks
	private ShunoRenkeiController shunoRenkeiController;

	private static final String JICHITAI_CD = "011002";

	@Nested
	@DisplayName("kakunin メソッドのテスト")
	class KakuninTest {

		@Test
		@DisplayName("正常系：有効なJSONとアクセス権があり、サービスからデータが取得できる場合、モデルに設定されてビュー名が返却されること")
		void kakunin_success() {
			String keysJson = "[{\"shiteiNo\":\"S001\",\"nendo\":2026,\"kibetsu\":1}]";
			List<ShunoDto> rows = List.of(new ShunoDto());

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.findByKeys(eq(JICHITAI_CD), any())).thenReturn(rows);

			String viewName = shunoRenkeiController.kakunin(keysJson, model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkeiKakunin");
			verify(accessChecker, times(1)).checkAccess(any());
			verify(model, times(1)).addAttribute("rows", rows);
		}

		@Test
		@DisplayName("異常系：アクセス権限チェックで例外が発生した場合、例外がキャッチされ空のリストがモデルに設定されること")
		void kakunin_accessDenied_addsEmptyList() {
			String keysJson = "[]";

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			doThrow(new RuntimeException("Access Denied")).when(accessChecker).checkAccess(any());

			String viewName = shunoRenkeiController.kakunin(keysJson, model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkeiKakunin");
			verify(model, times(1)).addAttribute("rows", Collections.emptyList());
		}

		@Test
		@DisplayName("異常系：不正なJSON文字列が渡された場合、パースエラーがキャッチされ空のリストがモデルに設定されること")
		void kakunin_invalidJson_addsEmptyList() {
			String keysJson = "invalid-json";

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);

			String viewName = shunoRenkeiController.kakunin(keysJson, model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkeiKakunin");
			verify(model, times(1)).addAttribute("rows", Collections.emptyList());
		}

		@Test
		@DisplayName("異常系：サービス層の実行中に例外が発生した場合、例外がキャッチされ空のリストがモデルに設定されること")
		void kakunin_serviceThrowsException_addsEmptyList() {
			String keysJson = "[{\"shiteiNo\":\"S001\",\"nendo\":2026,\"kibetsu\":1}]";

			when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
			when(shunoRenkeiService.findByKeys(any(), any())).thenThrow(new RuntimeException("Service Error"));

			String viewName = shunoRenkeiController.kakunin(keysJson, model);

			assertThat(viewName).isEqualTo("renkei/shunoRenkeiKakunin");
			verify(model, times(1)).addAttribute("rows", Collections.emptyList());
		}
	}
}