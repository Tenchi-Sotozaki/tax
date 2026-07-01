package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.ShunoDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.ShunoRenkeiService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShunoRenkeiServiceImpl implements ShunoRenkeiService {

	private final EntityManager em;
	private final FukaRepository fukaRepository;
	private final TokugimuRepository tokugimuRepository;

	@Override
	public List<ShunoDto> search(String jichitaiCd, LocalDate shinkokuFrom, LocalDate shinkokuTo,
			String taishoMonth, String shiteiNo, String name) {

		// 申告日または対象月の条件がある場合はFukaテーブルを基準に検索
		if ((shinkokuFrom != null) || (shinkokuTo != null) || (taishoMonth != null && !taishoMonth.isEmpty())) {
			return searchFromFuka(jichitaiCd, shinkokuFrom, shinkokuTo, taishoMonth, shiteiNo, name);
		} else {
			// 申告日・対象月の条件がない場合はTokugimuテーブルを基準に検索
			return searchFromTokugimu(jichitaiCd, shiteiNo, name);
		}
	}

	private List<ShunoDto> searchFromFuka(String jichitaiCd, LocalDate shinkokuFrom, LocalDate shinkokuTo,
			String taishoMonth, String shiteiNo, String name) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Fuka> cq = cb.createQuery(Fuka.class);
		Root<Fuka> f = cq.from(Fuka.class);
		List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

		predicates.add(cb.equal(f.get("jichitaiCd"), jichitaiCd));

		if (shinkokuFrom != null) {
			predicates.add(cb.greaterThanOrEqualTo(f.get("shinkokuYmd"), shinkokuFrom));
		}
		if (shinkokuTo != null) {
			predicates.add(cb.lessThanOrEqualTo(f.get("shinkokuYmd"), shinkokuTo));
		}
		if (taishoMonth != null && !taishoMonth.isEmpty()) {
			String ym = taishoMonth.replace("-", "");
			predicates.add(cb.equal(f.get("taishoYm"), ym));
		}
		if (shiteiNo != null && !shiteiNo.isEmpty()) {
			predicates.add(cb.like(f.get("shiteiNo"), cb.literal('%' + shiteiNo + '%')));
		}

		// 氏名検索条件
		if (name != null && !name.isEmpty()) {
			jakarta.persistence.criteria.Subquery<Tokugimu> subquery = cq.subquery(Tokugimu.class);
			Root<Tokugimu> t = subquery.from(Tokugimu.class);
			Join<Tokugimu, Atena> atenaJoin = t.join("atena", JoinType.INNER);

			subquery.select(t)
					.where(cb.and(
							cb.equal(t.get("jichitaiCd"), f.get("jichitaiCd")),
							cb.equal(t.get("shiteiNo"), f.get("shiteiNo")),
							cb.like(atenaJoin.get("name"), cb.literal('%' + name + '%'))));

			predicates.add(cb.exists(subquery));
		}

		cq.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
		cq.orderBy(cb.asc(f.get("shiteiNo")), cb.asc(f.get("taishoYm")));

		TypedQuery<Fuka> q = em.createQuery(cq);
		List<Fuka> fukaList = q.getResultList();

		return fukaList.stream().map(fuka -> {
			List<Tokugimu> toks = tokugimuRepository.findByJichitaiCdAndShiteiNo(fuka.getJichitaiCd(),
					fuka.getShiteiNo());
			if (!toks.isEmpty()) {
				return toDtoFromTokugimuAndFuka(toks.get(0), fuka);
			}
			return null;
		}).filter(dto -> dto != null).collect(Collectors.toList());
	}

	private List<ShunoDto> searchFromTokugimu(String jichitaiCd, String shiteiNo, String name) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Tokugimu> cq = cb.createQuery(Tokugimu.class);
		Root<Tokugimu> t = cq.from(Tokugimu.class);
		List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

		predicates.add(cb.equal(t.get("jichitaiCd"), jichitaiCd));

		if (shiteiNo != null && !shiteiNo.isEmpty()) {
			predicates.add(cb.like(t.get("shiteiNo"), cb.literal('%' + shiteiNo + '%')));
		}

		// 氏名検索条件
		if (name != null && !name.isEmpty()) {
			Join<Tokugimu, Atena> atenaJoin = t.join("atena", JoinType.INNER);
			predicates.add(cb.like(atenaJoin.get("name"), cb.literal('%' + name + '%')));
		}

		cq.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
		cq.orderBy(cb.asc(t.get("shiteiNo")));

		TypedQuery<Tokugimu> q = em.createQuery(cq);
		List<Tokugimu> tokugimuList = q.getResultList();

		// 賦課情報がある場合のみ表示
		return tokugimuList.stream()
				.map(tokugimu -> {
					List<Fuka> fukaList = fukaRepository.findByJichitaiCdAndShiteiNo(jichitaiCd,
							tokugimu.getShiteiNo());
					if (fukaList.isEmpty()) {
						return null;
					} else {
						// 最新の賦課情報を使用
						Fuka latestFuka = fukaList.get(0); // ORDER BY で最新が先頭
						return toDtoFromTokugimuAndFuka(tokugimu, latestFuka);
					}
				})
				.filter(dto -> dto != null)
				.collect(Collectors.toList());
	}

	@Override
	public List<ShunoDto> findByKeys(String jichitaiCd, List<ShunoDto.Key> keys) {
		List<ShunoDto> result = new ArrayList<>();
		for (ShunoDto.Key k : keys) {
			List<Fuka> fukaList = fukaRepository.findLatestByNendoAndKibetsu(jichitaiCd, k.getShiteiNo(),
					k.getNendo(), k.getKibetsu());
			if (!fukaList.isEmpty()) {
				Fuka f = fukaList.get(0);
				List<Tokugimu> toks = tokugimuRepository.findByJichitaiCdAndShiteiNo(f.getJichitaiCd(),
						f.getShiteiNo());
				if (!toks.isEmpty()) {
					result.add(toDtoFromTokugimuAndFuka(toks.get(0), f));
				}
			}
		}
		return result;
	}

	private ShunoDto toDtoFromTokugimuAndFuka(Tokugimu t, Fuka f) {
		ShunoDto dto = new ShunoDto();
		dto.setJichitaiCd(t.getJichitaiCd());
		dto.setShiteiNo(t.getShiteiNo());
		if (t.getAtena() != null) {
			dto.setAtenaNo(t.getAtenaNo() != null ? String.valueOf(t.getAtenaNo()) : null);
			dto.setName(t.getAtena().getName());
		} else {
			dto.setAtenaNo(t.getAtenaNo() != null ? String.valueOf(t.getAtenaNo()) : null);
		}
		dto.setTaishoYm(f.getTaishoYm() != null && f.getTaishoYm().length() == 6
				? f.getTaishoYm().substring(0, 4) + "-" + f.getTaishoYm().substring(4)
				: f.getTaishoYm());
		dto.setTotalZeigaku(f.getTotalZeigaku());
		dto.setTorokuYmd(f.getTorokuYmd());
		dto.setShinkokuYmd(f.getShinkokuYmd());
		dto.setNendo(f.getNendo());
		dto.setKibetsu(f.getKibetsu());
		dto.setKasanKbn1(f.getKasanKbn1());
		dto.setKasanRitsu1(f.getKasanRitsu1());
		dto.setKasanGaku1(f.getKasanGaku1());
		dto.setNokigen1(f.getNokigen1());
		dto.setKasanKbn2(f.getKasanKbn2());
		dto.setKasanRitsu2(f.getKasanRitsu2());
		dto.setKasanGaku2(f.getKasanGaku2());
		dto.setNokigen2(f.getNokigen2());
		dto.setKasanKbn3(f.getKasanKbn3());
		dto.setKasanRitsu3(f.getKasanRitsu3());
		dto.setKasanGaku3(f.getKasanGaku3());
		dto.setNokigen3(f.getNokigen3());
		dto.setCityZeigaku(f.getCityZeigaku());
		dto.setKenZeigaku(f.getKenZeigaku());
		return dto;
	}
}
