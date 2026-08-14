package jp.lg.asp.accommodation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.AtenaId;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.exception.ResourceNotFoundException;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.impl.AtenaConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtenaConfigServiceImplTest {

	@Mock
	private AtenaRepository atenaRepository;

	@Mock
	private JichitaiRepository jichitaiRepository;

	@InjectMocks
	private AtenaConfigServiceImpl service;

	private static final String JICHITAI_CD = "011002";

	//===========================================
	// findByAtenaNo（宛名検索）
	//===========================================
	@Test
	void findByAtenaNo_正常系_宛名が取得できること() {
		BigDecimal atenaNo = BigDecimal.TEN;
		Atena expectedAtena = new Atena();
		expectedAtena.setAtenaNo(atenaNo);

		when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, atenaNo))
				.thenReturn(Optional.of(expectedAtena));

		Atena result = service.findByAtenaNo(JICHITAI_CD, atenaNo);

		assertThat(result).isNotNull();
		assertThat(result.getAtenaNo()).isEqualTo(atenaNo);
	}

	@Test
	void findByAtenaNo_異常系_宛名が見つからない場合例外が発生すること() {
		BigDecimal atenaNo = BigDecimal.TEN;
		when(atenaRepository.findByJichitaiCdAndAtenaNo(JICHITAI_CD, atenaNo))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findByAtenaNo(JICHITAI_CD, atenaNo))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("宛名が見つかりません。");
	}

	//===========================================
	// register（登録）
	//===========================================
	@Test
	void register_正常系_宛名が新規登録され番号が採番されること() {
		Atena inputAtena = new Atena();
		inputAtena.setKojinNo("123456789012"); // 個人番号あり (kbn = "1" になる)

		Jichitai jichitai = new Jichitai();
		jichitai.setAtenaStNo(BigDecimal.valueOf(100));

		when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
		when(atenaRepository.save(any(Atena.class))).thenAnswer(inv -> inv.getArgument(0));

		Atena result = service.register(inputAtena, JICHITAI_CD);

		assertThat(result).isNotNull();
		// 採番されて101になっていること
		assertThat(result.getAtenaNo()).isEqualTo(BigDecimal.valueOf(101));
		assertThat(jichitai.getAtenaStNo()).isEqualTo(BigDecimal.valueOf(101));
		// 個人番号があるので区分が "1" になること
		assertThat(result.getKbn()).isEqualTo("1");
		verify(jichitaiRepository).save(jichitai);
		verify(atenaRepository).save(inputAtena);
	}

	@Test
	void register_境界値_自治体の開始番号がnullの場合は1から採番されること() {
		Atena inputAtena = new Atena();
		inputAtena.setHojinNo("1234567890123"); // 法人番号あり (kbn = "2" になる)

		Jichitai jichitai = new Jichitai();
		jichitai.setAtenaStNo(null); // 開始番号がnull

		when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.of(jichitai));
		when(atenaRepository.save(any(Atena.class))).thenAnswer(inv -> inv.getArgument(0));

		Atena result = service.register(inputAtena, JICHITAI_CD);

		assertThat(result).isNotNull();
		// nullの場合は1になること
		assertThat(result.getAtenaNo()).isEqualTo(BigDecimal.ONE);
		assertThat(result.getKbn()).isEqualTo("2");
	}

	@Test
	void register_異常系_自治体情報が見つからない場合例外が発生すること() {
		Atena inputAtena = new Atena();
		when(jichitaiRepository.findById(JICHITAI_CD)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.register(inputAtena, JICHITAI_CD))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("自治体情報が見つかりません。");

		verify(atenaRepository, never()).save(any());
	}

	//===========================================
	// update（更新）
	//===========================================
	@Test
	void update_正常系_既存の宛名が更新されること() {
		Atena inputAtena = new Atena();
		inputAtena.setAtenaNo(BigDecimal.TEN);
		inputAtena.setKojinNo(null); // 個人番号なし -> 法人またはその他 (kbn = "2")

		when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.of(new Atena()));
		when(atenaRepository.save(any(Atena.class))).thenAnswer(inv -> inv.getArgument(0));

		Atena result = service.update(inputAtena, JICHITAI_CD);

		assertThat(result).isNotNull();
		assertThat(result.getKbn()).isEqualTo("2");
		verify(atenaRepository).save(inputAtena);
	}

	@Test
	void update_異常系_更新対象の宛名が見つからない場合例外が発生すること() {
		Atena inputAtena = new Atena();
		inputAtena.setAtenaNo(BigDecimal.TEN);

		when(atenaRepository.findById(any(AtenaId.class))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(inputAtena, JICHITAI_CD))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("宛名が見つかりません。");

		verify(atenaRepository, never()).save(any());
	}
}