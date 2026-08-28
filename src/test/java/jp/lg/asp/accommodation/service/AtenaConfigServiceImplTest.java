package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.exception.BusinessException;
import jp.lg.asp.accommodation.exception.ResourceNotFoundException;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.impl.AtenaConfigServiceImpl;
import jp.lg.asp.accommodation.util.HashUtil;

@ExtendWith(MockitoExtension.class)
class AtenaConfigServiceImplTest {

	@InjectMocks
	private AtenaConfigServiceImpl atenaConfigService;

	@Mock
	private AtenaRepository atenaRepository;

	@Mock
	private JichitaiRepository jichitaiRepository;

	@Mock
	private HashUtil hashUtil;
	
    private static final String JICHITAI_CD = "123456";

	@Nested
	@DisplayName("register メソッドのテスト")
	class RegisterTest {

		@Test
		@DisplayName("正常系：個人番号が指定されており、重複がない場合に正常に登録され、区分が「1」かつ個人番号がハッシュ化されること")
		void success_kojinNo() {
			String jichitaiCd = "123456";
			Jichitai jichitai = new Jichitai();
			Atena atena = new Atena();
			atena.setKojinNo("123456789012");

			when(jichitaiRepository.findById(jichitaiCd)).thenReturn(Optional.of(jichitai));
			when(hashUtil.sha256("123456789012")).thenReturn("hashed_kojin");
			when(atenaRepository.existsByKojinNo(jichitaiCd, "hashed_kojin", null)).thenReturn(false);
			when(atenaRepository.findMaxAtenaNoByJichitaiCd(jichitaiCd)).thenReturn(Optional.of(BigDecimal.TEN));
			when(atenaRepository.save(any(Atena.class))).thenAnswer(inv -> inv.getArgument(0));

			Atena result = atenaConfigService.register(atena, jichitaiCd);

			assertThat(result).isNotNull();
			assertThat(result.getKbn()).isEqualTo("1");
			assertThat(result.getKojinNo()).isEqualTo("hashed_kojin");
			assertThat(result.getAtenaNo()).isEqualTo(BigDecimal.valueOf(11));
			verify(atenaRepository).save(atena);
		}

		@Test
		@DisplayName("正常系：個人番号が空で法人番号が指定されており、重複がない場合に正常に登録され、区分が「2」になること")
		void success_hojinNo() {
			String jichitaiCd = "123456";
			Jichitai jichitai = new Jichitai();
			jichitai.setAtenaStNo(BigDecimal.valueOf(10));
			Atena atena = new Atena();
			atena.setHojinNo("1234567890123");

			when(jichitaiRepository.findById(jichitaiCd)).thenReturn(Optional.of(jichitai));
			when(atenaRepository.existsByHojinNo(jichitaiCd, "1234567890123", null)).thenReturn(false);
			when(atenaRepository.findMaxAtenaNoByJichitaiCd(jichitaiCd)).thenReturn(Optional.empty());
			when(atenaRepository.save(any(Atena.class))).thenAnswer(inv -> inv.getArgument(0));

			Atena result = atenaConfigService.register(atena, jichitaiCd);

			assertThat(result).isNotNull();
			assertThat(result.getKbn()).isEqualTo("2");
			assertThat(result.getAtenaNo()).isEqualTo(BigDecimal.valueOf(10));
			verify(atenaRepository).save(atena);
		}

		@Test
		@DisplayName("境界値：既存の宛名が存在する場合（最大値 + 1 が採番されること）")
		void boundary_maxNoPlusOne() {
			String jichitaiCd = "123456";
			Jichitai jichitai = new Jichitai();
			Atena atena = new Atena();
			atena.setHojinNo("1234567890123");

			when(jichitaiRepository.findById(jichitaiCd)).thenReturn(Optional.of(jichitai));
			when(atenaRepository.existsByHojinNo(any(), any(), any())).thenReturn(false);
			when(atenaRepository.findMaxAtenaNoByJichitaiCd(jichitaiCd)).thenReturn(Optional.of(BigDecimal.valueOf(5)));
			when(atenaRepository.save(any(Atena.class))).thenAnswer(inv -> inv.getArgument(0));

			Atena result = atenaConfigService.register(atena, jichitaiCd);

			assertThat(result.getAtenaNo()).isEqualTo(BigDecimal.valueOf(6));
		}

		@Test
		@DisplayName("境界値：既存の宛名が0件で、開始番号が未設定の場合（BigDecimal.ONE が使われること）")
		void boundary_defaultToOne() {
			String jichitaiCd = "123456";
			Jichitai jichitai = new Jichitai();
			jichitai.setAtenaStNo(null);
			Atena atena = new Atena();
			atena.setHojinNo("1234567890123");

			when(jichitaiRepository.findById(jichitaiCd)).thenReturn(Optional.of(jichitai));
			when(atenaRepository.existsByHojinNo(any(), any(), any())).thenReturn(false);
			when(atenaRepository.findMaxAtenaNoByJichitaiCd(jichitaiCd)).thenReturn(Optional.empty());
			when(atenaRepository.save(any(Atena.class))).thenAnswer(inv -> inv.getArgument(0));

			Atena result = atenaConfigService.register(atena, jichitaiCd);

			assertThat(result.getAtenaNo()).isEqualTo(BigDecimal.ONE);
		}

		@Test
		@DisplayName("異常系：指定した自治体コードが存在しない場合、例外がスローされること")
		void exception_jichitaiNotFound() {
			String jichitaiCd = "999999";
			Atena atena = new Atena();

			when(jichitaiRepository.findById(jichitaiCd)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> atenaConfigService.register(atena, jichitaiCd))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessage("自治体情報が見つかりません。");
		}

		@Test
		@DisplayName("異常系：登録しようとした個人番号がすでに存在する場合、ビジネス例外がスローされること")
		void exception_duplicateKojinNo() {
			String jichitaiCd = "123456";
			Jichitai jichitai = new Jichitai();
			Atena atena = new Atena();
			atena.setKojinNo("123456789012");

			when(jichitaiRepository.findById(jichitaiCd)).thenReturn(Optional.of(jichitai));
			when(hashUtil.sha256("123456789012")).thenReturn("hashed_kojin");
			when(atenaRepository.existsByKojinNo(jichitaiCd, "hashed_kojin", null)).thenReturn(true);

			assertThatThrownBy(() -> atenaConfigService.register(atena, jichitaiCd))
					.isInstanceOf(BusinessException.class)
					.hasMessage("この個人番号はすでに登録されています。");
		}

		@Test
		@DisplayName("異常系：登録しようとした法人番号がすでに存在する場合、ビジネス例外がスローされること")
		void exception_duplicateHojinNo() {
			String jichitaiCd = "123456";
			Jichitai jichitai = new Jichitai();
			Atena atena = new Atena();
			atena.setHojinNo("1234567890123");

			when(jichitaiRepository.findById(jichitaiCd)).thenReturn(Optional.of(jichitai));
			when(atenaRepository.existsByHojinNo(jichitaiCd, "1234567890123", null)).thenReturn(true);

			assertThatThrownBy(() -> atenaConfigService.register(atena, jichitaiCd))
					.isInstanceOf(BusinessException.class)
					.hasMessage("この法人番号はすでに登録されています。");
		}
	}

	@Nested
	@DisplayName("update メソッドのテスト")
	class UpdateTest {

		@Test
		@DisplayName("正常系：既存の宛名が存在し、新しい個人番号を指定して更新する場合に正常に処理されること")
		void success_newKojinNo() {
			String jichitaiCd = "123456";
			Atena atena = new Atena();
			atena.setAtenaNo(BigDecimal.ONE);
			atena.setKojinNo("new_kojin");

			Atena existing = new Atena();
			existing.setAtenaNo(BigDecimal.ONE);

			when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.of(existing));
			when(hashUtil.sha256("new_kojin")).thenReturn("hashed_new_kojin");
			when(atenaRepository.existsByKojinNo(jichitaiCd, "hashed_new_kojin", BigDecimal.ONE)).thenReturn(false);
			when(atenaRepository.save(any(Atena.class))).thenAnswer(inv -> inv.getArgument(0));

			Atena result = atenaConfigService.update(atena, jichitaiCd);

			assertThat(result).isNotNull();
			assertThat(result.getKbn()).isEqualTo("1");
			assertThat(result.getKojinNo()).isEqualTo("hashed_new_kojin");
			verify(atenaRepository).save(atena);
		}

		@Test
		@DisplayName("正常系：更新時に個人番号が空の場合、既存の個人番号と区分が引き継がれること")
		void success_emptyKojinNo() {
			String jichitaiCd = "123456";
			Atena atena = new Atena();
			atena.setAtenaNo(BigDecimal.ONE);
			atena.setKojinNo(null);

			Atena existing = new Atena();
			existing.setAtenaNo(BigDecimal.ONE);
			existing.setKojinNo("existing_hash");
			existing.setKbn("1");

			when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.of(existing));
			when(atenaRepository.save(any(Atena.class))).thenAnswer(inv -> inv.getArgument(0));

			Atena result = atenaConfigService.update(atena, jichitaiCd);

			assertThat(result).isNotNull();
			assertThat(result.getKbn()).isEqualTo("1");
			assertThat(result.getKojinNo()).isEqualTo("existing_hash");
			verify(atenaRepository).save(atena);
		}

		@Test
		@DisplayName("異常系：更新対象の宛名が存在しない場合、例外がスローされること")
		void exception_notFound() {
			String jichitaiCd = "123456";
			Atena atena = new Atena();
			atena.setAtenaNo(BigDecimal.valueOf(999));

			when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.empty());

			assertThatThrownBy(() -> atenaConfigService.update(atena, jichitaiCd))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessage("宛名が見つかりません。");
		}

		@Test
		@DisplayName("異常系：更新しようとした個人番号が、自分以外のデータですでに使用されている場合、ビジネス例外がスローされること")
		void exception_duplicateKojinNo() {
			String jichitaiCd = "123456";
			Atena atena = new Atena();
			atena.setAtenaNo(BigDecimal.ONE);
			atena.setKojinNo("other_kojin");

			Atena existing = new Atena();
			existing.setAtenaNo(BigDecimal.ONE);

			when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.of(existing));
			when(hashUtil.sha256("other_kojin")).thenReturn("hashed_other");
			when(atenaRepository.existsByKojinNo(jichitaiCd, "hashed_other", BigDecimal.ONE)).thenReturn(true);

			assertThatThrownBy(() -> atenaConfigService.update(atena, jichitaiCd))
					.isInstanceOf(BusinessException.class)
					.hasMessage("この個人番号はすでに登録されています。");
		}
	}

    @Nested
    @DisplayName("findByAtenaNo メソッドのテスト")
    class FindByAtenaNoTest {

        @Test
        @DisplayName("正常系：指定した自治体コードと宛名番号に該当する宛名情報が正しく取得できること")
        void findByAtenaNo_found() {
            BigDecimal atenaNo = BigDecimal.valueOf(1);
            Atena expectedAtena = new Atena();
            expectedAtena.setJichitaiCd(JICHITAI_CD);
            expectedAtena.setAtenaNo(atenaNo);

            when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, atenaNo))
                    .thenReturn(Optional.of(expectedAtena));

            Atena result = atenaConfigService.findByAtenaNo(JICHITAI_CD, atenaNo);

            assertThat(result).isNotNull();
            assertThat(result.getJichitaiCd()).isEqualTo(JICHITAI_CD);
            assertThat(result.getAtenaNo()).isEqualTo(atenaNo);
        }

        @Test
        @DisplayName("異常系：指定した自治体コードと宛名番号に該当する宛名情報が存在しない場合、例外がスローされること")
        void findByAtenaNo_notFound_throwsException() {
            BigDecimal atenaNo = BigDecimal.valueOf(999);

            when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, atenaNo))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> atenaConfigService.findByAtenaNo(JICHITAI_CD, atenaNo))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("宛名が見つかりません。");
        }
    }
}