package jp.lg.asp.accommodation.service.impl;

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
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.ShoreikinRenkeiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.FurikomiKoza;
import jp.lg.asp.accommodation.entity.Shoreikin;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.FurikomiKozaRepository;
import jp.lg.asp.accommodation.repository.ShoreikinRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.ShoreikinRenkeiService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ShoreikinRenkeiServiceImpl implements ShoreikinRenkeiService {

    private final EntityManager em;
    private final ShoreikinRepository shoreikinRepository;
    private final TokugimuRepository tokugimuRepository;
    private final FurikomiKozaRepository furikomiKozaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ShoreikinRenkeiDto> search(String jichitaiCd, String nendo, String shiteiNo, String name, String nameMatchType) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Shoreikin> cq = cb.createQuery(Shoreikin.class);
        Root<Shoreikin> s = cq.from(Shoreikin.class);
        List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(s.get("jichitaiCd"), jichitaiCd));
        // new_flgとdel_flgは存在しないため削除

        if (nendo != null && !nendo.isEmpty()) {
            predicates.add(cb.equal(s.get("nendo"), nendo));
        }
        if (shiteiNo != null && !shiteiNo.isEmpty()) {
            predicates.add(cb.like(s.get("shiteiNo"), cb.literal('%' + shiteiNo + '%')));
        }

        // 氏名検索条件
        if (name != null && !name.isEmpty()) {
            String namePattern = toLikePattern(name, nameMatchType);
            jakarta.persistence.criteria.Subquery<Tokugimu> subquery = cq.subquery(Tokugimu.class);
            Root<Tokugimu> t = subquery.from(Tokugimu.class);
            Join<Tokugimu, Atena> atenaJoin = t.join("atena", JoinType.INNER);
            
            subquery.select(t)
                .where(cb.and(
                    cb.equal(t.get("jichitaiCd"), s.get("jichitaiCd")),
                    cb.equal(t.get("shiteiNo"), s.get("shiteiNo")),
                    cb.like(atenaJoin.get("name"), cb.literal(namePattern))
                ));
            
            predicates.add(cb.exists(subquery));
        }

        cq.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        cq.orderBy(cb.desc(s.get("nendo")), cb.asc(s.get("shiteiNo")));

        TypedQuery<Shoreikin> q = em.createQuery(cq);
        List<Shoreikin> shoreikinList = q.getResultList();

        return shoreikinList.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShoreikinRenkeiDto> findByKeys(String jichitaiCd, List<ShoreikinRenkeiDto.Key> keys) {
        List<ShoreikinRenkeiDto> result = new ArrayList<>();
        for (ShoreikinRenkeiDto.Key k : keys) {
            shoreikinRepository.findByJichitaiCdAndShiteiNoAndNendo(jichitaiCd, k.getShiteiNo(), k.getNendo())
                    .ifPresent(shoreikin -> result.add(toDto(shoreikin)));
        }
        return result;
    }

    private String toLikePattern(String value, String matchType) {
        if (value == null || value.isBlank()) return "%";
        return switch (matchType) {
            case "prefix" -> value + "%";
            case "exact"  -> value;
            default       -> "%" + value + "%";
        };
    }

    private ShoreikinRenkeiDto toDto(Shoreikin s) {
        ShoreikinRenkeiDto dto = new ShoreikinRenkeiDto();
        dto.setJichitaiCd(s.getJichitaiCd());
        dto.setShiteiNo(s.getShiteiNo());
        dto.setNendo(s.getNendo());
        dto.setKofuZeigaku(s.getKofuZeigaku());
        dto.setKofuRitsu(s.getKofuRitsu());
        dto.setKofuGaku(s.getKofuGaku());
        dto.setKofuYmd(s.getKofuYmd());
        
        // Tokugimu情報を取得
        List<Tokugimu> toks = tokugimuRepository.findByJichitaiCdAndShiteiNo(s.getJichitaiCd(), s.getShiteiNo());
        if (!toks.isEmpty()) {
            Tokugimu t = toks.get(0);
            if (t.getAtena() != null) {
                dto.setAtenaNo(t.getAtenaNo() != null ? String.valueOf(t.getAtenaNo()) : null);
                dto.setName(t.getAtena().getName());
            } else {
                dto.setAtenaNo(t.getAtenaNo() != null ? String.valueOf(t.getAtenaNo()) : null);
            }
        }
        
        // 振込口座情報を取得
        furikomiKozaRepository.findByJichitaiCdAndShiteiNo(s.getJichitaiCd(), s.getShiteiNo())
            .ifPresent(koza -> {
                dto.setBankCd(koza.getBankCd());
                dto.setBankName(koza.getBankName());
                dto.setBranchCd(koza.getBranchCd());
                dto.setBranchName(koza.getBranchName());
                dto.setShumoku(koza.getShumoku());
                dto.setKozaNo(koza.getKozaNo());
                dto.setMeigi(koza.getMeigi());
            });
        
        return dto;
    }
}