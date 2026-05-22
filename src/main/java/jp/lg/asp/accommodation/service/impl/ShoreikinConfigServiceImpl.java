package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.ShoreikinConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoreikinConfigServiceImpl implements ShoreikinConfigService {

	private final ShoreikinRepository shoreikinRepository;
	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final FukaRepository fukaRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@Value("${app.kofukin.rate}")
	private BigDecimal defaultKofuritsu;

	@Override
	@Transactional(readOnly = true)
	public List<ShoreikinConfigDto> getShoreikinList(List<String> shiteiNos, String nendo) {
		List<ShoreikinConfigDto> resultList = new ArrayList<>();

		for (String shiteiNo : shiteiNos) {
			ShoreikinConfigDto dto = getShoreikin(shiteiNo, nendo);
			if (dto != null) {
				resultList.add(dto);
			}
		}

		return resultList;
	}

	@Override
	@Transactional(readOnly = true)
	public ShoreikinConfigDto getShoreikin(String shiteiNo, String nendo) {
		ShoreikinConfigDto dto = new ShoreikinConfigDto();
		dto.setShiteiNo(shiteiNo);
		dto.setNendo(nendo);

		// 特別徴収義務者情報を取得
		Optional<Tokugimu> tokugimuOpt = tokugimuRepository
				.findByJichitaiCdAndShiteiNoAndNewFlgAndDelFlg(jichitaiCd, shiteiNo, "1", "0");

		if (tokugimuOpt.isPresent()) {
			Tokugimu tokugimu = tokugimuOpt.get();
			dto.setShisetsuName(tokugimu.getShisetsuName());

			// 宛名情報を取得
			Optional<Atena> atenaOpt = atenaRepository
					.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo());
			if (atenaOpt.isPresent()) {
				dto.setShimei(atenaOpt.get().getName());
			}
		}

		// 交付金情報を取得
		if (nendo != null && !nendo.isEmpty()) {
			ShoreikinId id = new ShoreikinId(jichitaiCd, shiteiNo, nendo);
			Optional<Shoreikin> shoreikinOpt = shoreikinRepository.findById(id);

			if (shoreikinOpt.isPresent()) {
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
				dto.setKofuRitsu(defaultKofuritsu);
			}
		}

		return dto;
	}

	@Override
	@Transactional
	public ShoreikinConfigDto createShoreikin(ShoreikinConfigDto dto) {
		String currentUser = getCurrentUser();
		LocalDateTime now = LocalDateTime.now();

		Shoreikin shoreikin = new Shoreikin();
		shoreikin.setJichitaiCd(jichitaiCd);
		shoreikin.setShiteiNo(dto.getShiteiNo());
		shoreikin.setNendo(dto.getNendo());
		shoreikin.setKofuZeigaku(dto.getKofuZeigaku());
		shoreikin.setKofuRitsu(dto.getKofuRitsu());
		shoreikin.setKofuGaku(dto.getKofuGaku());
		shoreikin.setKofuYmd(dto.getKofuYmd());
		shoreikin.setAddDt(now);
		shoreikin.setAddUser(currentUser);
		shoreikin.setUpdDt(now);
		shoreikin.setUpdUser(currentUser);
		shoreikin.setVersion(1);

		shoreikinRepository.save(shoreikin);

		dto.setExists(true);
		dto.setVersion(1);
		dto.setMode("view");

		return dto;
	}

	@Override
	@Transactional
	public ShoreikinConfigDto updateShoreikin(ShoreikinConfigDto dto) {
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

		String currentUser = getCurrentUser();
		LocalDateTime now = LocalDateTime.now();

		shoreikin.setKofuZeigaku(dto.getKofuZeigaku());
		shoreikin.setKofuRitsu(dto.getKofuRitsu());
		shoreikin.setKofuGaku(dto.getKofuGaku());
		shoreikin.setKofuYmd(dto.getKofuYmd());
		shoreikin.setUpdDt(now);
		shoreikin.setUpdUser(currentUser);
		shoreikin.setVersion(shoreikin.getVersion() + 1);

		shoreikinRepository.save(shoreikin);

		dto.setVersion(shoreikin.getVersion());
		dto.setMode("view");

		return dto;
	}

	@Override
	@Transactional(readOnly = true)
	public ShoreikinConfigDto calculateShoreikin(ShoreikinConfigDto dto) {
		// 納入税額を算出
		Long kofuZeigaku = calculateKofuZeigaku(dto.getShiteiNo(), dto.getNendo());
		dto.setKofuZeigaku(kofuZeigaku);

		// デフォルト交付率を設定（3%）
		if (dto.getKofuRitsu() == null) {
			dto.setKofuRitsu(new BigDecimal("3.00"));
		}

		// 交付額を算出（納入税額 × 交付率 ÷ 100）
		if (kofuZeigaku != null && dto.getKofuRitsu() != null) {
			Long kofuGaku = new BigDecimal(kofuZeigaku).multiply(dto.getKofuRitsu())
					.divide(new BigDecimal("100")).longValue();
			dto.setKofuGaku(kofuGaku);
		}

		return dto;
	}

	@Override
	@Transactional(readOnly = true)
	public Long calculateKofuZeigaku(String shiteiNo, String nendo) {
		// 指定年度の賦課情報を取得（del_flg='0', new_flg='1'）
		List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(
				jichitaiCd, shiteiNo, nendo, "0", "1");

		return fukaList.stream()
				.map(Fuka::getTotalZeigaku)
				.filter(zeigaku -> zeigaku != null)
				.reduce(0L, Long::sum);
	}

	/**
	 * 現在のログインユーザーを取得
	 */
	private String getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getName() != null) {
			return authentication.getName();
		}
		return "SYSTEM";
	}
}