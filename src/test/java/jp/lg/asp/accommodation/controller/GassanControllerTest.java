package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.GassanDaichoItem;
import jp.lg.asp.accommodation.dto.GassanDaichoSearchForm;
import jp.lg.asp.accommodation.dto.GassanForm;
import jp.lg.asp.accommodation.service.GassanDaichoService;
import jp.lg.asp.accommodation.service.GassanService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GassanControllerTest {

    @Mock GassanService gassanService;
    @Mock GassanDaichoService gassanDaichoService;
    @Mock ScreenAccessChecker accessChecker;

    @InjectMocks GassanController controller;

    //=====================================================
    // showRegistrationForm（登録）
    //=====================================================
    @Test
    void showRegistrationForm_登録画面を返す() {
        Model model = new ExtendedModelMap();

        String view = controller.showRegistrationForm(model);

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("isEdit", false);
    }

    //=====================================================
  	// showDaicho（台帳）
  	//=====================================================
    @Test
    void showDaicho_一覧画面を返す() {
        when(gassanDaichoService.search(any())).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        Model model = new ExtendedModelMap();

        String view = controller.showDaicho(new GassanDaichoSearchForm(), model);

        assertThat(view).isEqualTo("gassan/tGassanDaicho");
    }
    
	@Test
	void showDaicho_境界値_0件() {
		when(gassanDaichoService.search(any())).thenReturn(
				new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
		Model model = new ExtendedModelMap();

		String view = controller.showDaicho(new GassanDaichoSearchForm(), model);

		assertThat(view).isEqualTo("gassan/tGassanDaicho");
		assertThat(model.asMap()).containsKey("items");
	}

	//=====================================================
	// showView (詳細)
	//=====================================================
    @Test
    void showView_データあり_照会画面を返す() {
        GassanDaichoItem item = new GassanDaichoItem();
        when(gassanDaichoService.getByGassanShiteiNo("G001")).thenReturn(item);
        Model model = new ExtendedModelMap();

        String view = controller.showView("G001", model);

        assertThat(view).isEqualTo("gassan/tGassanView");
    }

    @Test
    void showView_データなし_リダイレクト() {
        when(gassanDaichoService.getByGassanShiteiNo("G999")).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.showView("G999", model);

        assertThat(view).isEqualTo("redirect:/gassan/list");
    }

	//=====================================================
	// showViewForm (照会フォーム)
	//=====================================================
    @Test
    void showViewForm_照会フォームを返す() {
        GassanForm form = new GassanForm();
        when(gassanService.getByGassanShiteiNo("G001")).thenReturn(form);
        Model model = new ExtendedModelMap();

        String view = controller.showViewForm("G001", null, model);

        assertThat(view).isEqualTo("gassan/tGassanConfig");
        assertThat(model.asMap()).containsEntry("isView", true);
    }
    
	@Test
	void showViewForm_境界値_rnoあり() {
		GassanForm form = new GassanForm();
		when(gassanService.getByGassanShiteiNoAndRno(eq("G001"), eq(BigDecimal.ONE))).thenReturn(form);
		Model model = new ExtendedModelMap();

		String view = controller.showViewForm("G001", BigDecimal.ONE, model);

		assertThat(view).isEqualTo("gassan/tGassanConfig");
		assertThat(model.asMap()).containsEntry("isView", true);
	}

	@Test
	void showViewForm_異常系_例外発生() {
		when(gassanService.getByGassanShiteiNo("G999")).thenThrow(new RuntimeException("Error"));
		Model model = new ExtendedModelMap();

		String view = controller.showViewForm("G999", null, model);

		assertThat(view).isEqualTo("redirect:/gassan/list");
		assertThat(model.asMap()).containsEntry("errorMessage", "指定された合算申告情報が見つかりません。");
	}
	
	//=====================================================
	// showEditForm (編集画面)
	//=====================================================
	@Test
	void showEditForm_正常() {
		GassanForm form = new GassanForm();
		when(gassanService.getByGassanShiteiNo("G001")).thenReturn(form);
		Model model = new ExtendedModelMap();

		String view = controller.showEditForm("G001", model);

		assertThat(view).isEqualTo("gassan/tGassanConfig");
		assertThat(model.asMap()).containsEntry("isEdit", true);
	}

	@Test
	void showEditForm_異常系() {
		when(gassanService.getByGassanShiteiNo("G999")).thenThrow(new RuntimeException("Error"));
		Model model = new ExtendedModelMap();

		String view = controller.showEditForm("G999", model);

		assertThat(view).isEqualTo("redirect:/gassan/list");
	}
	
	//=====================================================
	// updateGassan (編集・更新)
	//=====================================================
	@Test
	void updateGassan_正常() {
		GassanForm form = new GassanForm();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
		Model model = new ExtendedModelMap();
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.updateGassan("G001", form, bindingResult, model, redirectAttributes);

		assertThat(view).isEqualTo("redirect:/gassan/list");
		assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
		verify(gassanService).updateByGassanShiteiNo(eq("G001"), eq(form));
	}

	@Test
	void updateGassan_境界値_バリデーションエラー() {
		GassanForm form = new GassanForm();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
		bindingResult.rejectValue("gassanShiteiNo", "error.required");
		Model model = new ExtendedModelMap();
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.updateGassan("G001", form, bindingResult, model, redirectAttributes);

		assertThat(view).isEqualTo("gassan/tGassanConfig");
		assertThat(model.asMap()).containsEntry("isEdit", true);
		verify(gassanService, never()).updateByGassanShiteiNo(any(), any());
	}

	@Test
	void updateGassan_異常系_サービス例外() {
		GassanForm form = new GassanForm();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
		doThrow(new RuntimeException("DB Error")).when(gassanService).updateByGassanShiteiNo(any(), any());
		Model model = new ExtendedModelMap();
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.updateGassan("G001", form, bindingResult, model, redirectAttributes);

		assertThat(view).isEqualTo("gassan/tGassanConfig");
		assertThat(model.asMap()).containsKey("errorMessage");
	}
	
	//=====================================================
	// register (登録)
	//=====================================================
    @Test
    void register_正常登録() {
        GassanForm form = new GassanForm();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
        Model model = new ExtendedModelMap();

        String view = controller.register(form, bindingResult, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/gassan/list");
        verify(gassanService).register(form);
    }
    
	@Test
	void register_境界値_バリデーションエラー() {
		GassanForm form = new GassanForm();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
		bindingResult.rejectValue("gassanShiteiNo", "error.required");
		Model model = new ExtendedModelMap();
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.register(form, bindingResult, model, redirectAttributes);

		assertThat(view).isEqualTo("gassan/tGassanConfig");
		assertThat(model.asMap()).containsEntry("isEdit", false);
		verify(gassanService, never()).register(any());
	}

	@Test
	void register_異常系_サービス例外() {
		GassanForm form = new GassanForm();
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
		doThrow(new RuntimeException("DB Error")).when(gassanService).register(any());
		Model model = new ExtendedModelMap();
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.register(form, bindingResult, model, redirectAttributes);

		assertThat(view).isEqualTo("gassan/tGassanConfig");
		assertThat(model.asMap()).containsKey("errorMessage");
	}

	//=====================================================
	// getFacilitiesByAtena (施設一覧取得)
	//=====================================================
    @Test
    void getFacilitiesByAtena_施設一覧を返す() {
        when(gassanService.getFacilitiesByAtenaNo(BigDecimal.valueOf(1001))).thenReturn(List.of());

        List<GassanForm.FacilityItem> result = controller.getFacilitiesByAtena(
                Map.of("atenaNo", "1001"));

        assertThat(result).isNotNull();
    }
    
	//=====================================================
	// delete (削除)
	//=====================================================
	@Test
	void delete_正常() {
		GassanForm form = new GassanForm();
		form.setGassanShiteiNo("G001");
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
		Model model = new ExtendedModelMap();
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.delete(form, bindingResult, model, redirectAttributes);

		assertThat(view).isEqualTo("redirect:/gassan/list");
		assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
		verify(gassanService).deleteByGassanShiteiNo("G001");
	}

	@Test
	void delete_境界値_指定番号なし() {
		GassanForm form = new GassanForm();
		form.setGassanShiteiNo(""); // 空文字
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
		Model model = new ExtendedModelMap();
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.delete(form, bindingResult, model, redirectAttributes);

		assertThat(view).isEqualTo("redirect:/gassan/list");
		assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
		verify(gassanService, never()).deleteByGassanShiteiNo(any());
	}

	@Test
	void delete_異常系_サービス例外() {
		GassanForm form = new GassanForm();
		form.setGassanShiteiNo("G001");
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "GassanForm");
		doThrow(new RuntimeException("DB Error")).when(gassanService).deleteByGassanShiteiNo("G001");
		Model model = new ExtendedModelMap();
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.delete(form, bindingResult, model, redirectAttributes);

		assertThat(view).isEqualTo("redirect:/gassan/edit/G001");
		assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
	}
}
