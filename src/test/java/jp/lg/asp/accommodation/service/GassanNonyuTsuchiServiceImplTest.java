package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.constant.ReportsConstants;
import jp.lg.asp.accommodation.dto.GassanNonyuTsuchiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.Nokigen;
import jp.lg.asp.accommodation.entity.NokigenId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.NokigenRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.GassanNonyuTsuchiServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GassanNonyuTsuchiServiceImplTest {

	@InjectMocks
	private GassanNonyuTsuchiServiceImpl service;

	@Mock
	private TokugimuRepository tokugimuRepository;

	@Mock
	private AtenaRepository atenaRepository;

	@Mock
	private GassanRepository gassanRepository;

	@Mock
	private NokigenRepository nokigenRepository;

	@Mock
	private ReportsCommonService reportsCommonService;

	@Mock
	private JichitaiContext jichitaiContext;

	private static final String JICHITAI_CD = "123456";
	private static final String SHITEI_NO = "S001";

	@BeforeEach
	void setUp() {
		when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);
	}

	@Nested
	@DisplayName("getGassanNonyuTsuchiInfo メソッドのテスト")
	class GetGassanNonyuTsuchiInfoTest {

		@Test
		@DisplayName("正常系：すべての関連情報が正常に存在し、納入期限が取得できる場合にDTOが返却されること")
		void success_fullData() {
			Jichitai jichitai = new Jichitai();
			jichitai.setName("テスト市");
			jichitai.setNendoStMonth("04");

			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setAtenaNo(BigDecimal.valueOf(1L));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			atena.setYubinNo("1234567");
			atena.setJusho("テスト住所");

			Gassan gassan = new Gassan();
			gassan.setGassanShiteiNo("GS001");
			gassan.setTekiyoStYmd(LocalDate.of(2026, 5, 15));

			Nokigen nokigen = new Nokigen();
			nokigen.setNokigen2nd("20260630");

			byte[] expectedKoin = "公印データ".getBytes();

			when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
			when(reportsCommonService.getReportsDefText(ReportsConstants.GASSAN_NONYU_JOREI)).thenReturn("条例テキスト");
			when(reportsCommonService.getReportsDefData(ReportsConstants.KOIN)).thenReturn(expectedKoin);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, tokugimu.getAtenaNo()))
					.thenReturn(Optional.of(atena));
			when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(List.of(gassan));
			when(nokigenRepository.findById(any(NokigenId.class))).thenReturn(Optional.of(nokigen));

			GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo(SHITEI_NO);

			assertThat(result).isNotNull();
			assertThat(result.getShiteiNo()).isEqualTo(SHITEI_NO);
			assertThat(result.getTokuName()).isEqualTo("テスト宛名");
			assertThat(result.getTokuJusho()).isEqualTo("〒1234567\r\nテスト住所");
			assertThat(result.getGassanShiteiNo()).isEqualTo("GS001");
			assertThat(result.getNonyuKigen()).isEqualTo("6月30日");
			assertThat(result.getCity()).isEqualTo("テスト市");
			assertThat(result.getJorei()).isEqualTo("条例テキスト");
			assertThat(result.getKoin()).isEqualTo(expectedKoin);
		}

		@Test
		@DisplayName("正常系：適用開始月が年度開始月より前の場合（年度を前年に繰り下げて計算）に納入期限が取得できること")
		void success_targetMonthBeforeNendoStMonth() {
			Jichitai jichitai = new Jichitai();
			jichitai.setName("テスト市");
			jichitai.setNendoStMonth("04");

			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setAtenaNo(BigDecimal.ONE);

			Atena atena = new Atena();
			atena.setName("テスト宛名");

			Gassan gassan = new Gassan();
			gassan.setGassanShiteiNo("GS001");
			gassan.setTekiyoStYmd(LocalDate.of(2026, 2, 15)); // 2月 (targetMonth < nendoStMonth)

			Nokigen nokigen = new Nokigen();
			nokigen.setNokigen11th("20260228");

			when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
			when(reportsCommonService.getReportsDefText(any())).thenReturn("条例");
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
					.thenReturn(Optional.of(tokugimu));
			when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.of(atena));
			when(gassanRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of(gassan));
			when(nokigenRepository.findById(any())).thenReturn(Optional.of(nokigen));

			GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo(SHITEI_NO);

			assertThat(result).isNotNull();
			assertThat(result.getNonyuKigen()).isEqualTo("2月28日");
		}

		@Test
		@DisplayName("正常系：郵便番号がnullの場合に住所のみが設定されること")
		void success_yubinNoNull() {
			Jichitai jichitai = new Jichitai();
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setAtenaNo(BigDecimal.ONE);

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			atena.setYubinNo(null);
			atena.setJusho("テスト住所");

			Gassan gassan = new Gassan();
			gassan.setTekiyoStYmd(LocalDate.of(2026, 4, 15));

			Nokigen nokigen = new Nokigen();
			nokigen.setNokigen1st("20260430");

			when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
					.thenReturn(Optional.of(tokugimu));
			when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.of(atena));
			when(gassanRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of(gassan));
			when(nokigenRepository.findById(any())).thenReturn(Optional.of(nokigen));

			GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo(SHITEI_NO);

			assertThat(result).isNotNull();
			assertThat(result.getTokuJusho()).isEqualTo("テスト住所");
		}

		@Test
		@DisplayName("異常系：合算情報が空の場合に例外がスローされること")
		void error_gassanListEmpty() {
			Jichitai jichitai = new Jichitai();
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setAtenaNo(BigDecimal.ONE);

			Atena atena = new Atena();
			atena.setName("テスト宛名");

			when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
					.thenReturn(Optional.of(tokugimu));
			when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.of(atena));
			when(gassanRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(Collections.emptyList());

			assertThatThrownBy(() -> service.getGassanNonyuTsuchiInfo(SHITEI_NO))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("異常系：納入期限の日付文字列の長さが8桁以外の場合に例外がスローされること")
		void error_nokigenYmdInvalidLength() {
			Jichitai jichitai = new Jichitai();
			jichitai.setNendoStMonth("04");

			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setAtenaNo(BigDecimal.ONE);

			Atena atena = new Atena();

			Gassan gassan = new Gassan();
			gassan.setTekiyoStYmd(LocalDate.of(2026, 4, 15));

			Nokigen nokigen = new Nokigen();
			nokigen.setNokigen1st("202604301"); // 9桁（不正）

			when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
					.thenReturn(Optional.of(tokugimu));
			when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.of(atena));
			when(gassanRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of(gassan));
			when(nokigenRepository.findById(any())).thenReturn(Optional.of(nokigen));

			assertThatThrownBy(() -> service.getGassanNonyuTsuchiInfo(SHITEI_NO))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("異常系：全12期以外の期番号（範囲外）の場合に例外がスローされること")
		void error_invalidKi() {
			Jichitai jichitai = new Jichitai();
			jichitai.setNendoStMonth("invalid"); 

			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setAtenaNo(BigDecimal.ONE);
			Atena atena = new Atena();
			Gassan gassan = new Gassan();
			gassan.setTekiyoStYmd(LocalDate.of(2026, 4, 15));

			when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
					.thenReturn(Optional.of(tokugimu));
			when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.of(atena));
			when(gassanRepository.findByJichitaiCdAndShiteiNo(any(), any())).thenReturn(List.of(gassan));

			assertThatThrownBy(() -> service.getGassanNonyuTsuchiInfo(SHITEI_NO))
					.isInstanceOf(Exception.class);
		}

		@Test
		@DisplayName("異常系：特別徴収義務者が見つからない場合に例外がスローされること")
		void error_tokugimuNotFound() {
			when(reportsCommonService.getJichitaiInfo()).thenReturn(new Jichitai());
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
					.thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getGassanNonyuTsuchiInfo(SHITEI_NO))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("異常系：宛名情報が見つからない場合に例外がスローされること")
		void error_atenaNotFound() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setAtenaNo(BigDecimal.ONE);

			when(reportsCommonService.getJichitaiInfo()).thenReturn(new Jichitai());
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
					.thenReturn(Optional.of(tokugimu));
			when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any())).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getGassanNonyuTsuchiInfo(SHITEI_NO))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}
}