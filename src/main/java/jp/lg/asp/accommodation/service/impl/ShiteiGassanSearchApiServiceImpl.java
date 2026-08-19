package jp.lg.asp.accommodation.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.ShiteiGassanSearchApiService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShiteiGassanSearchApiServiceImpl implements ShiteiGassanSearchApiService {

    private final TokugimuRepository tokugimuRepository;
    private final GassanUchiRepository gassanUchiRepository;
    private final GassanRepository gassanRepository;
    private final AtenaRepository atenaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ShiteiGassanSearchDto> searchByShiteiNo(String jichitaiCd, String shiteiNo) {
        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        if (tokugimuList.isEmpty()) return List.of();

        List<ShiteiGassanSearchDto> results = new ArrayList<>();
        Tokugimu t = tokugimuList.get(0);
        String atenaName = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, t.getAtenaNo())
                .map(Atena::getName).orElse("");

        results.add(new ShiteiGassanSearchDto(t.getAtenaNo().toPlainString(), t.getShiteiNo(), null, atenaName, t.getShisetsuName()));

        List<GassanUchi> uchiList = gassanUchiRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        for (GassanUchi uchi : uchiList) {
            List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, uchi.getGassanShiteiNo());
            if (!gassanList.isEmpty()) {
                Gassan representative = gassanList.get(0);
                String repShiteiNo = representative.getShiteiNo();
                String repName = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, representative.getAtenaNo())
                        .map(Atena::getName).orElse("");
                String repShisetsuName = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, repShiteiNo)
                        .stream().findFirst().map(Tokugimu::getShisetsuName).orElse("");
                results.add(new ShiteiGassanSearchDto(representative.getAtenaNo().toPlainString(), repShiteiNo, uchi.getGassanShiteiNo(), repName, repShisetsuName));
            }
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiteiGassanSearchDto> searchByGassanShiteiNo(String jichitaiCd, String gassanShiteiNo) {
        List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        if (gassanList.isEmpty()) return List.of();

        Gassan representative = gassanList.get(0);
        String repShiteiNo = representative.getShiteiNo();
        String repName = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, representative.getAtenaNo())
                .map(Atena::getName).orElse("");
        String repShisetsuName = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, repShiteiNo)
                .stream().findFirst().map(Tokugimu::getShisetsuName).orElse("");

        List<ShiteiGassanSearchDto> results = new ArrayList<>();
        results.add(new ShiteiGassanSearchDto(representative.getAtenaNo().toPlainString(), repShiteiNo, gassanShiteiNo, repName, repShisetsuName));
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiteiGassanSearchDto> searchByName(String jichitaiCd, String name, String matchType) {
        String namePattern = applyMatchPattern(name, matchType);
        List<Atena> atenaList = atenaRepository.search(jichitaiCd, "%", namePattern, "%", "%", "%", "%", "%", "%");
        return searchTokugimuByAtenaList(jichitaiCd, atenaList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiteiGassanSearchDto> searchByShisetsuName(String jichitaiCd, String shisetsuName, String matchType) {
        List<Tokugimu> allTokugimu = tokugimuRepository.findAllByJichitaiCd(jichitaiCd);
        List<Tokugimu> filtered = allTokugimu.stream()
                .filter(t -> matchesName(t.getShisetsuName(), shisetsuName, matchType))
                .toList();
        return toDtoWithGassan(jichitaiCd, filtered);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiteiGassanSearchDto> searchByKojinNo(String jichitaiCd, String kojinNo) {
        List<Atena> atenaList = atenaRepository.search(jichitaiCd, "%", "%", "%", "%", "%", "%", kojinNo, "%");
        return searchTokugimuByAtenaList(jichitaiCd, atenaList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiteiGassanSearchDto> searchByHojinNo(String jichitaiCd, String hojinNo) {
        List<Atena> atenaList = atenaRepository.search(jichitaiCd, "%", "%", "%", "%", "%", "%", "%", hojinNo);
        return searchTokugimuByAtenaList(jichitaiCd, atenaList);
    }

    private List<ShiteiGassanSearchDto> searchTokugimuByAtenaList(String jichitaiCd, List<Atena> atenaList) {
        List<ShiteiGassanSearchDto> results = new ArrayList<>();
        for (Atena atena : atenaList) {
            List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atena.getAtenaNo());
            results.addAll(toDtoWithGassan(jichitaiCd, tokugimuList));
        }
        return results;
    }

    private List<ShiteiGassanSearchDto> toDtoWithGassan(String jichitaiCd, List<Tokugimu> tokugimuList) {
        List<ShiteiGassanSearchDto> results = new ArrayList<>();
        Set<String> addedGassanShiteiNos = new HashSet<>();
        for (Tokugimu t : tokugimuList) {
            String atenaName = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, t.getAtenaNo())
                    .map(Atena::getName).orElse("");

            results.add(new ShiteiGassanSearchDto(t.getAtenaNo().toPlainString(), t.getShiteiNo(), null, atenaName, t.getShisetsuName()));

            List<GassanUchi> uchiList = gassanUchiRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, t.getShiteiNo());
            for (GassanUchi uchi : uchiList) {
                if (addedGassanShiteiNos.contains(uchi.getGassanShiteiNo())) continue;
                List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, uchi.getGassanShiteiNo());
                if (!gassanList.isEmpty()) {
                    Gassan representative = gassanList.get(0);
                    String repShiteiNo = representative.getShiteiNo();
                    String repName = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, representative.getAtenaNo())
                            .map(Atena::getName).orElse("");
                    String repShisetsuName = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, repShiteiNo)
                            .stream().findFirst().map(Tokugimu::getShisetsuName).orElse("");
                    results.add(new ShiteiGassanSearchDto(representative.getAtenaNo().toPlainString(), repShiteiNo, uchi.getGassanShiteiNo(), repName, repShisetsuName));
                    addedGassanShiteiNos.add(uchi.getGassanShiteiNo());
                }
            }
        }
        return results;
    }

    private String applyMatchPattern(String value, String matchType) {
        if (value == null) return "%";
        if ("exact".equals(matchType)) return value;
        if ("prefix".equals(matchType)) return value + "%";
        return "%" + value + "%";
    }

    private boolean matchesName(String target, String keyword, String matchType) {
        if (target == null || keyword == null) return false;
        if ("exact".equals(matchType)) return target.equals(keyword);
        if ("prefix".equals(matchType)) return target.startsWith(keyword);
        return target.contains(keyword);
    }
}
