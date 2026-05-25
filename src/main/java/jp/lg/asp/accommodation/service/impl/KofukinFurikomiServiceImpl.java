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
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.KofukinFurikomiDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.KofukinFurikomi;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.KofukinFurikomiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class KofukinFurikomiServiceImpl implements jp.lg.asp.accommodation.service.KofukinFurikomiService {

    private final EntityManager em;
    private final KofukinFurikomiRepository kofukinFurikomiRepository;
    private final TokugimuRepository tokugimuRepository;

    @Override
    @Transactional(readOnly = true)
    public List<KofukinFurikomiDto> search(String jichitaiCd, LocalDate furikomiFrom, LocalDate furikomiTo,
            String taishoMonth, String shiteiNo, String name, String furikomiStatus) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<KofukinFurikomi> cq = cb.createQuery(KofukinFurikomi.class);
        Root<KofukinFurikomi> k = cq.from(KofukinFurikomi.class);
        List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(k.get("jichitaiCd"), jichitaiCd));
        predicates.add(cb.equal(k.get("newFlg"), "1"));
        predicates.add(cb.equal(k.get("delFlg"), "0"));

        if (furikomiFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(k.get("furikomiYmd"), furikomiFrom));
        }
        if (furikomiTo != null) {
            predicates.add(cb.lessThanOrEqualTo(k.get("furikomiYmd"), furikomiTo));
        }
        if (taishoMonth != null && !taishoMonth.isEmpty()) {
            String ym = taishoMonth.replace("-", "");
            predicates.add(cb.equal(k.get("taishoYm"), ym));
        }
        if (shiteiNo != null && !shiteiNo.isEmpty()) {
            predicates.add(cb.like(k.get("shiteiNo"), cb.literal('%' + shiteiNo + '%')));
        }
        if (furikomiStatus != null && !furikomiStatus.isEmpty()) {
            predicates.add(cb.equal(k.get("furikomiStatus"), furikomiStatus));
        }

        // 氏名検索条件
        if (name != null && !name.isEmpty()) {
            jakarta.persistence.criteria.Subquery<Tokugimu> subquery = cq.subquery(Tokugimu.class);
            Root<Tokugimu> t = subquery.from(Tokugimu.class);
            Join<Tokugimu, Atena> atenaJoin = t.join("atena", JoinType.INNER);
            
            subquery.select(t)
                .where(cb.and(
                    cb.equal(t.get("jichitaiCd"), k.get("jichitaiCd")),
                    cb.equal(t.get("shiteiNo"), k.get("shiteiNo")),
                    cb.like(atenaJoin.get("name"), cb.literal('%' + name + '%'))
                ));
            
            predicates.add(cb.exists(subquery));
        }

        cq.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        cq.orderBy(cb.desc(k.get("furikomiYmd")), cb.asc(k.get("shiteiNo")));

        TypedQuery<KofukinFurikomi> q = em.createQuery(cq);
        List<KofukinFurikomi> kofukinList = q.getResultList();

        return kofukinList.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<KofukinFurikomiDto> findByKeys(String jichitaiCd, List<KofukinFurikomiDto.Key> keys) {
        List<KofukinFurikomiDto> result = new ArrayList<>();
        for (KofukinFurikomiDto.Key k : keys) {
            kofukinFurikomiRepository.findByJichitaiCdAndShiteiNoAndTaishoYmAndRno(
                    jichitaiCd, k.getShiteiNo(), k.getTaishoYm(), k.getRno())
                    .ifPresent(kofukin -> result.add(toDto(kofukin)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public KofukinFurikomiDto findById(String jichitaiCd, String shiteiNo, String taishoYm, Integer rno) {
        return kofukinFurikomiRepository.findByJichitaiCdAndShiteiNoAndTaishoYmAndRno(jichitaiCd, shiteiNo, taishoYm, rno)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    public void save(KofukinFurikomiDto dto) {
        KofukinFurikomi entity = toEntity(dto);
        kofukinFurikomiRepository.save(entity);
    }

    @Override
    public void delete(String jichitaiCd, String shiteiNo, String taishoYm, Integer rno) {
        kofukinFurikomiRepository.findByJichitaiCdAndShiteiNoAndTaishoYmAndRno(jichitaiCd, shiteiNo, taishoYm, rno)
                .ifPresent(entity -> {
                    entity.setDelFlg("1");
                    kofukinFurikomiRepository.save(entity);
                });
    }

    private KofukinFurikomiDto toDto(KofukinFurikomi k) {
        KofukinFurikomiDto dto = new KofukinFurikomiDto();
        dto.setJichitaiCd(k.getJichitaiCd());
        dto.setShiteiNo(k.getShiteiNo());
        dto.setRno(k.getRno());
        
        // Tokugimu情報を取得
        List<Tokugimu> toks = tokugimuRepository.findByJichitaiCdAndShiteiNo(k.getJichitaiCd(), k.getShiteiNo());
        if (!toks.isEmpty()) {
            Tokugimu t = toks.get(0);
            if (t.getAtena() != null) {
                dto.setAtenaNo(t.getAtenaNo() != null ? String.valueOf(t.getAtenaNo()) : null);
                dto.setName(t.getAtena().getName());
            } else {
                dto.setAtenaNo(t.getAtenaNo() != null ? String.valueOf(t.getAtenaNo()) : null);
            }
        }
        
        dto.setTaishoYm(k.getTaishoYm() != null && k.getTaishoYm().length() == 6 ? 
                k.getTaishoYm().substring(0,4) + "-" + k.getTaishoYm().substring(4) : k.getTaishoYm());
        dto.setTorokuYmd(k.getTorokuYmd());
        dto.setFurikomiYmd(k.getFurikomiYmd());
        dto.setFurikomiGaku(k.getFurikomiGaku());
        dto.setShiharaiGaku(k.getShiharaiGaku());
        dto.setTesuryo(k.getTesuryo());
        dto.setFurikomiKbn(k.getFurikomiKbn());
        dto.setFurikomiStatus(k.getFurikomiStatus());
        dto.setGinkoCd(k.getGinkoCd());
        dto.setGinkoName(k.getGinkoName());
        dto.setShitenCd(k.getShitenCd());
        dto.setShitenName(k.getShitenName());
        dto.setYokinShubetsu(k.getYokinShubetsu());
        dto.setKozaNo(k.getKozaNo());
        dto.setKozaMeigi(k.getKozaMeigi());
        dto.setBiko(k.getBiko());
        return dto;
    }

    private KofukinFurikomi toEntity(KofukinFurikomiDto dto) {
        KofukinFurikomi entity = new KofukinFurikomi();
        entity.setJichitaiCd(dto.getJichitaiCd());
        entity.setShiteiNo(dto.getShiteiNo());
        entity.setRno(dto.getRno());
        entity.setTaishoYm(dto.getTaishoYm() != null ? dto.getTaishoYm().replace("-", "") : null);
        entity.setTorokuYmd(dto.getTorokuYmd());
        entity.setFurikomiYmd(dto.getFurikomiYmd());
        entity.setFurikomiGaku(dto.getFurikomiGaku());
        entity.setShiharaiGaku(dto.getShiharaiGaku());
        entity.setTesuryo(dto.getTesuryo());
        entity.setFurikomiKbn(dto.getFurikomiKbn());
        entity.setFurikomiStatus(dto.getFurikomiStatus());
        entity.setGinkoCd(dto.getGinkoCd());
        entity.setGinkoName(dto.getGinkoName());
        entity.setShitenCd(dto.getShitenCd());
        entity.setShitenName(dto.getShitenName());
        entity.setYokinShubetsu(dto.getYokinShubetsu());
        entity.setKozaNo(dto.getKozaNo());
        entity.setKozaMeigi(dto.getKozaMeigi());
        entity.setBiko(dto.getBiko());
        entity.setNewFlg("1");
        entity.setDelFlg("0");
        return entity;
    }
}