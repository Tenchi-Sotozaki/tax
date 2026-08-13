package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.TokugimuForm;
import jp.lg.asp.accommodation.dto.TokugimuSearchForm;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.KyodoJigyoshaRepository;
import jp.lg.asp.accommodation.repository.ShoyushaRepository;
import jp.lg.asp.accommodation.repository.ShunoRirekiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.TokugimuServiceImpl;

@ExtendWith(MockitoExtension.class)
class TokugimuServiceImplTest {

    @Mock TokugimuRepository tokugimuRepository;
    @Mock AtenaRepository atenaRepository;
    @Mock GassanRepository gassanRepository;
    @Mock GassanUchiRepository gassanUchiRepository;
    @Mock ShoyushaRepository shoyushaRepository;
    @Mock KyodoJigyoshaRepository kyodoJigyoshaRepository;
    @Mock JichitaiRepository jichitaiRepository;
    @Mock FukaRepository fukaRepository;
    @Mock ShunoRirekiRepository shunoRirekiRepository;
    @Mock JichitaiContext jichitaiContext;
    @InjectMocks TokugimuServiceImpl service;

    private static final String JICHITAI_CD = "011002";
    private static final String SHITEI_NO = "00000001";

    @BeforeEach
    void setUp() {
        when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
    }

    private Tokugimu buildTokugimu(String shiteiNo) {
        Tokugimu t = new Tokugimu();
        t.setShiteiNo(shiteiNo);
        t.setAtenaNo(BigDecimal.ONE);
        t.setShisetsuName("テスト施設");
        t.setKyokaName("テスト事業者");
        t.setRno(BigDecimal.ONE);
        return t;
    }

    private Atena buildAtena() {
        Atena a = new Atena();
        a.setAtenaNo(BigDecimal.ONE);
        a.setName("テスト事業者");
        return a;
    }

    @Test
    void search_emptyForm_returnsAllItems() {
        TokugimuSearchForm form = new TokugimuSearchForm();
        form.setPage(0);
        form.setPageSize(10);

        Tokugimu t = buildTokugimu(SHITEI_NO);
        when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of(buildAtena()));
        when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of());

        Page<?> result = service.search(form);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_emptyResult_returnsEmptyPage() {
        TokugimuSearchForm form = new TokugimuSearchForm();
        form.setPage(0);
        form.setPageSize(10);
        when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());

        Page<?> result = service.search(form);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void getTokugimuByShiteiNo_found() {
        Tokugimu t = buildTokugimu(SHITEI_NO);
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(t));
        when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(Optional.of(buildAtena()));
        when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.of(1));
        when(tokugimuRepository.findMinRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.of(1));
        when(shoyushaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());
        when(kyodoJigyoshaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        TokugimuForm form = service.getTokugimuByShiteiNo(SHITEI_NO);

        assertThat(form.getShiteiNo()).isEqualTo(SHITEI_NO);
    }

    @Test
    void getTokugimuByShiteiNo_notFound_throwsException() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getTokugimuByShiteiNo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void register_noAtenaNo_throwsException() {
        TokugimuForm form = new TokugimuForm();
        form.setAtenaNo(null);

        assertThatThrownBy(() -> service.register(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("宛名番号");
    }

    @Test
    void deleteByShiteiNo_履歴が残らない場合はfalseを返す() {
        Tokugimu t = buildTokugimu(SHITEI_NO);
        t.setDelFlg("0");
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(t));
        when(tokugimuRepository.findActiveHistoryByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of());
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean remains = service.deleteByShiteiNo(SHITEI_NO);

        assertThat(remains).isFalse();
        assertThat(t.getDelFlg()).isEqualTo("1");
        assertThat(t.getNewFlg()).isEqualTo("0");
    }

    @Test
    void deleteByShiteiNo_履歴が残る場合は最新履歴を最新版に戻す() {
        Tokugimu current = buildTokugimu(SHITEI_NO);
        current.setDelFlg("0");
        current.setRno(BigDecimal.valueOf(2));
        Tokugimu prev = buildTokugimu(SHITEI_NO);
        prev.setDelFlg("0");
        prev.setNewFlg("0");
        prev.setRno(BigDecimal.ONE);

        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(current));
        when(tokugimuRepository.findActiveHistoryByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
                .thenReturn(List.of(prev));
        when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean remains = service.deleteByShiteiNo(SHITEI_NO);

        assertThat(remains).isTrue();
        assertThat(current.getDelFlg()).isEqualTo("1");
        assertThat(prev.getNewFlg()).isEqualTo("1");
    }

    @Test
    void deleteByShiteiNo_notFound_throwsException() {
        when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

        assertThatThrownBy(() -> service.deleteByShiteiNo(SHITEI_NO))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getShiteiNoById_found() {
        Tokugimu t = buildTokugimu(SHITEI_NO);
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE)).thenReturn(List.of(t));

        assertThat(service.getShiteiNoById(1L)).isEqualTo(SHITEI_NO);
    }

    @Test
    void getShiteiNoById_notFound_throwsException() {
        when(tokugimuRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.getShiteiNoById(99L))
                .isInstanceOf(RuntimeException.class);
    }

	@Test
	void search_条件指定時にフィルタリングされたアイテムを返す() {
		TokugimuSearchForm form = new TokugimuSearchForm();
		form.setPage(0);
		form.setPageSize(10);
		form.setShiteiNo(SHITEI_NO);

		Tokugimu t = buildTokugimu(SHITEI_NO);
		when(tokugimuRepository.findBySearchConditions(eq(JICHITAI_CD), eq(SHITEI_NO), any(), any(), any(), any(),
				any(), any(), any()))
						.thenReturn(List.of(t));
		when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of(buildAtena()));
		when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of());

		Page<?> result = service.search(form);

		assertThat(result.getTotalElements()).isEqualTo(1);
	}

	@Test
	void search_合算指定番号プレフィックスの場合合算経由でアイテムを返す() {
		TokugimuSearchForm form = new TokugimuSearchForm();
		form.setPage(0);
		form.setPageSize(10);
		String gassanShiteiNo = "90000001";
		form.setShiteiNo(gassanShiteiNo);

		jp.lg.asp.accommodation.entity.Jichitai jichitai = new jp.lg.asp.accommodation.entity.Jichitai();
		jichitai.setGassanStChar("900");
		when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
		when(gassanRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, gassanShiteiNo))
				.thenReturn(List.of(new jp.lg.asp.accommodation.entity.Gassan()));

		GassanUchi gassanUchi = new GassanUchi();
		gassanUchi.setShiteiNo(SHITEI_NO);
		gassanUchi.setGassanShiteiNo(gassanShiteiNo);
		when(gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(JICHITAI_CD, gassanShiteiNo))
				.thenReturn(List.of(gassanUchi));

		Tokugimu t = buildTokugimu(SHITEI_NO);
		when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(t));
		when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any())).thenReturn(List.of(buildAtena()));
		when(gassanUchiRepository.findByJichitaiCdAndShiteiNoIn(eq(JICHITAI_CD), any()))
				.thenReturn(List.of(gassanUchi));

		Page<?> result = service.search(form);

		assertThat(result.getTotalElements()).isEqualTo(1);
	}

	@Test
	void register_正常系() {
		TokugimuForm form = new TokugimuForm();
		form.setAtenaNo(1L);
		form.setFacilityName("新着施設");

		jp.lg.asp.accommodation.entity.Jichitai jichitai = new jp.lg.asp.accommodation.entity.Jichitai();
		jichitai.setShiteiStChar("000");
		when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
		when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any())).thenReturn(Optional.of(buildAtena()));
		when(tokugimuRepository.findMaxShiteiNoByJichitaiCdAndPrefix(eq(JICHITAI_CD), any()))
				.thenReturn(Optional.of(0));
		when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.register(form);

		verify(tokugimuRepository).save(any(Tokugimu.class));
	}

	@Test
	void register_宛名が見つからない場合は例外を投げる() {
		TokugimuForm form = new TokugimuForm();
		form.setAtenaNo(99L);

		when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.register(form))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("宛名番号が見つかりません");
	}

	@Test
	void updateByShiteiNo_正常系() {
		Tokugimu t = buildTokugimu(SHITEI_NO);
		t.setNewFlg("1");
		TokugimuForm form = new TokugimuForm();
		form.setName("更新後事業者");

		when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of(t));
		when(tokugimuRepository.findMaxRnoByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(Optional.of(1));
		when(atenaRepository.findByJichitaiCdAndAtenaNo(eq(JICHITAI_CD), any())).thenReturn(Optional.of(buildAtena()));
		when(tokugimuRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.updateByShiteiNo(SHITEI_NO, form);

		verify(tokugimuRepository, times(2)).save(any(Tokugimu.class));
		verify(atenaRepository).save(any(Atena.class));
	}

	@Test
	void updateByShiteiNo_指定番号が見つからない場合は例外を投げる() {
		TokugimuForm form = new TokugimuForm();
		when(tokugimuRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO)).thenReturn(List.of());

		assertThatThrownBy(() -> service.updateByShiteiNo(SHITEI_NO, form))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("特別徴収義務者が見つかりません");
	}
}
