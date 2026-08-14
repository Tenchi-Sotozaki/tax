package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.dto.AtenaConfigForm;
import jp.lg.asp.accommodation.dto.AtenaImportPreviewDto;
import jp.lg.asp.accommodation.dto.AtenaSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.AtenaConfigService;
import jp.lg.asp.accommodation.service.AtenaImportService;
import jp.lg.asp.accommodation.util.HashUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtenaControllerTest {

    @Mock AtenaRepository atenaRepository;
    @Mock AtenaImportService atenaImportService;
    @Mock ScreenAccessChecker accessChecker;
    @Mock HashUtil hashUtil;
    @Mock JichitaiContext jichitaiContext;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock AtenaConfigService atenaConfigService;

    @InjectMocks AtenaController controller;

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn("011002");
        when(atenaRepository.search(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testuser");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }
    
	//===========================================
	// list (一覧照会) 
	//===========================================
    @Test
    void list_正常系_検索結果が取得できること() {
        AtenaSearchForm form = new AtenaSearchForm();
        Model model = new ExtendedModelMap();
        
        // モックの設定
        Page<Atena> emptyPage = 
            new PageImpl<>(List.of());
        when(atenaRepository.searchPage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(emptyPage);
        when(jichitaiRepository.findById(any())).thenReturn(Optional.empty());

        String view = controller.list(form, 0, 10, model);

        assertThat(view).isEqualTo("atena/atenaDaicho");
        assertThat(model.getAttribute("items")).isNotNull();
        assertThat(model.getAttribute("searchForm")).isSameAs(form);
    }

	//===========================================
	// showImport (取込画面表示) 
	//===========================================
    @Test
    void showImport_取込画面を返す() {
        when(atenaImportService.findHistory("011002")).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.showImport(model);

        assertThat(view).isEqualTo("atena/atenaRenkei");
    }
    
	//===========================================
	// analyze (CSV解析) 
	//===========================================
    @Test
    void analyze_拡張子が小文字以外や不正な場合の異常系() {
        // 例: 大文字のCSVは通る仕様か、あるいは無効かなど。ここでは別拡張子
        MockMultipartFile file = new MockMultipartFile("file", "test.TXT", "text/plain", "data".getBytes());

        ResponseEntity<?> res = controller.analyze(file, new MockHttpSession());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void analyze_サービス層で例外発生時の異常系() {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        when(atenaImportService.analyze(any(), any())).thenThrow(new RuntimeException("解析エラー"));

        ResponseEntity<?> res = controller.analyze(file, new MockHttpSession());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void analyze_空ファイルはエラー() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        ResponseEntity<?> res = controller.analyze(emptyFile, new MockHttpSession());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void analyze_CSV以外はエラー() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        ResponseEntity<?> res = controller.analyze(file, new MockHttpSession());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void analyze_解析結果をセッションに保持する() {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        AtenaImportPreviewDto preview = new AtenaImportPreviewDto();
        when(atenaImportService.analyze(any(), any())).thenReturn(preview);
        MockHttpSession session = new MockHttpSession();

        ResponseEntity<?> res = controller.analyze(file, session);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(session.getAttribute("atenaImportPreview")).isSameAs(preview);
    }
    
    //===========================================
    // confirmImport (確定取込) 
    //===========================================
    @Test
    void confirmImport_境界値_選択リストがnullの場合() {
        AtenaImportPreviewDto preview = new AtenaImportPreviewDto();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("atenaImportPreview", preview);

        ResponseEntity<?> res = controller.confirmImport(null, session);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(atenaImportService).confirm(eq(preview), eq(Set.of()), eq("011002"), eq("testuser"));
    }

    @Test
    void confirmImport_サービス層で例外発生時の異常系() {
        AtenaImportPreviewDto preview = new AtenaImportPreviewDto();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("atenaImportPreview", preview);
        doThrow(new RuntimeException("取込失敗")).when(atenaImportService).confirm(any(), any(), any(), any());

        ResponseEntity<?> res = controller.confirmImport(List.of("100"), session);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void confirmImport_解析結果が無い場合はエラー() {
        ResponseEntity<?> res = controller.confirmImport(List.of("1"), new MockHttpSession());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void confirmImport_選択された宛名のみ取り込む() {
        AtenaImportPreviewDto preview = new AtenaImportPreviewDto();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("atenaImportPreview", preview);

        ResponseEntity<?> res = controller.confirmImport(List.of("100", "200"), session);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(atenaImportService).confirm(eq(preview), eq(Set.of("100", "200")), eq("011002"), eq("testuser"));
        // 二重登録を防ぐため、確定後はセッションから解析結果を破棄する
        assertThat(session.getAttribute("atenaImportPreview")).isNull();
    }
    
	//===========================================
	// importDetail (取込結果明細取得) 
	//===========================================
	@Test
	void importDetail_正常系() {
		BigDecimal seq = BigDecimal.ONE;
		when(atenaImportService.findDetail(eq("011002"), eq(seq))).thenReturn(List.of());

		ResponseEntity<?> res = controller.importDetail(seq);

		assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
	
	//===========================================
	// register (登録)
	//===========================================
	@Test
	void showRegister_登録画面を返す() {
		Model model = new ExtendedModelMap();

		String view = controller.showRegister(model);

		assertThat(view).isEqualTo("atena/atenaConfig");
		assertThat(model.getAttribute("mode")).isEqualTo("register");
		assertThat(model.getAttribute("form")).isNotNull();
	}

	@Test
	void register_異常系_個人番号も法人番号も未入力() {
		AtenaConfigForm form = new AtenaConfigForm();
		form.setName("テスト 太郎");
		// kojinNoもhojinNoもnull/blank
		BindingResult bindingResult = mock(BindingResult.class);
		Model model = new ExtendedModelMap();
		RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.register(form, bindingResult, model, redirectAttributes);

		assertThat(view).isEqualTo("atena/atenaConfig");
		assertThat(model.getAttribute("errorMessage")).isEqualTo("個人番号または法人番号のいずれかを入力してください。");
	}

	@Test
	void register_異常系_個人番号と法人番号が両方入力されている() {
		AtenaConfigForm form = new AtenaConfigForm();
		form.setKojinNo("123456789012");
		form.setHojinNo("1234567890123");
		BindingResult bindingResult = mock(BindingResult.class);
		Model model = new ExtendedModelMap();
		RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.register(form, bindingResult, model, redirectAttributes);

		assertThat(view).isEqualTo("atena/atenaConfig");
		assertThat(model.getAttribute("errorMessage")).isEqualTo("個人番号と法人番号は同時に入力できません。");
	}

	@Test
	void register_正常系() {
		AtenaConfigForm form = new AtenaConfigForm();
		form.setKojinNo("123456789012");
		form.setName("テスト 太郎");
		BindingResult bindingResult = mock(BindingResult.class);
		Model model = new ExtendedModelMap();
		RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.register(form, bindingResult, model, redirectAttributes);

		assertThat(view).isEqualTo("redirect:/atena/list");
		verify(atenaConfigService).register(any(), eq("011002"));
	}
	
	//===========================================
	// view (照会)
	//===========================================
	@Test
	void view_正常系() {
		BigDecimal atenaNo = BigDecimal.TEN;
		Atena atena = new Atena();
		atena.setAtenaNo(atenaNo);
		atena.setName("テスト 太郎");
		when(atenaConfigService.findByAtenaNo(eq("011002"), eq(atenaNo))).thenReturn(atena);
		Model model = new ExtendedModelMap();

		String view = controller.view(atenaNo, model);

		assertThat(view).isEqualTo("atena/atenaConfig");
		assertThat(model.getAttribute("mode")).isEqualTo("view");
		assertThat(model.getAttribute("form")).isNotNull();
	}

	//===========================================
	// showEdit (編集画面表示) 
	//===========================================
	@Test
	void showEdit_正常系() {
		BigDecimal atenaNo = BigDecimal.TEN;
		Atena atena = new Atena();
		atena.setAtenaNo(atenaNo);
		when(atenaConfigService.findByAtenaNo(eq("011002"), eq(atenaNo))).thenReturn(atena);
		Model model = new ExtendedModelMap();

		String view = controller.showEdit(atenaNo, model);

		assertThat(view).isEqualTo("atena/atenaConfig");
		assertThat(model.getAttribute("mode")).isEqualTo("edit");
	}
	
	//===========================================
	// edit (編集) 
	//===========================================
	@Test
	void edit_異常系_番号未入力() {
		AtenaConfigForm form = new AtenaConfigForm();
		form.setAtenaNo(BigDecimal.TEN);
		Model model = new ExtendedModelMap();
		RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.edit(form, model, redirectAttributes);

		assertThat(view).isEqualTo("atena/atenaConfig");
		assertThat(model.getAttribute("errorMessage")).isEqualTo("個人番号または法人番号のいずれかを入力してください。");
	}

	@Test
	void edit_正常系() {
		AtenaConfigForm form = new AtenaConfigForm();
		form.setAtenaNo(BigDecimal.TEN);
		form.setKojinNo("123456789012");
		Model model = new ExtendedModelMap();
		RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

		String view = controller.edit(form, model, redirectAttributes);

		assertThat(view).isEqualTo("redirect:/atena/view/10");
		verify(atenaConfigService).update(any(), eq("011002"));
	}
}
