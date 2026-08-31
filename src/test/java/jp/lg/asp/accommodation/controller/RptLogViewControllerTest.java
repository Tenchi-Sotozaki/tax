package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.RptLogViewDto;
import jp.lg.asp.accommodation.entity.Reports;
import jp.lg.asp.accommodation.service.RptLogViewService;

@ExtendWith(MockitoExtension.class)
class RptLogViewControllerTest {

	@InjectMocks
	private RptLogViewController rptLogViewController;

	@Mock
	private RptLogViewService rptLogViewService;

	@Mock
	private ScreenAccessChecker accessChecker;

	@Nested
	@DisplayName("init メソッドのテスト")
	class InitTest {

		@Test
		@DisplayName("正常系：画面初期表示時に権限チェックが通り、モデルに必要データが設定されること")
		void success() {
			Model model = new ConcurrentModel();
			List<Reports> reportsList = List.of(new Reports());

			when(rptLogViewService.findAllReports()).thenReturn(reportsList);

			String viewName = rptLogViewController.init(model);

			assertThat(viewName).isEqualTo("log/rptLogView");
			assertThat(model.getAttribute("form")).isInstanceOf(RptLogViewDto.class);
			assertThat(model.getAttribute("reports")).isEqualTo(reportsList);
			assertThat(model.getAttribute("items")).isEqualTo(List.of());
			verify(accessChecker).checkAccess(ScreenManagement.RPT_LOG_VIEW);
		}

		@Test
		@DisplayName("境界値：帳票定義が0件の場合でも初期表示が正常に行われること")
		void emptyReports() {
			Model model = new ConcurrentModel();

			when(rptLogViewService.findAllReports()).thenReturn(List.of());

			String viewName = rptLogViewController.init(model);

			assertThat(viewName).isEqualTo("log/rptLogView");
			assertThat((List<?>) model.getAttribute("reports")).isEmpty();
			verify(accessChecker).checkAccess(ScreenManagement.RPT_LOG_VIEW);
		}

		@Test
		@DisplayName("異常系：アクセス権限がない場合に例外がスローされること")
		void accessDenied() {
			Model model = new ConcurrentModel();

			doThrow(new AccessDeniedException("Access Denied"))
					.when(accessChecker).checkAccess(ScreenManagement.RPT_LOG_VIEW);

			assertThatThrownBy(() -> rptLogViewController.init(model))
					.isInstanceOf(AccessDeniedException.class);
			verify(rptLogViewService, never()).findAllReports();
		}
	}

	@Nested
	@DisplayName("search メソッドのテスト")
	class SearchTest {

		@Test
		@DisplayName("正常系：バリデーションエラーがなく検索成功した場合に結果が設定されること")
		void success() {
			RptLogViewDto form = new RptLogViewDto();
			BindingResult bindingResult = mock(BindingResult.class);
			Model model = new ConcurrentModel();
			List<Reports> reportsList = List.of(new Reports());
			List<RptLogViewDto> itemsList = List.of(new RptLogViewDto());

			when(bindingResult.hasErrors()).thenReturn(false);
			when(rptLogViewService.findAllReports()).thenReturn(reportsList);
			when(rptLogViewService.search(form)).thenReturn(itemsList);

			String viewName = rptLogViewController.search(form, bindingResult, model);

			assertThat(viewName).isEqualTo("log/rptLogView");
			assertThat(model.getAttribute("reports")).isEqualTo(reportsList);
			assertThat(model.getAttribute("items")).isEqualTo(itemsList);
			assertThat(model.getAttribute("searched")).isEqualTo(true);
			verify(accessChecker).checkAccess(ScreenManagement.RPT_LOG_VIEW);
		}

		@Test
		@DisplayName("境界値：検索結果が0件の場合に空のリストが設定されること")
		void emptyResult() {
			RptLogViewDto form = new RptLogViewDto();
			BindingResult bindingResult = mock(BindingResult.class);
			Model model = new ConcurrentModel();
			List<Reports> reportsList = List.of(new Reports());

			when(bindingResult.hasErrors()).thenReturn(false);
			when(rptLogViewService.findAllReports()).thenReturn(reportsList);
			when(rptLogViewService.search(form)).thenReturn(List.of());

			String viewName = rptLogViewController.search(form, bindingResult, model);

			assertThat(viewName).isEqualTo("log/rptLogView");
			assertThat((List<?>) model.getAttribute("items")).isEmpty();
			assertThat(model.getAttribute("searched")).isEqualTo(true);
		}

		@Test
		@DisplayName("異常系：バリデーションエラーがある場合に検索処理が実行されないこと")
		void validationError() {
			RptLogViewDto form = new RptLogViewDto();
			BindingResult bindingResult = mock(BindingResult.class);
			Model model = new ConcurrentModel();
			List<Reports> reportsList = List.of(new Reports());

			when(bindingResult.hasErrors()).thenReturn(true);
			when(rptLogViewService.findAllReports()).thenReturn(reportsList);

			String viewName = rptLogViewController.search(form, bindingResult, model);

			assertThat(viewName).isEqualTo("log/rptLogView");
			assertThat(model.getAttribute("reports")).isEqualTo(reportsList);
			assertThat((List<?>) model.getAttribute("items")).isEmpty();
			assertThat(model.containsAttribute("searched")).isFalse();
			verify(rptLogViewService, never()).search(any());
		}

		@Test
		@DisplayName("異常系：検索時にアクセス権限がない場合に例外がスローされること")
		void accessDenied() {
			RptLogViewDto form = new RptLogViewDto();
			BindingResult bindingResult = mock(BindingResult.class);
			Model model = new ConcurrentModel();

			doThrow(new AccessDeniedException("Access Denied"))
					.when(accessChecker).checkAccess(ScreenManagement.RPT_LOG_VIEW);

			assertThatThrownBy(() -> rptLogViewController.search(form, bindingResult, model))
					.isInstanceOf(AccessDeniedException.class);
			verify(rptLogViewService, never()).findAllReports();
			verify(rptLogViewService, never()).search(any());
		}
	}
}