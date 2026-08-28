package jp.lg.asp.accommodation.service.impl;
import jp.lg.asp.accommodation.config.JichitaiContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	public List<ShoreikinDto> search(ShoreikinDto form) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();

		List<Tokugimu> tokugimuList;
		if (form.getGassanShiteiNo() != null && !form.getGassanShiteiNo().isBlank()) {
			tokugimuList = findTokugimuByGassanShiteiNo(form.getGassanShiteiNo());
		} else if (form.getShiteiNo() != null && form.getShiteiNo().startsWith("9")) {
			tokugimuList = findTokugimuByGassanShiteiNo(form.getShiteiNo());
		} else {
			tokugimuList = tokugimuRepository.findBySearchConditions(
					jichitaiCd,
					form.getShiteiNo(),
					form.getName(),
					toLikePattern(form.getName(), form.getNameMatchType()),
					form.getShisetsuName(),
					toLikePattern(form.getShisetsuName(), form.getShisetsuNameMatchType()),
					null,
					null,
					null);
		}

		if (tokugimuList.isEmpty()) {
			return List.of();
		}

		List<BigDecimal> atenaNos = tokugimuList.stream().map(Tokugimu::getAtenaNo).distinct().toList();
		List<String> shiteiNos = tokugimuList.stream().map(Tokugimu::getShiteiNo).toList();

		Map<BigDecimal, Atena> atenaMap = atenaRepository
				.findByJichitaiCdAndAtenaNoIn(jichitaiCd, atenaNos)
				.stream()
				.collect(Collectors.toMap(Atena::getAtenaNo, a -> a));

		Map<String, List<Shoreikin>> shoreikinMap = shoreikinRepository
				.findByJichitaiCdAndShiteiNoInAndNendo(jichitaiCd, shiteiNos, form.getNendo())
				.stream()
				.collect(Collectors.toMap(Shoreikin::getShiteiNo,
						t -> new ArrayList<>(List.of(t)),
						(a, b) -> {
							a.addAll(b);
							return a;
						}));

		return tokugimuList.stream()
				.<ShoreikinDto> flatMap(t -> {
					List<Shoreikin> shoreikinList = shoreikinMap.get(t.getShiteiNo());
					Atena atena = atenaMap.get(t.getAtenaNo());
					if (shoreikinList == null || shoreikinList.isEmpty()) {
						if ("1".equals(form.getKofuSanshutsuUmu())) {
							return Stream.empty();
						}
						ShoreikinDto dto = new ShoreikinDto();
						dto.setListShiteiNo(t.getShiteiNo());
						dto.setListShisetsuName(t.getShisetsuName());
						dto.setShimei(atena != null ? atena.getName() : null);
						return Stream.of(dto);
					}
					return shoreikinList.stream()
							.filter(s -> !"2".equals(form.getKofuSanshutsuUmu()))
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
