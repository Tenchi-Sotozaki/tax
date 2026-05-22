package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import jp.lg.asp.accommodation.entity.ShoreikinId;
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

	@Override
	@Transactional
	public int bulkCalculate(String nendo) {
		if (nendo == null || nendo.isEmpty()) {
			throw new IllegalArgumentException("年度が指定されていません");
		}

		// 対象となる特別徴収義務者を取得（del_flg='0', new_flg='1'）
		List<Tokugimu> tokugimuList = tokugimuRepository
				.findByJichitaiCdAndDelFlgAndNewFlg(jichitaiCd, "0", "1");

		int count = 0;
		for (Tokugimu tokugimu : tokugimuList) {
			// 既存の交付金レコードをチェック
			ShoreikinId id = new ShoreikinId(jichitaiCd, tokugimu.getShiteiNo(), nendo);

			if (!shoreikinRepository.existsById(id)) {
				// 交付金を算出（簡易実装：固定率3%）
				Long kofuZeigaku = calculateKofuZeigaku(tokugimu.getShiteiNo(), nendo);
				BigDecimal kofuRitsu = new BigDecimal("3.00");
				Long kofuGaku = new BigDecimal(kofuZeigaku).multiply(kofuRitsu).divide(new BigDecimal("100"))
						.longValue();

				Shoreikin shoreikin = new Shoreikin();
				shoreikin.setJichitaiCd(jichitaiCd);
				shoreikin.setShiteiNo(tokugimu.getShiteiNo());
				shoreikin.setNendo(nendo);
				shoreikin.setKofuZeigaku(kofuZeigaku);
				shoreikin.setKofuRitsu(kofuRitsu);
				shoreikin.setKofuGaku(kofuGaku);
				shoreikin.setAddDt(LocalDateTime.now());
				shoreikin.setAddUser("SYSTEM");
				shoreikin.setUpdDt(LocalDateTime.now());
				shoreikin.setUpdUser("SYSTEM");
				shoreikin.setVersion(1);

				shoreikinRepository.save(shoreikin);
				count++;
			}
		}

		return count;
	}

	private Long calculateKofuZeigaku(String shiteiNo, String nendo) {
		// 実際の実装では、t_fuka テーブルから該当年度の納入税額を集計
		// ここでは簡易実装として固定値を返す
		return 100000L;
	}
}
