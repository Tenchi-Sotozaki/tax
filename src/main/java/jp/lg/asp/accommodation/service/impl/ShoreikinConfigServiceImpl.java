package jp.lg.asp.accommodation.service.impl;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.ShoreikinConfigDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.ShoreikinId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.KofuRitsuRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
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

				LocalDate today = LocalDate.now();
				dto.setNendo(String.valueOf(today.getYear()));

				// 交付率を取得して設定 ( 処理日を指定 )
				List<BigDecimal> ritsuList1 = kofuRitsuRepository.findKofuRitsuByJichitaiCd(jichitaiCd, today.getYear());
				dto.setKofuRitsu(ritsuList1.isEmpty() ? null : ritsuList1.get(0));
			}
		} else {
			// 新規登録モード
			dto.setExists(false);
			dto.setMode("create");

			LocalDate today = LocalDate.now();
			dto.setNendo(String.valueOf(today.getYear()));

			// 交付率を取得して設定 ( 処理日を指定 )
			List<BigDecimal> ritsuList2 = kofuRitsuRepository.findKofuRitsuByJichitaiCd(jichitaiCd, today.getYear());
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
		// 納入税額を算出
		Long kofuZeigaku = calculateKofuZeigaku(dto.getShiteiNo(), dto.getNendo());
		dto.setKofuZeigaku(kofuZeigaku);

		// デフォルト交付率を設定
		if (dto.getKofuRitsu() == null) {
			List<BigDecimal> ritsuList = kofuRitsuRepository.findKofuRitsuByJichitaiCd(jichitaiCd, LocalDate.now().getYear());
			dto.setKofuRitsu(ritsuList.isEmpty() ? null : ritsuList.get(0));
		}

		// 交付額を算出（納入税額 × 交付率 ÷ 100）
		if (kofuZeigaku != null && dto.getKofuRitsu() != null) {
			Long kofuGaku = new BigDecimal(kofuZeigaku)
					.multiply(dto.getKofuRitsu())
					.divide(new BigDecimal("100"), RoundingMode.DOWN)
					.longValue();
			dto.setKofuGaku(kofuGaku);
		}

		return dto;
	}

	@Override
	@Transactional(readOnly = true)
	public Long calculateKofuZeigaku(String shiteiNo, String nendo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		// 指定年度の賦課情報を取得（del_flg='0', new_flg='1'）
		List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(
				jichitaiCd, shiteiNo, nendo, "0", "1");

		return fukaList.stream()
				.collect(Collectors.toMap(Fuka::getKibetsu, f -> f, (a, b) -> a.getRno() > b.getRno() ? a : b)).values()
				.stream()
				.map(Fuka::getTotalZeigaku)
				.filter(zeigaku -> zeigaku != null)
				.reduce(0L, Long::sum);
	}
}
