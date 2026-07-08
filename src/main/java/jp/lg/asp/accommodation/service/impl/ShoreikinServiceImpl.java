package jp.lg.asp.accommodation.service.impl;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.ShoreikinDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.ShoreikinService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShoreikinServiceImpl implements ShoreikinService {

	private final ShoreikinRepository shoreikinRepository;
	private final TokugimuRepository tokugimuRepository;
	private final AtenaRepository atenaRepository;
	private final GassanRepository gassanRepository;
	private final GassanUchiRepository gassanUchiRepository;

	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public Page<ShoreikinDto> search(ShoreikinDto form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();

		List<Tokugimu> tokugimuList;
		if (form.getShiteiNo() != null && form.getShiteiNo().startsWith("9")) {
			// 指定番号が9で始まる場合、t_gassanから検索
			tokugimuList = findTokugimuByGassanShiteiNo(form.getShiteiNo());
		} else {
			tokugimuList = tokugimuRepository.findBySearchConditions(
					jichitaiCd,
					form.getShiteiNo(),
					form.getName(),
					toLikePattern(form.getName(), form.getNameMatchType()),
					form.getShisetsuName(),
					toLikePattern(form.getShisetsuName(), form.getShisetsuNameMatchType()),
					form.getKyokaShu(),
					form.getKojinNo(),
					form.getHojinNo());
		}

		if (tokugimuList.isEmpty()) {
			return Page.empty(PageRequest.of(form.getPage(), form.getPageSize()));
		}

		List<BigDecimal> atenaNos = tokugimuList.stream().map(Tokugimu::getAtenaNo).distinct().toList();
		List<String> shiteiNos = tokugimuList.stream().map(Tokugimu::getShiteiNo).toList();

		// m_atena を atena_no をキーに取得
		Map<BigDecimal, Atena> atenaMap = atenaRepository
				.findByJichitaiCdAndAtenaNoIn(jichitaiCd, atenaNos)
				.stream()
				.collect(Collectors.toMap(Atena::getAtenaNo, a -> a));

		// 合算対象判定用
		Map<String, Boolean> gassanMap = gassanUchiRepository
				.findByJichitaiCdAndShiteiNoIn(jichitaiCd, shiteiNos)
				.stream()
				.collect(Collectors.toMap(GassanUchi::getShiteiNo, g -> true, (a, b) -> a));

		// t_shoreikin を shitei_no、nendo をキーに取得
		Map<String, List<Shoreikin>> shoreikinMap = shoreikinRepository
				.findByJichitaiCdAndShiteiNoInAndNendo(jichitaiCd, shiteiNos, form.getNendo())
				.stream()
				.collect(Collectors.toMap(Shoreikin::getShiteiNo,
						t -> new ArrayList<>(List.of(t)),
						(a, b) -> {
							a.addAll(b);
							return a;
						}));

		List<ShoreikinDto> result = tokugimuList.stream()
				.<ShoreikinDto> flatMap(t -> {
					List<Shoreikin> shoreikinList = shoreikinMap.get(t.getShiteiNo());
					Atena atena = atenaMap.get(t.getAtenaNo());
					boolean isGassanTarget = gassanMap.containsKey(t.getShiteiNo());

					// 合算対象フィルタ
					if (!"999".equals(form.getGassanTaisho())) {
						boolean shouldBeTarget = "2".equals(form.getGassanTaisho());
						if (shouldBeTarget != isGassanTarget) {
							return Stream.empty();
						}
					}

					// ステータスフィルタ
					if (!"999".equals(form.getStatus())) {
						String currentStatus = t.getStatus();
						if (!form.getStatus().equals(currentStatus)) {
							return Stream.empty();
						}
					}

					// shoreikinListがnullまたは空の場合の処理
					if (shoreikinList == null || shoreikinList.isEmpty()) {
						// 交付金算出有無フィルタ（算出無のみ表示する場合）
						if ("1".equals(form.getKofuSanshutsuUmu())) {
							// 算出有のみ表示する場合は除外
							return Stream.empty();
						} else {
							// 算出無のみ、またはすべて表示する場合
							ShoreikinDto dto = new ShoreikinDto();
							dto.setListShiteiNo(t.getShiteiNo());
							dto.setListShisetsuName(t.getShisetsuName());
							dto.setShimei(atena != null ? atena.getName() : null);
							dto.setKofuGaku(null);
							dto.setKofuNendo(null);
							dto.setKofuYmd(null);
							return Stream.of(dto);
						}
					}

					// shoreikinListから複数のDTOを生成
					return shoreikinList.stream()
							.filter(s -> {
								// 交付金算出有無フィルタ
								if ("2".equals(form.getKofuSanshutsuUmu())) {
									// 算出無のみ表示する場合は除外
									return false;
								}
								return true;
							})
							.map(s -> {
								ShoreikinDto dto = new ShoreikinDto();
								dto.setListShiteiNo(t.getShiteiNo());
								dto.setListShisetsuName(t.getShisetsuName());
								dto.setShimei(atena != null ? atena.getName() : null);
								dto.setKofuGaku(s.getKofuGaku());
								dto.setKofuNendo(s.getNendo() != null ? Integer.valueOf(s.getNendo()) : null);
								dto.setKofuYmd(s.getKofuYmd());
								return dto;
							});
				})
				.toList();

		// ページ分割
		PageRequest pageable = PageRequest.of(form.getPage(), form.getPageSize());
		int start = (int) pageable.getOffset();
		int end = Math.min(start + pageable.getPageSize(), result.size());
		List<ShoreikinDto> pageContent = start >= result.size() ? List.of() : result.subList(start, end);
		return new PageImpl<>(pageContent, pageable, result.size());

	}

	private String toLikePattern(String value, String matchType) {
		if (value == null || value.isBlank()) return null;
		return switch (matchType) {
			case "prefix" -> value + "%";
			case "exact"  -> value;
			default       -> "%" + value + "%";
		};
	}

	private List<Tokugimu> findTokugimuByGassanShiteiNo(String gassanShiteiNo) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
		if (gassanList.isEmpty()) {
			return List.of();
		}
		List<String> shiteiNos = gassanUchiRepository
				.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo)
				.stream().map(GassanUchi::getShiteiNo).toList();
		if (shiteiNos.isEmpty()) {
			return List.of();
		}
		return shiteiNos.stream()
				.flatMap(sn -> tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, sn).stream())
				.toList();
	}
}
