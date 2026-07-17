package jp.lg.asp.accommodation.service;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.GassanDaichoItem;
import jp.lg.asp.accommodation.dto.GassanDaichoSearchForm;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GassanDaichoServiceImpl implements GassanDaichoService {

	private final GassanRepository gassanRepository;
	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final GassanUchiRepository gassanUchiRepository;

	private final JichitaiContext jichitaiContext;

	@Override
	public Page<GassanDaichoItem> search(GassanDaichoSearchForm searchForm) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		log.info("合算申告情報管理台帳検索開始: {}", searchForm);

		List<Gassan> gassanList = gassanRepository.findAllByJichitaiCd(jichitaiCd);

		// 検索条件でフィルタリング
		if (searchForm.getGassanShiteiNo() != null && !searchForm.getGassanShiteiNo().isEmpty()) {
			gassanList = gassanList.stream()
					.filter(g -> g.getGassanShiteiNo().contains(searchForm.getGassanShiteiNo()))
					.collect(Collectors.toList());
		}

		if (searchForm.getShiteiNo() != null && !searchForm.getShiteiNo().isEmpty()) {
			List<String> matchedGassanShiteiNos = gassanUchiRepository
					.findByJichitaiCdAndShiteiNo(jichitaiCd, searchForm.getShiteiNo())
					.stream().map(GassanUchi::getGassanShiteiNo).collect(Collectors.toList());
			gassanList = gassanList.stream()
					.filter(g -> matchedGassanShiteiNos.contains(g.getGassanShiteiNo()))
					.collect(Collectors.toList());
		}

		// 合算指定番号でグループ化して変換
		List<GassanDaichoItem> allItems = gassanList.stream()
				.collect(Collectors.groupingBy(Gassan::getGassanShiteiNo))
				.entrySet().stream()
				.map(entry -> {
					String gassanShiteiNo = entry.getKey();
					List<Gassan> group = entry.getValue();

					// 代表施設（rno=1）を取得
					Gassan daihyo = group.stream()
							.filter(g -> g.getRno().compareTo(BigDecimal.ONE) == 0)
							.findFirst()
							.orElse(group.get(0));

					return convertToGassanDaichoItem(gassanShiteiNo, daihyo, group);
				})
				.filter(item -> {
					// 氏名/名称でのフィルタリング
					if (searchForm.getName() != null && !searchForm.getName().isEmpty()) {
						String pattern = toLikePattern(searchForm.getName(), searchForm.getNameMatchType());
						return item.getName() != null && item.getName().matches(patternToRegex(pattern));
					}
					return true;
				})
				.sorted((item1, item2) -> item1.getGassanShiteiNo().compareTo(item2.getGassanShiteiNo()))
				.collect(Collectors.toList());

		// 全件取得後に指定ページ分に分割
		int pageSize = searchForm.getPageSize() > 0 ? searchForm.getPageSize() : 10;
		int page = searchForm.getPage();
		int total = allItems.size();
		int fromIndex = Math.min(page * pageSize, total);
		int toIndex = Math.min(fromIndex + pageSize, total);

		return new PageImpl<>(allItems.subList(fromIndex, toIndex), PageRequest.of(page, pageSize), total);
	}

	@Override
	public GassanDaichoItem getByGassanShiteiNo(String gassanShiteiNo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		log.info("合算申告情報詳細取得: {}", gassanShiteiNo);

		List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);

		if (gassanList.isEmpty()) {
			return null;
		}

		// 代表施設（rno=1）を取得
		Gassan daihyo = gassanList.stream()
				.filter(g -> g.getRno().compareTo(BigDecimal.ONE) == 0)
				.findFirst()
				.orElse(gassanList.get(0));

		return convertToGassanDaichoItem(gassanShiteiNo, daihyo, gassanList);
	}

	private String toLikePattern(String value, String matchType) {
		if (value == null || value.isBlank()) return null;
		return switch (matchType) {
			case "prefix" -> value + "%";
			case "exact"  -> value;
			default       -> "%" + value + "%";
		};
	}

	private String patternToRegex(String likePattern) {
		if (likePattern == null) return ".*";
		String[] parts = likePattern.split("%", -1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (i > 0) sb.append(".*");
			if (!parts[i].isEmpty()) sb.append(java.util.regex.Pattern.quote(parts[i]));
		}
		return sb.toString();
	}

	private GassanDaichoItem convertToGassanDaichoItem(String gassanShiteiNo, Gassan daihyo, List<Gassan> gassanList) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		GassanDaichoItem item = new GassanDaichoItem();
		item.setGassanShiteiNo(gassanShiteiNo);

		// 合算内訳テーブルから施設情報を取得
		List<GassanUchi> gassanUchiList = gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd,
				gassanShiteiNo);
		
		if (!gassanUchiList.isEmpty()) {
			// rno=1の代表施設を優先、なければ先頭レコード
			GassanUchi daihyoUchi = gassanUchiList.stream()
					.filter(u -> u.getRno().compareTo(BigDecimal.ONE) == 0)
					.findFirst()
					.orElse(gassanUchiList.get(0));
			String daihyoShiteiNo = daihyoUchi.getShiteiNo();

			// 代表施設情報を取得
			List<Tokugimu> daihyoTokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd,
					daihyoShiteiNo);
			
			if (!daihyoTokugimuList.isEmpty()) {
				Tokugimu daihyoTokugimu = daihyoTokugimuList.get(0);
				item.setDaihyoShisetsuName(daihyoTokugimu.getShisetsuName());
				item.setShiteiNo(daihyoTokugimu.getShiteiNo());

				// 宛名情報を取得
				atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, daihyoTokugimu.getAtenaNo())
						.ifPresent(atena -> {
							item.setName(atena.getName());
							item.setAtenaNo(atena.getAtenaNo());
						});
			}

			// 合算対象施設リストを作成
			List<GassanDaichoItem.GassanFacilityItem> facilityList = new ArrayList<>();
			for (GassanUchi gassanUchi : gassanUchiList) {
				List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd,
						gassanUchi.getShiteiNo());
				if (!tokugimuList.isEmpty()) {
					Tokugimu tokugimu = tokugimuList.get(0);
					GassanDaichoItem.GassanFacilityItem facilityItem = new GassanDaichoItem.GassanFacilityItem();
					facilityItem.setShiteiNo(tokugimu.getShiteiNo());
					facilityItem.setShisetsuName(tokugimu.getShisetsuName());
					facilityItem.setAtenaNo(tokugimu.getAtenaNo());

					// 宛名情報を取得
					atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo())
							.ifPresent(atena -> facilityItem.setName(atena.getName()));

					facilityList.add(facilityItem);
				}
			}
			item.setFacilityList(facilityList);

		} else {
			// GassanUchiにデータがない場合はGassanエンティティから直接取得
			if (daihyo.getShiteiNo() != null) {
				List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd,
						daihyo.getShiteiNo());
				if (!tokugimuList.isEmpty()) {
					Tokugimu tokugimu = tokugimuList.get(0);
					item.setDaihyoShisetsuName(tokugimu.getShisetsuName());
					item.setShiteiNo(tokugimu.getShiteiNo());
				}
			}
			if (daihyo.getAtenaNo() != null) {
				atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, daihyo.getAtenaNo())
						.ifPresent(atena -> {
							item.setName(atena.getName());
							item.setAtenaNo(atena.getAtenaNo());
						});
			}
		}

		return item;
	}
}