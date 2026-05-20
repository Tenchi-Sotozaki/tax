package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
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

import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.ShunoRenkeiService;
import jp.lg.asp.accommodation.service.dto.ShunoDto;
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
            // convert YYYY-MM to YYYYMM
            String ym = taishoMonth.replace("-", "");
            predicates.add(cb.equal(f.get("taishoYm"), ym));
        }
        if (shiteiNo != null && !shiteiNo.isEmpty()) {
            predicates.add(cb.like(f.get("shiteiNo"), cb.literal('%' + shiteiNo + '%')));
        }

        cq.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        cq.orderBy(cb.asc(f.get("shiteiNo")), cb.asc(f.get("taishoYm")));

        TypedQuery<Fuka> q = em.createQuery(cq);
        List<Fuka> list = q.getResultList();

        // If name filter provided, further filter by joining Tokugimu->Atena
        List<Fuka> filtered = list;
        if (name != null && !name.isEmpty()) {
            filtered = list.stream().filter(fuka -> {
                List<Tokugimu> toks = tokugimuRepository.findByJichitaiCdAndShiteiNo(fuka.getJichitaiCd(), fuka.getShiteiNo());
                return toks.stream().anyMatch(t -> t.getAtena() != null && t.getAtena().getName() != null && t.getAtena().getName().contains(name));
            }).collect(Collectors.toList());
        }

        return filtered.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ShunoDto> findByKeys(String jichitaiCd, List<ShunoDto.Key> keys) {
        List<ShunoDto> result = new ArrayList<>();
        for (ShunoDto.Key k : keys) {
            fukaRepository.findByJichitaiCdAndShiteiNoAndNendoAndKibetsu(jichitaiCd, k.getShiteiNo(), k.getNendo(), k.getKibetsu())
                    .ifPresent(f -> result.add(toDto(f)));
        }
        return result;
    }

    private ShunoDto toDto(Fuka f) {
        ShunoDto dto = new ShunoDto();
        dto.setJichitaiCd(f.getJichitaiCd());
        dto.setShiteiNo(f.getShiteiNo());
        // try to get atena via tokugimu
        List<Tokugimu> toks = tokugimuRepository.findByJichitaiCdAndShiteiNo(f.getJichitaiCd(), f.getShiteiNo());
        if (!toks.isEmpty()) {
            Tokugimu t = toks.get(0);
            if (t.getAtena() != null) {
                dto.setAtenaNo(t.getAtenaNo() != null ? String.valueOf(t.getAtenaNo()) : null);
                dto.setName(t.getAtena().getName());
            } else {
                dto.setAtenaNo(t.getAtenaNo() != null ? String.valueOf(t.getAtenaNo()) : null);
            }
        }
        dto.setTaishoYm(f.getTaishoYm() != null && f.getTaishoYm().length() == 6 ? f.getTaishoYm().substring(0,4) + "-" + f.getTaishoYm().substring(4) : f.getTaishoYm());
        dto.setTotalZeigaku(f.getTotalZeigaku());
        dto.setTorokuYmd(f.getTorokuYmd());
        dto.setShinkokuYmd(f.getShinkokuYmd());
        dto.setNendo(f.getNendo());
        dto.setKibetsu(f.getKibetsu());
        dto.setKasanKbn(f.getKasanKbn());
        dto.setKasanRitsu(f.getKasanRitsu());
        dto.setKasanGaku(f.getKasanGaku());
        dto.setNokigen(f.getNokigen());
        dto.setCityZeigaku(f.getCityZeigaku());
        dto.setKenZeigaku(f.getKenZeigaku());
        return dto;
    }
}
