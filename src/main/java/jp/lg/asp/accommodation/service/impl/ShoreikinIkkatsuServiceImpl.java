package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.ShoreikinBulkDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.ShoreikinId;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.ShoreikinBulkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoreikinIkkatsuServiceImpl implements ShoreikinBulkService {

	private final ShoreikinRepository shoreikinRepository;
	private final TokugimuRepository tokugimuRepository;
	private final FukaRepository fukaRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@Override
	@Transactional
	public ShoreikinBulkDto executeBulkSanshutsu(ShoreikinBulkDto dto) {
		if (dto.getNendo() == null || dto.getNendo().isEmpty()) {
			dto.setResultMessage("交付金年度が指定されていません");
			return dto;
		}

		// 対象となる特別徴収義務者を取得（del_flg='0', new_flg='1'）
		List<Tokugimu> tokugimuList = tokugimuRepository
				.findByJichitaiCdAndDelFlgAndNewFlg(jichitaiCd, "0", "1");

		dto.setTargetCount(tokugimuList.size());
		int successCount = 0;
		int failureCount = 0;

		String currentUser = getCurrentUser();
		LocalDateTime now = LocalDateTime.now();

		for (Tokugimu tokugimu : tokugimuList) {
			try {
				// 既存の交付金レコードをチェック
				ShoreikinId id = new ShoreikinId(jichitaiCd, tokugimu.getShiteiNo(), dto.getNendo());

				if (!shoreikinRepository.existsById(id)) {
					// 交付金税額を算出（指定年度の賦課情報から集計）
					Long kofuZeigaku = calculateKofuZeigaku(tokugimu.getShiteiNo(), dto.getNendo());

					if (kofuZeigaku > 0) {
						// 交付額を算出（交付税額 × 交付率）
						Long kofuGaku = new BigDecimal(kofuZeigaku).multiply(dto.getKofuRitsu())
								.divide(new BigDecimal("100")).longValue();

						Shoreikin shoreikin = new Shoreikin();
						shoreikin.setJichitaiCd(jichitaiCd);
						shoreikin.setShiteiNo(tokugimu.getShiteiNo());
						shoreikin.setNendo(dto.getNendo());
						shoreikin.setKofuZeigaku(kofuZeigaku);
						shoreikin.setKofuRitsu(dto.getKofuRitsu());
						shoreikin.setKofuGaku(kofuGaku);
						shoreikin.setAddDt(now);
						shoreikin.setAddUser(currentUser);
						shoreikin.setUpdDt(now);
						shoreikin.setUpdUser(currentUser);
						shoreikin.setVersion(1);

						shoreikinRepository.save(shoreikin);
						successCount++;
					}
				}
			} catch (Exception e) {
				log.error("交付金算出エラー - 指定番号: {}", tokugimu.getShiteiNo(), e);
				failureCount++;
			}
		}

		dto.setSuccessCount(successCount);
		dto.setFailureCount(failureCount);
		dto.setExecuted(true);

		if (failureCount == 0) {
			dto.setResultMessage(String.format("一括算出が完了しました。処理件数: %d件", successCount));
		} else {
			dto.setResultMessage(String.format("一括算出が完了しました。成功: %d件、失敗: %d件",
					successCount, failureCount));
		}

		return dto;
	}

	@Override
	@Transactional(readOnly = true)
	public int getTargetCount(String nendo) {
		if (nendo == null || nendo.isEmpty()) {
			return 0;
		}

		List<Tokugimu> tokugimuList = tokugimuRepository
				.findByJichitaiCdAndDelFlgAndNewFlg(jichitaiCd, "0", "1");

		return tokugimuList.size();
	}

	/**
	 * 指定年度の賦課情報から交付金税額を算出
	 */
	private Long calculateKofuZeigaku(String shiteiNo, String nendo) {
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