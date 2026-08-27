package jp.lg.asp.accommodation.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
import jp.lg.asp.accommodation.util.SessionHelper;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/shitei-gassan")
@RequiredArgsConstructor
public class ShiteiGassanSearchApiController {

    private final TokugimuRepository tokugimuRepository;
    private final GassanUchiRepository gassanUchiRepository;
    private final GassanRepository gassanRepository;
    private final AtenaRepository atenaRepository;

    /** @deprecated {@link SessionHelper#SHITEI_GASSAN_KEY} を使用してください */
    @Deprecated
    public static final String SESSION_KEY = SessionHelper.SHITEI_GASSAN_KEY;

    private final JichitaiContext jichitaiContext;

    @GetMapping("/search")
    public List<ShiteiGassanSearchDto> search(
            @RequestParam(required = false) String shiteiNo,
            @RequestParam(required = false) String gassanShiteiNo,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String nameMatchType,
            @RequestParam(required = false) String shisetsuName,
            @RequestParam(required = false) String shisetsuNameMatchType,
            @RequestParam(required = false) String kojinNo,
            @RequestParam(required = false) String hojinNo) {

        if (!StringUtils.hasText(shiteiNo) && !StringUtils.hasText(gassanShiteiNo)
                && !StringUtils.hasText(name) && !StringUtils.hasText(shisetsuName)
                && !StringUtils.hasText(kojinNo) && !StringUtils.hasText(hojinNo)) {
            return List.of();
        }

        if (StringUtils.hasText(shiteiNo)) {
            return searchByShiteiNo(shiteiNo);
        }
        if (StringUtils.hasText(gassanShiteiNo)) {
            return searchByGassanShiteiNo(gassanShiteiNo);
        }
        if (StringUtils.hasText(name)) {
            return searchByName(name, nameMatchType);
        }
        if (StringUtils.hasText(shisetsuName)) {
            return searchByShisetsuName(shisetsuName, shisetsuNameMatchType);
        }
        if (StringUtils.hasText(kojinNo)) {
            return searchByKojinNo(kojinNo);
        }
        if (StringUtils.hasText(hojinNo)) {
            return searchByHojinNo(hojinNo);
        }
        return List.of();
    }

    @PostMapping("/select")
    public ShiteiGassanSearchDto select(@RequestBody ShiteiGassanSearchDto dto, HttpSession session) {
        SessionHelper.saveShiteiGassan(session, dto);
        return dto;
    }

    @GetMapping("/selected")
    public ShiteiGassanSearchDto getSelected(HttpSession session) {
        ShiteiGassanSearchDto dto = SessionHelper.getShiteiGassan(session);
        return dto != null ? dto : new ShiteiGassanSearchDto();
    }

    private List<ShiteiGassanSearchDto> searchByShiteiNo(String shiteiNo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        if (tokugimuList.isEmpty()) return List.of();

        List<ShiteiGassanSearchDto> results = new ArrayList<>();
        Tokugimu t = tokugimuList.get(0);
        String atenaName = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, t.getAtenaNo())
                .map(Atena::getName).orElse("");

        // 合算指定番号なしのレコード
        results.add(new ShiteiGassanSearchDto(t.getAtenaNo().toPlainString(), t.getShiteiNo(), null, atenaName, t.getShisetsuName()));

        // 合算指定番号ありのレコード（同じ合算指定番号は1つにまとめ、代表指定番号の情報を表示）
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

    private List<ShiteiGassanSearchDto> searchByGassanShiteiNo(String gassanShiteiNo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        if (gassanList.isEmpty()) return List.of();

        // 代表指定番号の情報を取得し、合算指定番号は1行にまとめて表示
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

    private List<ShiteiGassanSearchDto> searchByName(String name, String matchType) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        String namePattern = applyMatchPattern(name, matchType);
        List<Atena> atenaList = atenaRepository.search(jichitaiCd, "%", namePattern, "%", "%", "%", "%", "%", "%");
        return searchTokugimuByAtenaList(atenaList);
    }

    private List<ShiteiGassanSearchDto> searchByShisetsuName(String shisetsuName, String matchType) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        List<Tokugimu> allTokugimu = tokugimuRepository.findAllByJichitaiCd(jichitaiCd);
        List<Tokugimu> filtered = allTokugimu.stream()
                .filter(t -> matchesName(t.getShisetsuName(), shisetsuName, matchType))
                .toList();
        return toDtoWithGassan(filtered);
    }

    private List<ShiteiGassanSearchDto> searchByKojinNo(String kojinNo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        List<Atena> atenaList = atenaRepository.search(jichitaiCd, "%", "%", "%", "%", "%", "%", kojinNo, "%");
        return searchTokugimuByAtenaList(atenaList);
    }

    private List<ShiteiGassanSearchDto> searchByHojinNo(String hojinNo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        List<Atena> atenaList = atenaRepository.search(jichitaiCd, "%", "%", "%", "%", "%", "%", "%", hojinNo);
        return searchTokugimuByAtenaList(atenaList);
    }

    private List<ShiteiGassanSearchDto> searchTokugimuByAtenaList(List<Atena> atenaList) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        List<ShiteiGassanSearchDto> results = new ArrayList<>();
        for (Atena atena : atenaList) {
            List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atena.getAtenaNo());
            results.addAll(toDtoWithGassan(tokugimuList));
        }
        return results;
    }

    private List<ShiteiGassanSearchDto> toDtoWithGassan(List<Tokugimu> tokugimuList) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        List<ShiteiGassanSearchDto> results = new ArrayList<>();
        Set<String> addedGassanShiteiNos = new HashSet<>();
        for (Tokugimu t : tokugimuList) {
            String atenaName = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, t.getAtenaNo())
                    .map(Atena::getName).orElse("");

            // 合算指定番号なしのレコード
            results.add(new ShiteiGassanSearchDto(t.getAtenaNo().toPlainString(), t.getShiteiNo(), null, atenaName, t.getShisetsuName()));

            // 合算指定番号ありのレコード（同じ合算指定番号は1つにまとめ、代表指定番号の情報を表示）
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
        if ("exact".equals(matchType)) {
            return value;
        } else if ("prefix".equals(matchType)) {
            return value + "%";
        } else {
            return "%" + value + "%";
        }
    }

    private boolean matchesName(String target, String keyword, String matchType) {
        if (target == null || keyword == null) return false;
        if ("exact".equals(matchType)) {
            return target.equals(keyword);
        } else if ("prefix".equals(matchType)) {
            return target.startsWith(keyword);
        } else {
            return target.contains(keyword);
        }
    }
}
