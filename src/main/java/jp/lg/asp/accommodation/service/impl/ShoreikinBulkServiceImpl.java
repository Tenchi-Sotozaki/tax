package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.ShoreikinBulkDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.ShoreikinId;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.service.ShoreikinBulkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoreikinBulkServiceImpl implements ShoreikinBulkService {

	private final ShoreikinRepository shoreikinRepository;
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

		// 賦課情報がある特別徴収義務者を取得
		List<Fuka> fukaList = fukaRepository
				.findByJichitaiCdAndNendoAndNewFlgAndDelFlg(jichitaiCd, dto.getNendo(), "1", "0");
		List<String> shiteiNos = fukaList.stream().map(Fuka::getShiteiNo).distinct().toList();

		dto.setTargetCount(shiteiNos.size());
		int successCount = 0;
		int failureCount = 0;
		int skipCount = 0;

		for (String shiteiNo : shiteiNos) {
			try {
				// 既存の交付金レコードをチェック
				ShoreikinId id = new ShoreikinId(jichitaiCd, shiteiNo, dto.getNendo());
				Shoreikin shoreikin = shoreikinRepository.findById(id).orElse(new Shoreikin());

				if (dto.isIncludeCalculated() || shoreikin.getShiteiNo() == null) {
					// 交付金税額を算出（指定年度の賦課情報から集計）
					Long kofuZeigaku = calculateKofuZeigaku(shiteiNo, dto.getNendo());
					kofuZeigaku = kofuZeigaku < 0L ? 0L : kofuZeigaku;

					// 交付額を算出（交付税額 × 交付率）
					Long kofuGaku = new BigDecimal(kofuZeigaku).multiply(dto.getKofuRitsu())
							.divide(new BigDecimal("100")).longValue();

					shoreikin.setJichitaiCd(jichitaiCd);
					shoreikin.setShiteiNo(shiteiNo);
					shoreikin.setNendo(dto.getNendo());
					shoreikin.setKofuZeigaku(kofuZeigaku);
					shoreikin.setKofuRitsu(dto.getKofuRitsu());
					shoreikin.setKofuGaku(kofuGaku);

					shoreikinRepository.save(shoreikin);
					successCount++;
				} else {
					skipCount++;
				}
			} catch (Exception e) {
				log.error("交付金算出エラー - 指定番号: {}", shiteiNo, e);
				failureCount++;
			}
		}

		dto.setSuccessCount(successCount);
		dto.setFailureCount(failureCount);
		dto.setSkipCount(skipCount);
		dto.setExecuted(true);

		dto.setResultMessage(String.format("一括算出が完了しました。スキップ: %d件、成功: %d件、失敗: %d件",
				skipCount, successCount, failureCount));

		return dto;
	}

	/**
	 * 指定年度の賦課情報から交付金税額を算出
	 */
	private Long calculateKofuZeigaku(String shiteiNo, String nendo) {
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