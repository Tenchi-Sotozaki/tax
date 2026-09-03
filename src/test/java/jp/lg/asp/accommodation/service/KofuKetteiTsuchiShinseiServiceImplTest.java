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
	@DisplayName("getReportData(String shiteiNo) メソッドのテスト")
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
		@DisplayName("正常系：口座情報の各項目がnullまたは空文字の場合のフォールバック・網羅検証")
		void success_kozaFieldsNullOrEmpty() {
			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(BigDecimal.ONE);
			// 施設郵便番号・住所をnullにして分岐を網羅
			tokugimu.setShisetsuYubinNo(null);
			tokugimu.setShisetsuJusho("");

			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト宛名");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
					.thenReturn(Optional.of(atena));

			Shoreikin shoreikin = new Shoreikin();
			shoreikin.setKofuZeigaku(null); // 数値nullのケース
			shoreikin.setKofuGaku(null);
			shoreikin.setKofuYmd(null); // 交付日nullのケース
			when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
					.thenReturn(Optional.of(shoreikin));

			FurikomiKoza koza = new FurikomiKoza();
			koza.setBankCd(""); // 空文字
			koza.setBankName(null); // null
			koza.setBranchName(null); // null
			koza.setShumoku(""); // 空文字
			koza.setMeigi(null); // null
			koza.setKozaNo(null); // null
			when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(Optional.of(koza));

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

			assertThat(result).isNotNull();
			assertThat(result.getBankCd()).isEqualTo("-1");
			assertThat(result.getBankName()).isEqualTo("****");
			assertThat(result.getBranchName()).isEqualTo("****");
			assertThat(result.getShumoku()).isEqualTo("0");
			assertThat(result.getMeigi()).isEqualTo("****");
			assertThat(result.getFurigana()).isEqualTo("****");
			assertThat(result.getNonyugaku()).isEqualTo("0");
			assertThat(result.getKofugaku()).isEqualTo("0");
		}

		@Test
		@DisplayName("正常系：自治体情報がnull、かつ帳票定義テキストがnullの場合のinit/getReportsDefText分岐網羅")
		void success_jichitaiNullAndReportsDefNull() {
			when(reportsCommonService.getJichitaiInfo()).thenReturn(null);
			when(reportsDefRepository.findByIdAndJichitaiCd(ReportsConstants.KOFU_HAKKO_YOSHIKI, JICHITAI_CD))
					.thenReturn(Optional.empty()); // 定義が見つからないケース

			Tokugimu tokugimu = new Tokugimu();
			tokugimu.setShiteiNo(SHITEI_NO);
			tokugimu.setAtenaNo(BigDecimal.ONE);
			tokugimu.setShisetsuYubinNo("123");
			tokugimu.setShisetsuJusho("住所");
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(JICHITAI_CD, SHITEI_NO, "1", "0"))
					.thenReturn(Optional.of(tokugimu));

			Atena atena = new Atena();
			atena.setName("テスト");
			when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, BigDecimal.ONE))
					.thenReturn(Optional.of(atena));

			Shoreikin shoreikin = new Shoreikin();
			shoreikin.setKofuZeigaku(100L);
			shoreikin.setKofuGaku(50L);
			when(shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(JICHITAI_CD, SHITEI_NO, NENDO))
					.thenReturn(Optional.of(shoreikin));

			when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, SHITEI_NO))
					.thenReturn(Optional.empty());

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);

			assertThat(result).isNotNull();
			assertThat(result.getHakkoYoshiki()).isEmpty();
		}

		@Test
		@DisplayName("異常系：処理中に例外が発生した場合にnullが返却されること（catchブロック網羅）")
		void error_exceptionHandling() {
			when(tokugimuRepository.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(any(), any(), any(), any()))
					.thenThrow(new RuntimeException("DBエラー発生"));

			KofuKetteiTsuchiShinseiDto result = service.getReportData(SHITEI_NO, NENDO);
			assertThat(result).isNull();
		}
	}

	@Nested
	@DisplayName("getAllReportData(String nendo) メソッドのテスト")
	class GetAllReportDataTest {

		@Test
		@DisplayName("正常系：複数件取得時に一部データ（奨励金・交付日など）が欠落している場合の分岐網羅")
		void success_getAllReportData_edgeCases() {
			Tokugimu tokugimu1 = new Tokugimu();
			tokugimu1.setShiteiNo("S001");
			tokugimu1.setAtenaNo(BigDecimal.ONE);
			tokugimu1.setShisetsuYubinNo("111-1111");
			tokugimu1.setShisetsuJusho("東京都");

			when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of(tokugimu1));

			Atena atena1 = new Atena();
			atena1.setAtenaNo(BigDecimal.ONE);
			atena1.setName("宛名1");
			when(atenaRepository.findByJichitaiCdAndAtenaNoIn(eq(JICHITAI_CD), any()))
					.thenReturn(List.of(atena1));

			Shoreikin shoreikin1 = new Shoreikin();
			shoreikin1.setShiteiNo("S001");
			shoreikin1.setKofuZeigaku(null);
			shoreikin1.setKofuGaku(null);
			shoreikin1.setKofuYmd(null); // 交付日null
			when(shoreikinRepository.findByJichitaiCdAndShiteiNoInAndNendo(eq(JICHITAI_CD), any(), eq(NENDO)))
					.thenReturn(List.of(shoreikin1));

			FurikomiKoza koza = new FurikomiKoza();
			koza.setBankCd("0001");
			koza.setBankName("銀行");
			koza.setBranchName("本店"); // 本店キーワード
			koza.setShumoku("1");
			koza.setMeigi("テスト 名義");
			koza.setKozaNo("1234567");
			when(furikomiKozaRepository.findByJichitaiCdAndShiteiNo(JICHITAI_CD, "S001"))
					.thenReturn(Optional.of(koza));

			List<KofuKetteiTsuchiShinseiDto> result = service.getAllReportData(NENDO);

			assertThat(result).isNotNull();
			assertThat(result).hasSize(1);
			assertThat(result.get(0).getBranchShubetsu()).isEqualTo("本店");
		}

		@Test
		@DisplayName("境界値：引数の年度がnullまたはブランクの場合に空リストを返すこと")
		void boundary_nendoNullOrBlank() {
			assertThat(service.getAllReportData(null)).isEmpty();
			assertThat(service.getAllReportData("")).isEmpty();
		}

		@Test
		@DisplayName("異常系：特別徴収義務者リストが空の場合に空リストを返すこと")
		void error_tokugimuEmpty() {
			when(tokugimuRepository.findAllByJichitaiCd(JICHITAI_CD)).thenReturn(List.of());
			assertThat(service.getAllReportData(NENDO)).isEmpty();
		}
	}
}