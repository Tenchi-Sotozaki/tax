package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.dto.AtenaImportPreviewDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;
import jp.lg.asp.accommodation.entity.AtenaRenkei;
import jp.lg.asp.accommodation.entity.AtenaRenkeiDef;
import jp.lg.asp.accommodation.repository.AtenaRenkeiDefRepository;
import jp.lg.asp.accommodation.repository.AtenaRenkeiRepository;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.service.impl.AtenaImportServiceImpl;
import jp.lg.asp.accommodation.util.HashUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtenaImportServiceImplTest {

	@Mock
	private AtenaRepository atenaRepository;

	@Mock
	private AtenaRenkeiRepository atenaRenkeiRepository;

	@Mock
	private AtenaRenkeiDefRepository atenaRenkeiDefRepository;

	@Mock
	private HashUtil hashUtil;

	@InjectMocks
	private AtenaImportServiceImpl atenaImportService;

	private static final String JICHITAI_CD = "123456";

	@Nested
	@DisplayName("analyze メソッドのテスト")
	class AnalyzeTest {

		@Test
		@DisplayName("正常系：有効なCSV（BOM付き、既存データあり・差分あり、エスケープ・引用符含む）を解析できること")
		void analyze_validCsvWithDiff_returnsPreview() {
			String csvContent = "\uFEFF"
					+ "\"宛名番号\",\"個人番号\",\"法人番号\",\"氏名/名称\",\"ふりがな\",\"郵便番号\",\"住所\",\"電話番号\"\r\n"
					+ "\"1\",\"123456789012\",\"HOJIN001\",\"山田 太郎\",\"ヤマダ タロウ\",\"100-0001\",\"東京都千代田区1-1\",\"03-0000-0000\"";
			MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
					csvContent.getBytes(StandardCharsets.UTF_8));

			// 既存データ（差分が発生する内容）
			Atena existing = new Atena();
			existing.setName("山田 変更前");
			existing.setNameKana("ヤマダ ヘンコウマエ");
			existing.setYubinNo("999-9999");
			existing.setJusho("旧住所");
			existing.setTel1("03-9999-9999");
			existing.setTel2("090-9999-9999");
			existing.setHojinNo("OLD_HOJIN");
			existing.setKojinNo("old_hashed");

			when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.of(existing));
			when(hashUtil.sha256(anyString())).thenReturn("new_hashed");

			AtenaImportPreviewDto result = atenaImportService.analyze(file, JICHITAI_CD);

			assertThat(result).isNotNull();
			assertThat(result.getRows()).hasSize(1);
			assertThat(result.getRows().get(0).isShinki()).isFalse();
			assertThat(result.getRows().get(0).isSabunAri()).isTrue();
		}

		@Test
		@DisplayName("正常系：空白行や項目数が少ない不正行のスキップ、および既存データと差分なしの場合の処理")
		void analyze_blankLinesAndNoDiff_returnsPreview() {
			String csvContent = "宛名番号,個人番号,法人番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\n"
					+ "\n" // 空行（スキップされる）
					+ "1,,,山田 太郎,ヤマダ タロウ,100-0001,東京都,03-0000-0000";
			MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
					csvContent.getBytes(StandardCharsets.UTF_8));

			// 既存データ（CSVと完全に一致＝差分なし）
			Atena existing = new Atena();
			existing.setName("山田 太郎");
			existing.setNameKana("ヤマダ タロウ");
			existing.setYubinNo("100-0001");
			existing.setJusho("東京都");
			existing.setTel1("03-0000-0000");

			when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.of(existing));

			AtenaImportPreviewDto result = atenaImportService.analyze(file, JICHITAI_CD);

			assertThat(result).isNotNull();
			assertThat(result.getRows()).hasSize(1);
			assertThat(result.getRows().get(0).isShinki()).isFalse();
			assertThat(result.getRows().get(0).isSabunAri()).isFalse();
		}

		@Test
		@DisplayName("異常系：CSVファイルが空の場合に例外がスローされること")
		void analyze_emptyFile_throwsException() {
			MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

			assertThatThrownBy(() -> atenaImportService.analyze(file, JICHITAI_CD))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("CSVファイルが空です。");
		}

		@Test
		@DisplayName("異常系：ヘッダー行が空の場合に例外がスローされること")
		void analyze_blankHeader_throwsException() {
			MockMultipartFile file = new MockMultipartFile("file", "blank.csv", "text/csv",
					"\n1,,,A,B,C,D,E".getBytes(StandardCharsets.UTF_8));

			assertThatThrownBy(() -> atenaImportService.analyze(file, JICHITAI_CD))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("ヘッダー行が空です。");
		}

		@Test
		@DisplayName("異常系：ヘッダーの項目数不足または項目名不正の場合に例外がスローされること")
		void analyze_invalidHeader_throwsException() {
			String csvContent = "不正ヘッダー,個人番号\n1,2";
			MockMultipartFile file = new MockMultipartFile("file", "invalid.csv", "text/csv",
					csvContent.getBytes(StandardCharsets.UTF_8));

			assertThatThrownBy(() -> atenaImportService.analyze(file, JICHITAI_CD))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("CSVファイルのフォーマットが不正です。");
		}

		@Test
		@DisplayName("異常系：ヘッダー項目名が期待値と異なる場合に例外がスローされること")
		void analyze_mismatchedHeaderName_throwsException() {
			String csvContent = "誤ったヘッダー,個人番号,法人番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\n1,,,A,B,C,D,E";
			MockMultipartFile file = new MockMultipartFile("file", "mismatch.csv", "text/csv",
					csvContent.getBytes(StandardCharsets.UTF_8));

			assertThatThrownBy(() -> atenaImportService.analyze(file, JICHITAI_CD))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("ヘッダーの1番目の項目が不正です。");
		}

		@Test
		@DisplayName("異常系：データ行の項目数が不足している場合に例外がスローされること")
		void analyze_insufficientDataColumns_throwsException() {
			String csvContent = "宛名番号,個人番号,法人番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\n1,2,3";
			MockMultipartFile file = new MockMultipartFile("file", "short.csv", "text/csv",
					csvContent.getBytes(StandardCharsets.UTF_8));

			assertThatThrownBy(() -> atenaImportService.analyze(file, JICHITAI_CD))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("データの項目数が不足です。");
		}

		@Test
		@DisplayName("異常系：宛名番号が空または数値ではない場合に例外がスローされること")
		void analyze_invalidAtenaNo_throwsException() {
			// 宛名番号が空
			String csvContent1 = "宛名番号,個人番号,法人番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\n ,,,テスト,テスト,100-0001,住所,03-0000-0000";
			MockMultipartFile file1 = new MockMultipartFile("file", "f1.csv", "text/csv",
					csvContent1.getBytes(StandardCharsets.UTF_8));
			assertThatThrownBy(() -> atenaImportService.analyze(file1, JICHITAI_CD))
					.hasMessageContaining("宛名番号が空です。");

			// 宛名番号が数値ではない
			String csvContent2 = "宛名番号,個人番号,法人番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\nABC,,,テスト,テスト,100-0001,住所,03-0000-0000";
			MockMultipartFile file2 = new MockMultipartFile("file", "f2.csv", "text/csv",
					csvContent2.getBytes(StandardCharsets.UTF_8));
			assertThatThrownBy(() -> atenaImportService.analyze(file2, JICHITAI_CD))
					.hasMessageContaining("宛名番号が数値ではありません。");
		}

		@Test
		@DisplayName("異常系：必須項目（氏名、ふりがな、電話番号1）が空の場合に例外がスローされること")
		void analyze_missingRequiredFields_throwsException() {
			// 氏名が空
			String csvContent1 = "宛名番号,個人番号,法人番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\n1,,, ,ふりがな,100-0001,住所,03-0000-0000";
			MockMultipartFile f1 = new MockMultipartFile("file", "f.csv", "text/csv",
					csvContent1.getBytes(StandardCharsets.UTF_8));
			assertThatThrownBy(() -> atenaImportService.analyze(f1, JICHITAI_CD)).hasMessageContaining("氏名が空です。");

			// ふりがなが空
			String csvContent2 = "宛名番号,個人番号,法人番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\n1,,,氏名, ,100-0001,住所,03-0000-0000";
			MockMultipartFile f2 = new MockMultipartFile("file", "f.csv", "text/csv",
					csvContent2.getBytes(StandardCharsets.UTF_8));
			assertThatThrownBy(() -> atenaImportService.analyze(f2, JICHITAI_CD)).hasMessageContaining("氏名カナが空です。");

			// 電話番号1が空
			String csvContent3 = "宛名番号,個人番号,法人番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\n1,,,氏名,ふりがな,100-0001,住所, ";
			MockMultipartFile f3 = new MockMultipartFile("file", "f.csv", "text/csv",
					csvContent3.getBytes(StandardCharsets.UTF_8));
			assertThatThrownBy(() -> atenaImportService.analyze(f3, JICHITAI_CD)).hasMessageContaining("電話番号1が空です。");
		}

		@Test
		@DisplayName("異常系：予期しないRuntimeException以外の例外が発生した場合にラップされてスローされること")
		void analyze_unexpectedException_wrapsAndThrows() {
			// 不正なファイルを指定してInputStream等で強制的に予期せぬエラーを起こす、あるいはモックで例外を誘発
			// ここではコンストラクタ等でIOExceptionを投げるモックファイル等の代わりに、親クラスの例外を検証
			MultipartFile mockFile = mock(MultipartFile.class);
			try {
				when(mockFile.getOriginalFilename()).thenReturn("error.csv");
				when(mockFile.getInputStream()).thenThrow(new java.io.IOException("IO Error"));
			} catch (Exception ignored) {
			}

			assertThatThrownBy(() -> atenaImportService.analyze(mockFile, JICHITAI_CD))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("CSV取込に失敗しました:");
		}
	}

	@Nested
	@DisplayName("confirm メソッドのテスト")
	class ConfirmTest {

		@Test
		@DisplayName("正常系：新規、差分なし、取り込み対象（差分あり選択）、スキップ（差分あり未選択）の全パターンを網羅して確定処理ができること")
		void confirm_allBranchPatterns_savesCorrectly() {
			AtenaImportPreviewDto preview = new AtenaImportPreviewDto();
			preview.setFileName("test.csv");

			// 行1: 新規登録 (shinki = true)
			var row1 = new jp.lg.asp.accommodation.dto.AtenaImportRowDto();
			row1.setAtenaNo("1");
			row1.setName("新規 太郎");
			row1.setShinki(true);
			var val1 = new jp.lg.asp.accommodation.dto.AtenaImportValueDto();
			val1.setAtenaNo("1");
			val1.setName("新規 太郎");
			row1.setValue(val1);

			// 行2: 差分なし (shinki = false, sabunAri = false)
			var row2 = new jp.lg.asp.accommodation.dto.AtenaImportRowDto();
			row2.setAtenaNo("2");
			row2.setName("変更無 花子");
			row2.setShinki(false);
			row2.setSabunAri(false);

			// 行3: 差分あり・取り込み対象 (shinki = false, sabunAri = true, torikomuAtenaNoに含まれる)
			var row3 = new jp.lg.asp.accommodation.dto.AtenaImportRowDto();
			row3.setAtenaNo("3");
			row3.setName("更新 次郎");
			row3.setShinki(false);
			row3.setSabunAri(true);
			var val3 = new jp.lg.asp.accommodation.dto.AtenaImportValueDto();
			val3.setAtenaNo("3");
			val3.setName("更新 次郎");
			row3.setValue(val3);

			// 行4: 差分あり・スキップ (shinki = false, sabunAri = true, torikomuAtenaNoに含まれない)
			var row4 = new jp.lg.asp.accommodation.dto.AtenaImportRowDto();
			row4.setAtenaNo("4");
			row4.setName("スキップ 三郎");
			row4.setShinki(false);
			row4.setSabunAri(true);

			preview.getRows().add(row1);
			preview.getRows().add(row2);
			preview.getRows().add(row3);
			preview.getRows().add(row4);

			when(atenaRenkeiRepository.findMaxSeqByJichitaiCd(JICHITAI_CD)).thenReturn(BigDecimal.ZERO);
			when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.empty());
			when(atenaRenkeiRepository.save(any(AtenaRenkei.class))).thenAnswer(invocation -> invocation.getArgument(0));

			// AtenaNo "3" を取り込み対象に指定
			Set<String> torikomuSet = Set.of("3");

			AtenaRenkei result = atenaImportService.confirm(preview, torikomuSet, JICHITAI_CD, "user1");

			assertThat(result).isNotNull();
			assertThat(result.getSeq()).isEqualTo(BigDecimal.ONE);
			assertThat(result.getShinkiKensu()).isEqualByComparingTo(BigDecimal.ONE);
			assertThat(result.getKoshinKensu()).isEqualByComparingTo(BigDecimal.ONE);
			assertThat(result.getShoriKensu()).isEqualByComparingTo(BigDecimal.valueOf(2));

			verify(atenaRenkeiDefRepository, times(4)).save(any(AtenaRenkeiDef.class));
		}
	}

	@Nested
	@DisplayName("参照メソッドのテスト")
	class FindMethodsTest {

		@Test
		@DisplayName("正常系：連携履歴および詳細情報が正しく取得できること")
		void findMethods_returnsExpectedLists() {
			when(atenaRenkeiRepository.findByJichitaiCdOrderBySeqDesc(JICHITAI_CD)).thenReturn(List.of(new AtenaRenkei()));
			when(atenaRenkeiDefRepository.findByJichitaiCdAndSeqOrderByAtenaNoAsc(JICHITAI_CD, BigDecimal.ONE))
					.thenReturn(List.of(new AtenaRenkeiDef()));

			List<AtenaRenkei> history = atenaImportService.findHistory(JICHITAI_CD);
			List<AtenaRenkeiDef> detail = atenaImportService.findDetail(JICHITAI_CD, BigDecimal.ONE);

			assertThat(history).hasSize(1);
			assertThat(detail).hasSize(1);
		}
	}
}