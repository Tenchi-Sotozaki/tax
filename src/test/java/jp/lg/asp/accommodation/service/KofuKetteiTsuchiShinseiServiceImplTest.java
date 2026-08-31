package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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
import jp.lg.asp.accommodation.dto.KofuKetteiTsuchiShinseiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.FurikomiKoza;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.entity.ReportsDef;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FurikomiKozaRepository;
import jp.lg.asp.accommodation.repository.ReportsDefRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.impl.KofuKetteiTsuchiShinseiServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KofuKetteiTsuchiShinseiServiceImplTest {

	@InjectMocks
	private KofuKetteiTsuchiShinseiServiceImpl service;

	@Mock
	private TokugimuRepository tokugimuRepository;

	@Mock
	private AtenaRepository atenaRepository;

	@Mock
	private ShoreikinRepository shoreikinRepository;

	@Mock
	private ReportsDefRepository reportsDefRepository;

	@Mock
	private ReportsCommonService reportsCommonService;

	@Mock
	private FurikomiKozaRepository furikomiKozaRepository;

	@Mock
	private JichitaiContext jichitaiContext;

	private static final String JICHITAI_CD = "123456";
	private static final String SHITEI_NO = "S001";
	private static final String NENDO = "2025";

	@BeforeEach
	void setUp() {
		when(jichitaiContext.getJichitaiCd()).thenReturn(JICHITAI_CD);

		Jichitai jichitai = new Jichitai();
		jichitai.setName("テスト市");
		jichitai.setKbnName("市");
		when(reportsCommonService.getJichitaiInfo()).thenReturn(jichitai);

		when(reportsCommonService.getReportsDefText(ReportsConstants.SHOREIKIN_KOFU_JOREI)).thenReturn("テスト条例");
		when(reportsCommonService.getReportsDefData(ReportsConstants.KOIN)).thenReturn(new byte[]{1, 2, 3});

		ReportsDef reportsDefYoshiki = new ReportsDef();
		reportsDefYoshiki.setDefText("テスト様式");
		when(reportsDefRepository.findByIdAndJichitaiCd(ReportsConstants.KOFU_HAKKO_YOSHIKI, JICHITAI_CD))
				.thenReturn(Optional.of(reportsDefYoshiki));

		ReportsDef reportsDefJoken = new ReportsDef();
		reportsDefJoken.setDefText("テスト条件");
		when(reportsDefRepository.findByIdAndJichitaiCd(ReportsConstants.KOFU_JOKEN, JICHITAI_CD))
				.thenReturn(Optional.of(reportsDefJoken));
	}

	@Nested
	@DisplayName("getReportData(String shiteiNo) メソッドのテスト（現在年度取得）")
	class GetReportDataSingleArgTest {

		@Test
		@DisplayName("正常系：現在年度を自動判定し、指定番号に紐づく有効な交付申請書データを正常に取得できること")
		void success_currentNendo() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(BigDecimal.ONE);
			tokugimu.setShisetsuName("テスト施設");
			tokugimu.setShisetsuYubinNo("123-4567");
			tokugimu.setShisetsuJusho("テスト住所");

			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
					.thenReturn(Optional.of(atena));

			LocalDate now = LocalDate.now();
			String expectedNendo = String.valueOf(now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1);

			Shoreikin shoreikin = new Shoreikin();
			shoreikin.setKofuZeigaku(1000L);
			shoreikin.setKofuGaku(500L);
			shoreikin.setKofuYmd(LocalDate.of(2025, 6, 1));
			when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, expectedNendo))
					.thenReturn(Optional.of(shoreikin));

			FurikomiKoza koza = new FurikomiKoza();
			koza.setBankCd("0001");
			koza.setBankName("テスト銀行");
			koza.setBranchName("テスト支店");
			koza.setShumoku("1");
			koza.setMeigi("テストメイギ");
			koza.setKozaNo("1234567");
			when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(Optional.of(koza));

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO);

			assertThat(result).isNotNull();
			assertThat(result.getShiteiNo()).isEqualTo(SHITEI_NO);
			assertThat(result.getNendo()).isEqualTo(expectedNendo);
			assertThat(result.getTokuName()).isEqualTo("テスト宛名");
		}
	}

	@Nested
	@DisplayName("getReportData(String shiteiNo, String nendo) メソッドのテスト")
	class GetReportDataDoubleArgTest {

		@Test
		@DisplayName("正常系：指定された年度と指定番号に基づいて、口座・奨励金・宛名を含む交付申請書データを正常に取得できること")
		void success_withKozaAndKeywords() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(BigDecimal.ONE);
			tokugimu.setShisetsuName("テスト施設");
			tokugimu.setShisetsuYubinNo("123-4567");
			tokugimu.setShisetsuJusho("テスト住所");

			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
					.thenReturn(Optional.of(atena));

			Shoreikin shoreikin = new Shoreikin();
			shoreikin.setKofuZeigaku(1000L);
			shoreikin.setKofuGaku(500L);
			shoreikin.setKofuYmd(LocalDate.of(2025, 6, 1));
			when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
					.thenReturn(Optional.of(shoreikin));

			FurikomiKoza koza = new FurikomiKoza();
			koza.setBankCd("0001");
			koza.setBankName("テスト銀行");
			koza.setBranchName("東京支店");
			koza.setShumoku("1");
			koza.setMeigi("テスト口座");
			koza.setKozaNo("1234567");
			when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(Optional.of(koza));

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

			assertThat(result).isNotNull();
			assertThat(result.getShiteiNo()).isEqualTo(SHITEI_NO);
			assertThat(result.getNendo()).isEqualTo(NENDO);
			assertThat(result.getBankName()).isEqualTo("テスト");
			assertThat(result.getBranchName()).isEqualTo("東京");
			assertThat(result.getBranchShubetsu()).isEqualTo("支店");
			assertThat(result.getKozaNo()).isEqualTo(List.of("1", "2", "3", "4", "5", "6", "7"));
		}

		@Test
		@DisplayName("境界値：口座番号が正確に7桁の場合、および7桁未満の場合にそれぞれ正しくアスタリスク埋めされること")
		void boundary_kozaNoFormatting() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(BigDecimal.ONE);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
					.thenReturn(Optional.of(atena));

			Shoreikin shoreikin = new Shoreikin();
			shoreikin.setKofuZeigaku(1000L);
			shoreikin.setKofuGaku(500L);
			when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
					.thenReturn(Optional.of(shoreikin));

			FurikomiKoza koza = new FurikomiKoza();
			koza.setBankCd("");
			koza.setBankName("");
			koza.setBranchName("");
			koza.setShumoku("");
			koza.setMeigi("");
			koza.setKozaNo("123"); // 3桁（7桁未満）
			when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(Optional.of(koza));

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

			assertThat(result).isNotNull();
			assertThat(result.getKozaNo()).isEqualTo(List.of("1", "2", "3", "*", "*", "*", "*"));
		}
		
		@Test
		@DisplayName("境界値：支店名（branchName）がnullまたは空文字の場合に、そのまま処理が継続されること")
		void boundary_branchNameNullOrEmpty() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(BigDecimal.ONE);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
					.thenReturn(Optional.of(atena));

			Shoreikin shoreikin = new Shoreikin();
			shoreikin.setKofuZeigaku(1000L);
			shoreikin.setKofuGaku(500L);
			when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
					.thenReturn(Optional.of(shoreikin));

			FurikomiKoza koza = new FurikomiKoza();
			koza.setBankCd("0001");
			koza.setBankName("テスト");
			koza.setBranchName(null); // 支店名がnull
			koza.setShumoku("1");
			koza.setMeigi("テスト口座");
			koza.setKozaNo("1234567");
			when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(Optional.of(koza));

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

			assertThat(result).isNotNull();
			assertThat(result.getBranchName()).isEqualTo("****");
		}

		@Test
		@DisplayName("境界値：口座番号（kozaNo）の桁数が7桁より長い場合や短い場合のパディング・切り捨て処理の網羅")
		void boundary_kozaNoLengthVariation() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(BigDecimal.ONE);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
					.thenReturn(Optional.of(atena));

			Shoreikin shoreikin = new Shoreikin();
			shoreikin.setKofuZeigaku(1000L);
			shoreikin.setKofuGaku(500L);
			when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
					.thenReturn(Optional.of(shoreikin));

			FurikomiKoza koza = new FurikomiKoza();
			koza.setBankCd("0001");
			koza.setBankName("テスト");
			koza.setBranchName("支店");
			koza.setShumoku("1");
			koza.setMeigi("名義");
			koza.setKozaNo("1234567890"); // 7桁より長いケース
			when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(Optional.of(koza));

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

			assertThat(result).isNotNull();
			assertThat(result.getKozaNo()).hasSize(10);
		}

		@Test
		@DisplayName("異常系：特別徴収義務者情報が見つからない場合に、処理が中断されnullが返却されること")
		void error_tokugimuNotFound() {
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.empty());

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

			assertThat(result).isNull();
		}

		@Test
		@DisplayName("異常系：宛名情報が見つからない場合に、処理が中断されnullが返却されること")
		void error_atenaNotFound() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(BigDecimal.ONE);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
					.thenReturn(Optional.empty());

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

			assertThat(result).isNull();
		}

		@Test
		@DisplayName("異常系：奨励金情報が見つからない場合に、エラーログ出力後nullが返却されること")
		void error_shoreikinNotFound() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(BigDecimal.ONE);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
					.thenReturn(Optional.of(atena));

			when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, "9999"))
					.thenReturn(Optional.empty());

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, "9999");

			assertThat(result).isNull();
		}

		@Test
		@DisplayName("異常系：口座情報が存在しない場合に、各口座項目がマスク値で代替されること")
		void error_kozaNotFoundMasked() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(BigDecimal.ONE);
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
					.thenReturn(Optional.of(atena));

			Shoreikin shoreikin = new Shoreikin();
			shoreikin.setKofuZeigaku(1000L);
			shoreikin.setKofuGaku(500L);
			when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
					.thenReturn(Optional.of(shoreikin));

			when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(Optional.empty());

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

			assertThat(result).isNotNull();
			assertThat(result.getBankCd()).isEqualTo("-1");
			assertThat(result.getBankName()).isEqualTo("****");
			assertThat(result.getKozaNo()).isEqualTo(List.of("*", "*", "*", "*", "*", "*", "*"));
		}

		@Test
		@DisplayName("異常系：リポジトリ層や処理内部で予期せぬ例外が発生した場合に、キャッチされてnullが返却されること")
		void error_exceptionThrown() {
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
					.thenThrow(new RuntimeException("DB Connection Error"));

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

			assertThat(result).isNull();
		}
	}

	@Nested
	@DisplayName("getAllReportData(String nendo) メソッドのテスト")
	class GetAllReportDataTest {

		@Test
		@DisplayName("正常系：指定年度に合致するすべての特別徴収義務者分の帳票データリストが正常に取得できること")
		void success_getAllReportData() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(BigDecimal.ONE);
			tokugimu.setShisetsuName("テスト施設");
			when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(tokugimu));

			Atena atena = new Atena();
			atena.setAtenaNo(BigDecimal.ONE);
			atena.setName("テスト宛名");
			when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any()))
					.thenReturn(List.of(atena));

			Shoreikin shoreikin = new Shoreikin();
			shoreikin.setShiteiNo(SHITEI_NO);
			shoreikin.setKofuZeigaku(1000L);
			shoreikin.setKofuGaku(500L);
			when(shoreikinRepository.findByJichitaiCdAndShiteiNoInAndNendo(eq(JICHITAI_CD), any(), eq(NENDO)))
					.thenReturn(List.of(shoreikin));

			FurikomiKoza koza = new FurikomiKoza();
			koza.setBankCd("0001");
			koza.setBankName("テスト銀行");
			when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(Optional.of(koza));

			List<KofuKetteiTsuchiShinseiDto> result = service.getAllReportData(NENDO);

			assertThat(result).isNotNull();
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getShiteiNo()).isEqualTo(SHITEI_NO);
		}

		@Test
		@DisplayName("境界値：引数の年度（nendo）がnullまたはブランク文字の場合に、空のリストが即座に返却されること")
		void boundary_nendoNullOrBlank() {
			assertThat(service.getAllReportData(null)).isEmpty();
			assertThat(service.getAllReportData("")).isEmpty();
			assertThat(service.getAllReportData("   ")).isEmpty();
		}

		@Test
		@DisplayName("異常系：特別徴収義務者データが存在しない場合に、空のリストが返却されること")
		void error_tokugimuListEmpty() {
			when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());

			List<KofuKetteiTsuchiShinseiDto> result = service.getAllReportData(NENDO);

			assertThat(result).isEmpty();
		}
		
		@Test
		@DisplayName("異常系：全件取得時に一部の特別徴収義務者に対応する奨励金データが存在しない場合、該当データが除外されて正常に取得できること")
		void error_getAllReportDataPartialMissing() {
			Tokugimu tokugimu1 = new Tokugimu();
			tokugimu1.setShiteiNo("S001");
			tokugimu1.setAtenaNo(BigDecimal.ONE);
			Tokugimu tokugimu2 = new Tokugimu();
			tokugimu2.setShiteiNo("S002");
			tokugimu2.setAtenaNo(BigDecimal.valueOf(2));

			when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(tokugimu1, tokugimu2));

			Atena atena1 = new Atena();
			atena1.setAtenaNo(BigDecimal.ONE);
			atena1.setName("宛名1");
			Atena atena2 = new Atena();
			atena2.setAtenaNo(BigDecimal.valueOf(2));
			atena2.setName("宛名2");

			when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any()))
					.thenReturn(List.of(atena1, atena2));

			// S001の奨励金はあるが、S002の奨励金が存在しないケース
			Shoreikin shoreikin1 = new Shoreikin();
			shoreikin1.setShiteiNo("S001");
			shoreikin1.setKofuZeigaku(1000L);
			shoreikin1.setKofuGaku(500L);

			when(shoreikinRepository.findByJichitaiCdAndShiteiNoInAndNendo(eq(JICHITAI_CD), any(), eq(NENDO)))
					.thenReturn(List.of(shoreikin1));

			when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(any(), any()))
					.thenReturn(Optional.empty());

			List<KofuKetteiTsuchiShinseiDto> result = service.getAllReportData(NENDO);

			assertThat(result).isNotNull();
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getShiteiNo()).isEqualTo("S001");
		}
	}
}