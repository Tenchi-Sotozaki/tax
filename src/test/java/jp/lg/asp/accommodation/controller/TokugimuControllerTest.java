package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;
import jp.lg.asp.accommodation.service.NozeiShukiService;
import jp.lg.asp.accommodation.service.TokugimuService;
import jp.lg.asp.accommodation.util.SessionHelper;

@ExtendWith(MockitoExtension.class)
class TokugimuControllerTest {

    @Mock TokugimuService tokugimuService;
    @Mock NozeiShukiService nozeiShukiService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks TokugimuController controller;

    private MockHttpSession sessionWith(String shiteiNo) {
        MockHttpSession session = new MockHttpSession();
        ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
        dto.setShiteiNo(shiteiNo);
        SessionHelper.saveShiteiGassan(session, dto);
        return session;
    }
    
    //===========================================
  	// list
  	//===========================================

    @Test
    void list_検索済みの場合は一覧を表示する() {
        when(tokugimuService.searchAll(any())).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.list(new TokugimuSearchForm(), true, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuDaicho");
        assertThat(model.asMap()).containsKey("items");
        assertThat(model.asMap()).containsEntry("isSearched", true);
    }

    @Test
    void list_初期表示では検索を実行しない() {
        Model model = new ExtendedModelMap();

        String view = controller.list(new TokugimuSearchForm(), false, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuDaicho");
        assertThat(model.asMap()).containsEntry("isSearched", false);
        verify(tokugimuService, never()).searchAll(any());
    }
    
    //===========================================
  	// showRegistrationForm
  	//===========================================

    @Test
    void showRegistrationForm_登録画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.showRegistrationForm(model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    //===========================================
  	// delete
  	//===========================================

    @Test
    void delete_全履歴が削除された場合は一覧に戻りセッションを解除する() {
        MockHttpSession session = sessionWith("00100001");
        Model model = new ExtendedModelMap();
        when(tokugimuService.deleteByShiteiNo("00100001")).thenReturn(false);

        String view = controller.delete(session, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tokugimu/list");
        assertThat(SessionHelper.getShiteiGassan(session)).isNull();
    }

    @Test
    void delete_履歴が残る場合は照会画面に戻りセッションを維持する() {
        MockHttpSession session = sessionWith("00100001");
        Model model = new ExtendedModelMap();
        when(tokugimuService.deleteByShiteiNo("00100001")).thenReturn(true);

        String view = controller.delete(session, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tokugimu/view");
        assertThat(SessionHelper.getShiteiGassan(session)).isNotNull();
    }

    @Test
    void delete_セッション未設定は削除せずモーダル表示() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.delete(session, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
        verify(tokugimuService, never()).deleteByShiteiNo(any());
    }
    
	//===========================================
	// showView
	//===========================================
    
	@Test
	void showView_履歴番号指定ありで照会画面を返す() {

		MockHttpSession session = sessionWith("00100001");
		TokugimuForm form = new TokugimuForm();
		when(tokugimuService.getTokugimuByShiteiNoAndRno("00100001", 2)).thenReturn(form);
		Model model = new ExtendedModelMap();

		String view = controller.showView(session, 2, model);

		assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
		assertThat(model.asMap()).containsEntry("isView", true);
		verify(tokugimuService, never()).getTokugimuByShiteiNo(any());
	}
	
	@Test
    void showView_照会画面を返す() {
        MockHttpSession session = sessionWith("00100001");
        TokugimuForm form = new TokugimuForm();
        when(tokugimuService.getTokugimuByShiteiNo("00100001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showView(session, null, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
        // 帳票発行画面が参照するセッションに、表示中の特別徴収義務者が格納されること
        assertThat(SessionHelper.getShiteiGassan(session)).isNotNull();
    }

    @Test
    void showView_セッション未設定は照会画面でモーダル表示() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.showView(session, null, model);

        // モーダルは一覧ではなく遷移先の画面で開く
        assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
    }
	
	//===========================================
	// register
	//===========================================

	@Test
	void register_サービス例外発生時にエラーハンドリングして登録画面に戻る() {
		TokugimuForm form = new TokugimuForm();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "TokugimuForm");
		Model model = new ExtendedModelMap();

		// サービス層で例外が発生するケース
		doThrow(new RuntimeException("DB登録エラー")).when(tokugimuService).register(any());

		String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap());

		assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
		assertThat(model.asMap()).containsEntry("errorMessage", "DB登録エラー");
	}
	
	@Test
    void register_バリデーションエラー() {
        TokugimuForm form = new TokugimuForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "TokugimuForm");
        bindingResult.rejectValue("name", "NotBlank", "必須です");
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
    }

    @Test
    void register_正常登録() {
        TokugimuForm form = new TokugimuForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "TokugimuForm");
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/tokugimu/list");
        verify(tokugimuService).register(form);
    }

	//===========================================
	// showEditForm
	//===========================================

	@Test
	void showEditForm_セッションありの場合は編集画面を返す() {
		MockHttpSession session = sessionWith("00100001");
		TokugimuForm form = new TokugimuForm();
		when(tokugimuService.getTokugimuByShiteiNo("00100001")).thenReturn(form);
		Model model = new ExtendedModelMap();

		String view = controller.showEditForm(session, model);

		assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
		assertThat(model.asMap()).containsEntry("isEdit", true);
		verify(tokugimuService).getTokugimuByShiteiNo("00100001");
	}

	@Test
	void showEditForm_セッション未設定の場合は編集画面でモーダル表示() {
		MockHttpSession session = new MockHttpSession();
		Model model = new ExtendedModelMap();

		String view = controller.showEditForm(session, model);

		assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
		assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
		verify(tokugimuService, never()).getTokugimuByShiteiNo(any());
	}

	//===========================================
	// update
	//===========================================

	@Test
	void update_バリデーションエラーありの場合は編集画面に戻る() {
		MockHttpSession session = sessionWith("00100001");
		TokugimuForm form = new TokugimuForm();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "TokugimuForm");
		bindingResult.rejectValue("name", "NotBlank", "必須です");
		Model model = new ExtendedModelMap();

		String view = controller.update(session, form, bindingResult, model, new RedirectAttributesModelMap());

		assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
		verify(tokugimuService, never()).updateByShiteiNo(any(), any());
	}

	@Test
	void update_正常に更新された場合は一覧または詳細へリダイレクト() {
		MockHttpSession session = sessionWith("00100001");
		TokugimuForm form = new TokugimuForm();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "TokugimuForm");
		Model model = new ExtendedModelMap();

		String view = controller.update(session, form, bindingResult, model, new RedirectAttributesModelMap());

		assertThat(view).startsWith("redirect:");
		verify(tokugimuService).updateByShiteiNo(eq("00100001"), eq(form));
	}
	
	@Test
	void update_サービス例外発生時にエラーハンドリングして編集画面に戻る() {
		MockHttpSession session = sessionWith("00100001");
		TokugimuForm form = new TokugimuForm();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "TokugimuForm");
		Model model = new ExtendedModelMap();

		// 更新処理で例外が発生するケース
		doThrow(new RuntimeException("DB更新エラー")).when(tokugimuService).updateByShiteiNo(eq("00100001"), any());

		String view = controller.update(session, form, bindingResult, model, new RedirectAttributesModelMap());

		assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
	}

	@Test
	void update_セッション未設定の場合は更新せずモーダル表示() {
		MockHttpSession session = new MockHttpSession();
		TokugimuForm form = new TokugimuForm();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "TokugimuForm");
		Model model = new ExtendedModelMap();

		String view = controller.update(session, form, bindingResult, model, new RedirectAttributesModelMap());

		assertThat(view).isEqualTo("tokugimu/tTokugimuConfig");
		assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
		verify(tokugimuService, never()).updateByShiteiNo(any(), any());
	}

	//===========================================
	// showReport
	//===========================================

	@Test
	void showReport_セッションありの場合は帳票発行画面を返す() {
		MockHttpSession session = sessionWith("00100001");
		Model model = new ExtendedModelMap();

		String view = controller.showReport(session, model);

		assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
		assertThat(model.asMap()).doesNotContainKey("showShiteiGassanModal");
	}
	
	@Test
    void showReport_セッション未設定は帳票発行画面でモーダル表示() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = controller.showReport(session, model);

        assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
        assertThat(model.asMap()).containsEntry("showShiteiGassanModal", true);
    }

	//===========================================
	// showGassanReport
	//===========================================

	@Test
	void showGassanReport_合算指定番号の場合は専用処理で帳票画面を返す() {
		MockHttpSession session = new MockHttpSession();
		ShiteiGassanSearchDto dto = new ShiteiGassanSearchDto();
		dto.setShiteiNo("00100001");
		SessionHelper.saveShiteiGassan(session, dto);
		Model model = new ExtendedModelMap();

		String view = controller.showGassanReport(session, model);

		assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
	}

	@Test
	void showGassanReport_それ以外の場合は通常処理またはモーダル表示() {
		MockHttpSession session = sessionWith("00100001"); // 合算ではない通常指定
		Model model = new ExtendedModelMap();

		String view = controller.showGassanReport(session, model);

		assertThat(view).isEqualTo("tokugimu/tTokugimuReport");
	}
}
