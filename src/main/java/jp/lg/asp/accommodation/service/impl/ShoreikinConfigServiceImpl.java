package jp.lg.asp.accommodation.service.impl;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.KofuRitsu;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.ShoreikinId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.repository.KofuRitsuRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.ShunoRirekiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.ShoreikinConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 特別徴収事務交付金照会／登録／編集 Service 実装クラス
 * 仕様書：特別徴収事務交付金照会・登録・編集.csv に基づく実装
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShoreikinConfigServiceImpl implements ShoreikinConfigService {

	private final ShoreikinRepository shoreikinRepository;
	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final FukaRepository fukaRepository;
	private final KofuRitsuRepository kofuRitsuRepository;
	private final ShunoRirekiRepository shunoRirekiRepository;
	private final JichitaiRepository jichitaiRepository;

	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public ShoreikinConfigDto getShoreikin(String shiteiNo, String nendo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		ShoreikinConfigDto dto = new ShoreikinConfigDto();
		dto.setShiteiNo(shiteiNo);
		dto.setNendo(nendo);

		// 特別徴収義務者情報を取得（del_flg='0', new_flg='1'）
		Optional<Tokugimu> tokugimuOpt = tokugimuRepository
				.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(jichitaiCd, shiteiNo, "1", "0");

		if (tokugimuOpt.isPresent()) {
			Tokugimu tokugimu = tokugimuOpt.get();
			dto.setShisetsuName(tokugimu.getShisetsuName());

			// 宛名情報を取得
			Optional<Atena> atenaOpt = atenaRepository
					.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo());
			if (atenaOpt.isPresent()) {
				dto.setName(atenaOpt.get().getName());
			}
		}

		// 交付金情報を取得
		if (nendo != null && !nendo.isEmpty()) {
			ShoreikinId id = new ShoreikinId(jichitaiCd, shiteiNo, nendo);
			Optional<Shoreikin> shoreikinOpt = shoreikinRepository.findById(id);

			if (shoreikinOpt.isPresent()) {
				// 既存データがある場合（照会モード）
				Shoreikin shoreikin = shoreikinOpt.get();
				dto.setKofuZeigaku(shoreikin.getKofuZeigaku());
				dto.setKofuRitsu(shoreikin.getKofuRitsu());
				dto.setKofuGaku(shoreikin.getKofuGaku());
				dto.setKofuYmd(shoreikin.getKofuYmd());
				dto.setVersion(shoreikin.getVersion());
				dto.setExists(true);
				dto.setMode("view");
			} else {
				// 新規登録モード
				dto.setExists(false);
				dto.setMode("create");

				// 交付率を取得して設定 ( 画面の交付金年度を指定 )
				Integer nendoInt = Integer.parseInt(nendo);
				List<BigDecimal> ritsuList1 = kofuRitsuRepository.findKofuRitsuByJichitaiCd(jichitaiCd, nendoInt);
				dto.setKofuRitsu(ritsuList1.isEmpty() ? null : ritsuList1.get(0));
			}
		} else {
			// 新規登録モード（nendo未指定 → 年度開始月から今年度を算出）
			dto.setExists(false);
			dto.setMode("create");
			int currentNendo = resolveCurrentNendo(jichitaiCd);
			dto.setNendo(String.valueOf(currentNendo));
			List<BigDecimal> ritsuList2 = kofuRitsuRepository.findKofuRitsuByJichitaiCd(jichitaiCd, currentNendo);
			dto.setKofuRitsu(ritsuList2.isEmpty() ? null : ritsuList2.get(0));
		}

		return dto;
	}

	@Override
	@Transactional
	public ShoreikinConfigDto createShoreikin(ShoreikinConfigDto dto) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		// 交付金情報を新規登録
		Shoreikin shoreikin = new Shoreikin();
		shoreikin.setJichitaiCd(jichitaiCd);
		shoreikin.setShiteiNo(dto.getShiteiNo());
		shoreikin.setNendo(dto.getNendo());
		shoreikin.setKofuZeigaku(dto.getKofuZeigaku());
		shoreikin.setKofuRitsu(dto.getKofuRitsu());
		shoreikin.setKofuGaku(dto.getKofuGaku());
		shoreikin.setKofuYmd(dto.getKofuYmd());

		shoreikinRepository.save(shoreikin);

		// DTOを更新
		dto.setExists(true);
		dto.setVersion(1);
		dto.setMode("view");

		return dto;
	}

	@Override
	@Transactional
	public ShoreikinConfigDto updateShoreikin(ShoreikinConfigDto dto) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		ShoreikinId id = new ShoreikinId(jichitaiCd, dto.getShiteiNo(), dto.getNendo());
		Optional<Shoreikin> shoreikinOpt = shoreikinRepository.findById(id);

		if (shoreikinOpt.isEmpty()) {
			throw new RuntimeException("更新対象の交付金情報が見つかりません");
		}

		Shoreikin shoreikin = shoreikinOpt.get();

		// 楽観的排他制御
		if (!shoreikin.getVersion().equals(dto.getVersion())) {
			throw new RuntimeException("他のユーザーによって更新されています。画面を再表示してください。");
		}

		// 交付金情報を更新
		shoreikin.setKofuZeigaku(dto.getKofuZeigaku());
		shoreikin.setKofuRitsu(dto.getKofuRitsu());
		shoreikin.setKofuGaku(dto.getKofuGaku());
		shoreikin.setKofuYmd(dto.getKofuYmd());

		shoreikinRepository.save(shoreikin);

		// DTOを更新
		dto.setVersion(shoreikin.getVersion());
		dto.setMode("view");

		return dto;
	}

	@Override
	@Transactional(readOnly = true)
	public ShoreikinConfigDto calculateShoreikin(ShoreikinConfigDto dto) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();

		// 交付率設定を取得（画面の交付金年度を指定）
		Integer nendoInt = Integer.parseInt(dto.getNendo());
		List<KofuRitsu> kofuRitsuList = kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(
				jichitaiCd, nendoInt);
		// 交付率が取得できない場合は算出できないためエラーとする
		if (kofuRitsuList.isEmpty()) {
			throw new IllegalStateException("交付率が設定されていません。交付率設定画面で登録してください");
		}
		KofuRitsu kofuRitsuEntity = kofuRitsuList.get(0);

		// 交付率は画面で入力せず交付率設定から取得するため、常にマスタの値で上書きする
		dto.setKofuRitsu(kofuRitsuEntity.getKofuRitsu());

		// 納入税額を算出
		Long kofuZeigaku = calculateKofuZeigaku(dto.getShiteiNo(), dto.getNendo());
		dto.setKofuZeigaku(kofuZeigaku);

		// 交付額を算出（交付率は上で必ず設定されている）
		if (kofuZeigaku != null) {
			dto.setKofuGaku(calculateKofuGaku(kofuZeigaku, dto.getKofuRitsu(), kofuRitsuEntity));
		}

		return dto;
	}

	@Override
	@Transactional(readOnly = true)
	public Long calculateKofuZeigaku(String shiteiNo, String nendo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(
				jichitaiCd, shiteiNo, nendo, "0", "1");

		// 納入額を取得（期別ごとの合計）
		Map<Integer, Long> shunoByKibetsu = shunoRirekiRepository
				.sumNonyugakuByShiteiNoIn(jichitaiCd, List.of(shiteiNo))
				.stream()
				.filter(row -> nendo.equals(row[1]))
				.collect(Collectors.toMap(
						row -> ((Number) row[2]).intValue(),
						row -> ((Number) row[3]).longValue()));

		return fukaList.stream()
				.collect(Collectors.toMap(Fuka::getKibetsu, f -> f, (a, b) -> a.getRno() > b.getRno() ? a : b)).values()
				.stream()
				.filter(f -> f.getShinkokuYmd() != null) // 申告済み
				.filter(f -> {
					if (f.getTotalZeigaku() == null || f.getTotalZeigaku() == 0L) return false;
					long nonyugaku = shunoByKibetsu.getOrDefault(f.getKibetsu(), 0L);
					return nonyugaku >= f.getTotalZeigaku(); // 納付済み
				})
				.map(Fuka::getTotalZeigaku)
				.reduce(0L, Long::sum);
	}

	/**
	 * 年度開始月をもとに現在の年度を算出する
	 */
	private int resolveCurrentNendo(String jichitaiCd) {
		LocalDate today = LocalDate.now();
		int stMonth = jichitaiRepository.findById(jichitaiCd)
				.map(Jichitai::getNendoStMonth)
				.map(Integer::parseInt)
				.orElse(4);
		return today.getMonthValue() >= stMonth ? today.getYear() : today.getYear() - 1;
	}

	/**
	 * 交付額を算出（算出単位・切り捨て/切り上げ・最低額を考慮）
	 */
	private Long calculateKofuGaku(Long kofuZeigaku, BigDecimal kofuRitsu, KofuRitsu kofuRitsuEntity) {
		BigDecimal raw = new BigDecimal(kofuZeigaku).multiply(kofuRitsu).divide(new BigDecimal("100"), 10, RoundingMode.DOWN);

		int sanshutsu = (kofuRitsuEntity != null && kofuRitsuEntity.getSanshutsu() != null)
				? kofuRitsuEntity.getSanshutsu() : 1;
		String kbn = (kofuRitsuEntity != null) ? kofuRitsuEntity.getKbn() : "1";
		BigDecimal saiteigaku = (kofuRitsuEntity != null) ? kofuRitsuEntity.getSaiteigaku() : null;

		RoundingMode roundingMode = "2".equals(kbn) ? RoundingMode.CEILING : RoundingMode.FLOOR;
		BigDecimal unit = new BigDecimal(sanshutsu);
		long kofuGaku = raw.divide(unit, 0, roundingMode).multiply(unit).longValue();

		// 最低額適用（0円の場合を除く）
		if (kofuGaku > 0 && saiteigaku != null && saiteigaku.compareTo(BigDecimal.ZERO) > 0) {
			kofuGaku = Math.max(kofuGaku, saiteigaku.longValue());
		}
		return kofuGaku;
	}
}
