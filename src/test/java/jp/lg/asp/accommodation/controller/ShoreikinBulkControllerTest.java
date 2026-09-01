package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShoreikinBulkDto;
import jp.lg.asp.accommodation.service.ShoreikinBulkService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShoreikinBulkControllerTest {

	@Mock
	private ShoreikinBulkService shoreikinBulkService;

	@Mock
	private ScreenAccessChecker accessChecker;

	@Mock
	private JichitaiContext jichitaiContext;

	@Mock
	private Model model;

	@Mock
	private BindingResult bindingResult;

	@InjectMocks
	private ShoreikinBulkController controller;

	private static final String JICHITAI_CD = "011002";
	private static final String SCREEN_ID = ScreenManagement.SHOREIKIN_BULK;
	private static final String BULK_VIEW = "shoreikin/shoreikinBulk";

	@Nested
	@DisplayName("bulk メソッドのテスト")
	class BulkTest {

		@Test
        @DisplayName("正常系：初期表示時、年度未指定でデータが取得できる場合にフォームと交付率が設定されること")
        void bulk_success() {
            Model model = new ConcurrentModel();
            when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
            when(shoreikinBulkService.findKofuRitsuList(eq(JICHITAI_CD), anyInt()))
                    .thenReturn(List.of(BigDecimal.valueOf(0.5)));

            String viewName = controller.bulk(null, model);

            assertThat(viewName).isEqualTo(BULK_VIEW);
            assertThat(model.containsAttribute("bulkForm")).isTrue();
            ShoreikinBulkDto form = (ShoreikinBulkDto) model.getAttribute("bulkForm");
            assertThat(form.getKofuRitsu()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
            assertThat(model.containsAttribute("errorMessage")).isFalse();
            verify(accessChecker, times(1)).checkAccess(any());
        }

        @Test
        @DisplayName("異常系：交付率が未登録の場合、エラーメッセージがモデルに追加されること")
        void bulk_noKofuRitsu_setsErrorMessage() {
            Model model = new ConcurrentModel();
            when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
            when(shoreikinBulkService.findKofuRitsuList(eq(JICHITAI_CD), anyInt()))
                    .thenReturn(Collections.emptyList());

            String viewName = controller.bulk(null, model);

            assertThat(viewName).isEqualTo(BULK_VIEW);
            assertThat(model.getAttribute("errorMessage"))
                    .isEqualTo("交付率のシステム設定値が登録されていません。システム設定から交付率を設定してください。");
            assertThat(model.containsAttribute("bulkForm")).isTrue();
        }
	}

	@Nested
	@DisplayName("executeBulk メソッドのテスト")
	class ExecuteBulkTest {

		@Test
		@DisplayName("正常系：バリデーションエラーなくサービス処理が成功した場合、結果がモデルに設定されて画面が返却されること")
		void executeBulk_success_returnsView() {
			ShoreikinBulkDto dto = new ShoreikinBulkDto();
			dto.setNendo("2026");

			doNothing().when(accessChecker).checkWriteAccess(SCREEN_ID);
			when(bindingResult.hasErrors()).thenReturn(false);
			when(shoreikinBulkService.executeBulkSanshutsu(dto)).thenReturn(dto);

			String viewName = controller.executeBulk(dto, bindingResult, model);

			assertThat(viewName).isEqualTo(BULK_VIEW);
			verify(model, times(1)).addAttribute("bulkForm", dto);
			verify(model, never()).addAttribute(eq("errorMessage"), any());
		}

		@Test
		@DisplayName("境界値：バリデーションエラーがある場合、サービスを呼び出さずにフォームを保持して画面が返却されること")
		void executeBulk_validationHasErrors_returnsViewWithoutServiceCall() {
			ShoreikinBulkDto dto = new ShoreikinBulkDto();

			doNothing().when(accessChecker).checkWriteAccess(SCREEN_ID);
			when(bindingResult.hasErrors()).thenReturn(true);

			String viewName = controller.executeBulk(dto, bindingResult, model);

			assertThat(viewName).isEqualTo(BULK_VIEW);
			verify(shoreikinBulkService, never()).executeBulkSanshutsu(any());
			verify(model, times(1)).addAttribute("bulkForm", dto);
		}

		@Test
		@DisplayName("異常系：サービス処理中に例外が発生した場合、エラーメッセージと詳細がモデルに設定されて画面が返却されること")
		void executeBulk_serviceThrowsException_setsErrorAttributes() {
			ShoreikinBulkDto dto = new ShoreikinBulkDto();
			dto.setNendo("2026");

			doNothing().when(accessChecker).checkWriteAccess(SCREEN_ID);
			when(bindingResult.hasErrors()).thenReturn(false);
			when(shoreikinBulkService.executeBulkSanshutsu(dto)).thenThrow(new RuntimeException("DB Error"));

			String viewName = controller.executeBulk(dto, bindingResult, model);

			assertThat(viewName).isEqualTo(BULK_VIEW);
			verify(model, times(1)).addAttribute(eq("errorMessage"), eq("一括算出処理中にエラーが発生しました。システム管理者にお問い合わせください。"));
			verify(model, times(1)).addAttribute(eq("errorDetail"), anyString());
			verify(model, times(1)).addAttribute("bulkForm", dto);
		}
	}
}