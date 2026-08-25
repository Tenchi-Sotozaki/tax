package jp.lg.asp.accommodation.service.impl;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.ShoreikinBulkDto;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.KofuRitsu;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.ShoreikinId;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.KofuRitsuRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.ShunoRirekiRepository;
import jp.lg.asp.accommodation.service.ShoreikinBulkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoreikinBulkServiceImpl implements ShoreikinBulkService {

	private final ShoreikinRepository shoreikinRepository;
	private final FukaRepository fukaRepository;
	private final ShunoRirekiRepository shunoRirekiRepository;
	private final KofuRitsuRepository kofuRitsuRepository;

	private final JichitaiContext jichitaiContext;

	@Override
	public List<BigDecimal> findKofuRitsuList(String jichitaiCd, int year) {
		return kofuRitsuRepository.findKofuRitsuByJichitaiCd(jichitaiCd, year);
	}

	@Override
	@Transactional
	public ShoreikinBulkDto executeBulkSanshutsu(ShoreikinBulkDto dto) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		if (dto.getNendo() == null || dto.getNendo().isEmpty()) {
			dto.setResultMessage("交付金年度が指定されていません");
			return dto;
		}
		

		// 交付率設定を取得
		List<KofuRitsu> kofuRitsuList = kofuRitsuRepository.findKofuRitsuEntityByJichitaiCd(
				jichitaiCd, Integer.parseInt(dto.getNendo()));
		KofuRitsu kofuRitsuEntity = kofuRitsuList.isEmpty() ? null : kofuRitsuList.get(0);

		// 賦課情報がある特別徴収義務者を取得
		List<Fuka> fukaList = fukaRepository
				.findByJichitaiCdAndNendoAndNewFlgAndDelFlg(jichitaiCd, dto.getNendo(), "1", "0");
		List<String> shiteiNos = fukaList.stream().map(Fuka::getShiteiNo).distinct().toList();

		// 収納履歴を一括取得（指定番号・年度・期別ごとの納入額合計）
		Map<String, Long> shunoMap = shunoRirekiRepository.sumNonyugakuByShiteiNoIn(jichitaiCd, shiteiNos)
				.stream()
				.collect(Collectors.toMap(
						row -> row[0] + "-" + row[1] + "-" + row[2],
						row -> ((Number) row[3]).longValue(),
						Long::sum));

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
					long kofuZeigaku = calculateKofuZeigaku(shiteiNo, dto.getNendo(), shunoMap);
					kofuZeigaku = kofuZeigaku < 0L ? 0L : kofuZeigaku;

					Long kofuGaku = calculateKofuGaku(kofuZeigaku, dto.getKofuRitsu(), kofuRitsuEntity);

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
	 * 申告済みかつ納付済みの賦課情報から交付金税額を算出
	 */
	private Long calculateKofuZeigaku(String shiteiNo, String nendo, Map<String, Long> shunoMap) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndDelFlgAndNewFlg(
				jichitaiCd, shiteiNo, nendo, "0", "1");

		return fukaList.stream()
				.collect(Collectors.toMap(Fuka::getKibetsu, f -> f, (a, b) -> a.getRno() > b.getRno() ? a : b)).values()
				.stream()
				.filter(f -> f.getShinkokuYmd() != null) // 申告済み
				.filter(f -> {
					if (f.getTotalZeigaku() == null || f.getTotalZeigaku() == 0L) return false;
					String key = shiteiNo + "-" + nendo + "-" + f.getKibetsu();
					long nonyugaku = shunoMap.getOrDefault(key, 0L);
					return nonyugaku >= f.getTotalZeigaku(); // 納付済み
				})
				.map(Fuka::getTotalZeigaku)
				.reduce(0L, Long::sum);
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