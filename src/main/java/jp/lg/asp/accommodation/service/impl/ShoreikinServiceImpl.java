package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.ShoreikinDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
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
	private final GassanUchiRepository gassanUchiRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@Override
	@Transactional(readOnly = true)
	public List<ShoreikinDto> search(ShoreikinDto form) {

		List<Tokugimu> tokugimuList = tokugimuRepository.findBySearchConditions(
				jichitaiCd,
				form.getShiteiNo(),
				form.getName(),
				form.getShisetsuName(),
				form.getKyokaShu(),
				form.getKojinNo(),
				form.getHojinNo());

		if (tokugimuList.isEmpty()) {
			return List.of();
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
		Map<String, Shoreikin> shoreikinMap = shoreikinRepository
				.findByJichitaiCdAndShiteiNoInAndNendo(jichitaiCd, shiteiNos, form.getNendo())
				.stream()
				.collect(Collectors.toMap(Shoreikin::getShiteiNo, t -> t, (a, b) -> a));

		List<ShoreikinDto> result = tokugimuList.stream()
				.map(t -> {
					Shoreikin s = shoreikinMap.get(t.getShiteiNo());
					Atena atena = atenaMap.get(t.getAtenaNo());
					boolean isGassanTarget = gassanMap.containsKey(t.getShiteiNo());

					// 合算対象フィルタ
					if (!"999".equals(form.getGassanTaisho())) {
						boolean shouldBeTarget = "2".equals(form.getGassanTaisho());
						if (shouldBeTarget != isGassanTarget) {
							return null;
						}
					}

					// ステータスフィルタ
					if (!"999".equals(form.getStatus())) {
						String currentStatus = t.getStatus();
						if (!form.getStatus().equals(currentStatus)) {
							return null;
						}
					}

					// 交付金算出有無フィルタ（1=算出有: kofu_gaku > 0 / 2=算出無: kofu_gaku = 0）
					if (!"999".equals(form.getKofuSanshutsuUmu())) {
						boolean hasKofu = s != null && s.getKofuGaku() != null && s.getKofuGaku() > 0;
						if ("1".equals(form.getKofuSanshutsuUmu()) && !hasKofu) {
							return null;
						}
						if ("2".equals(form.getKofuSanshutsuUmu()) && hasKofu) {
							return null;
						}
					}

					ShoreikinDto dto = new ShoreikinDto();
					dto.setListShiteiNo(t.getShiteiNo());
					dto.setListShisetsuName(t.getShisetsuName());
					dto.setShimei(atena != null ? atena.getName() : null);
					dto.setKofuGaku(s != null ? s.getKofuGaku() : null);
					return dto;
				})
				.filter(dto -> dto != null)
				.toList();

		return result;

	}
}
