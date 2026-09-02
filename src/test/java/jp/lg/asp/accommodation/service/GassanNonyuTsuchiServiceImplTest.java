package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
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

		Jichitai jichitai = new Jichitai();
		jichitai.setName("テスト市");
		jichitai.setNendoStMonth("4"); // デフォルトで4月を設定
		when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);
		when(reportsCommonService.getReportsDefText(ReportsConstants.GASSAN_NONYU_JOREI)).thenReturn("テスト条例");
		when(reportsCommonService.getReportsDefData(ReportsConstants.KOIN)).thenReturn(new byte[]{1, 2, 3});
	}

	@Nested
	@DisplayName("getGassanNonyuTsuchiInfo(String shiteiNo) メソッドのテスト")
	class GetGassanNonyuTsuchiInfoTest {

		@Test
		@DisplayName("正常系：指定番号に紐づく合算申告納入通知書情報が正常に取得できること（4月以降の適用開始日）")
		void success_normalCase() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(java.math.BigDecimal.ONE);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			atena.setYubinNo("123-4567");
			atena.setJusho("テスト住所");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, java.math.BigDecimal.ONE))
					.thenReturn(Optional.of(atena));

			Gassan gassan = new Gassan();
			gassan.setGassanShiteiNo("G999");
			gassan.setTekiyoStYmd(LocalDate.of(2025, 6, 15)); // 6月（ki = 3）
			when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(List.of(gassan));

			Nokigen nokigen = new Nokigen();
			nokigen.setNokigen3rd("20250731"); // 第3期の納入期限
			when(nokigenRepository.findById(any())).thenReturn(Optional.of(nokigen));

			GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo(SHITEI_NO);

			assertThat(result).isNotNull();
			assertThat(result.getShiteiNo()).isEqualTo(SHITEI_NO);
			assertThat(result.getTokuName()).isEqualTo("テスト宛名");
			assertThat(result.getTokuJusho()).contains("〒123-4567").contains("テスト住所");
			assertThat(result.getGassanShiteiNo()).isEqualTo("G999");
			assertThat(result.getNonyuKigen()).isEqualTo("7月31日");
			assertThat(result.getCity()).isEqualTo("テスト市");
			assertThat(result.getJorei()).isEqualTo("テスト条例");
		}

		@Test
		@DisplayName("正常系：適用開始月が年度開始月より前（例：3月）の場合の年度計算分岐の検証")
		void success_targetMonthBeforeNendoStMonth() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(java.math.BigDecimal.ONE);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, java.math.BigDecimal.ONE))
					.thenReturn(Optional.of(atena));

			Gassan gassan = new Gassan();
			gassan.setGassanShiteiNo("G999");
			gassan.setTekiyoStYmd(LocalDate.of(2025, 3, 15)); // 3月（targetMonth < nendoStMonth）
			when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(List.of(gassan));

			Nokigen nokigen = new Nokigen();
			nokigen.setNokigen12th("20250331");
			when(nokigenRepository.findById(any())).thenReturn(Optional.of(nokigen));

			GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo(SHITEI_NO);

			assertThat(result).isNotNull();
			assertThat(result.getNonyuKigen()).isEqualTo("3月31日");
		}

		@Test
		@DisplayName("正常系：郵便番号がnullの場合の住所組み立て分岐の検証")
		void success_yubinNoNull() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(java.math.BigDecimal.ONE);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			atena.setYubinNo(null); // 郵便番号 null
			atena.setJusho("テスト住所のみ");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, java.math.BigDecimal.ONE))
					.thenReturn(Optional.of(atena));

			when(gassanRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(List.of()); // 合算情報なし

			GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo(SHITEI_NO);

			assertThat(result).isNotNull();
			assertThat(result.getTokuJusho()).isEqualTo("テスト住所のみ");
		}

		@Test
		@DisplayName("異常系：特別徴収義務者が見つからない場合にnullが返却されること")
		void error_tokugimuNotFound() {
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
					.thenReturn(Optional.empty());

			GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo("INVALID");
			assertThat(result).isNull();
		}

		@Test
		@DisplayName("異常系：宛名情報が見つからない場合にnullが返却されること")
		void error_atenaNotFound() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(java.math.BigDecimal.ONE);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
					.thenReturn(Optional.of(tokugimu));

			when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any()))
					.thenReturn(Optional.empty());

			GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo(SHITEI_NO);
			assertThat(result).isNull();
		}

		@Test
		@DisplayName("境界値：納入期限の日付文字列の長さが8桁以外の場合に納入期限が設定されないこと")
		void error_nokigenYmdInvalidLength() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(java.math.BigDecimal.ONE);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(any(), any()))
					.thenReturn(Optional.of(atena));

			Gassan gassan = new Gassan();
			gassan.setTekiyoStYmd(LocalDate.of(2025, 4, 15)); // ki = 1
			when(gassanRepository.findByJichitaiCdAndShiteiNo(any(), any()))
					.thenReturn(List.of(gassan));

			Nokigen nokigen = new Nokigen();
			nokigen.setNokigen1st("202504"); // 8桁未満の不正な長さ
			when(nokigenRepository.findById(any())).thenReturn(Optional.of(nokigen));

			GassanNonyuTsuchiDto result = service.getGassanNonyuTsuchiInfo(SHITEI_NO);

			assertThat(result).isNotNull();
			assertThat(result.getNonyuKigen()).isNull();
		}
	}
}