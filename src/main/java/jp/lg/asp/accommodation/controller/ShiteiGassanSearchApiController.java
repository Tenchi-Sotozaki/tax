package jp.lg.asp.accommodation.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import jp.lg.asp.accommodation.dto.ShiteiGassanSearchDto;
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

    public static final String SESSION_KEY = "selectedShiteiGassan";

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

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
        session.setAttribute(SESSION_KEY, dto);
        return dto;
    }

    @GetMapping("/selected")
    public ShiteiGassanSearchDto getSelected(HttpSession session) {
        ShiteiGassanSearchDto dto = (ShiteiGassanSearchDto) session.getAttribute(SESSION_KEY);
        return dto != null ? dto : new ShiteiGassanSearchDto();
    }

    private List<ShiteiGassanSearchDto> searchByShiteiNo(String shiteiNo) {
        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        if (tokugimuList.isEmpty()) return List.of();

        List<ShiteiGassanSearchDto> results = new ArrayList<>();
        Tokugimu t = tokugimuList.get(0);
        String atenaName = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, t.getAtenaNo())
                .map(Atena::getName).orElse("");

        // 合算指定番号なしのレコード
        results.add(new ShiteiGassanSearchDto(t.getAtenaNo().toPlainString(), t.getShiteiNo(), null, atenaName, t.getShisetsuName()));

        // 合算指定番号ありのレコード
        List<GassanUchi> uchiList = gassanUchiRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        for (GassanUchi uchi : uchiList) {
            List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, uchi.getGassanShiteiNo());
            if (!gassanList.isEmpty()) {
                results.add(new ShiteiGassanSearchDto(t.getAtenaNo().toPlainString(), t.getShiteiNo(), uchi.getGassanShiteiNo(), atenaName, t.getShisetsuName()));
            }
        }
        return results;
    }

    private List<ShiteiGassanSearchDto> searchByGassanShiteiNo(String gassanShiteiNo) {
        List<GassanUchi> uchiList = gassanUchiRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        List<ShiteiGassanSearchDto> results = new ArrayList<>();
        for (GassanUchi uchi : uchiList) {
            List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, uchi.getShiteiNo());
            for (Tokugimu t : tokugimuList) {
                String atenaName = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, t.getAtenaNo())
                        .map(Atena::getName).orElse("");
                // 合算指定番号なし
                results.add(new ShiteiGassanSearchDto(t.getAtenaNo().toPlainString(), t.getShiteiNo(), null, atenaName, t.getShisetsuName()));
                // 合算指定番号あり
                results.add(new ShiteiGassanSearchDto(t.getAtenaNo().toPlainString(), t.getShiteiNo(), gassanShiteiNo, atenaName, t.getShisetsuName()));
            }
        }
        return results;
    }

    private List<ShiteiGassanSearchDto> searchByName(String name, String matchType) {
        List<Atena> atenaList = atenaRepository.search(jichitaiCd, null, applyMatch(name, matchType), null, null, null, null, null, null);
        return searchTokugimuByAtenaList(atenaList);
    }

    private List<ShiteiGassanSearchDto> searchByShisetsuName(String shisetsuName, String matchType) {
        List<Tokugimu> allTokugimu = tokugimuRepository.findAllByJichitaiCd(jichitaiCd);
        List<Tokugimu> filtered = allTokugimu.stream()
                .filter(t -> matchesName(t.getShisetsuName(), shisetsuName, matchType))
                .toList();
        return toDtoWithGassan(filtered);
    }

    private List<ShiteiGassanSearchDto> searchByKojinNo(String kojinNo) {
        List<Atena> atenaList = atenaRepository.search(jichitaiCd, null, null, null, null, null, null, kojinNo, null);
        return searchTokugimuByAtenaList(atenaList);
    }

    private List<ShiteiGassanSearchDto> searchByHojinNo(String hojinNo) {
        List<Atena> atenaList = atenaRepository.search(jichitaiCd, null, null, null, null, null, null, null, hojinNo);
        return searchTokugimuByAtenaList(atenaList);
    }

    private List<ShiteiGassanSearchDto> searchTokugimuByAtenaList(List<Atena> atenaList) {
        List<ShiteiGassanSearchDto> results = new ArrayList<>();
        for (Atena atena : atenaList) {
            List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atena.getAtenaNo());
            results.addAll(toDtoWithGassan(tokugimuList));
        }
        return results;
    }

    private List<ShiteiGassanSearchDto> toDtoWithGassan(List<Tokugimu> tokugimuList) {
        List<ShiteiGassanSearchDto> results = new ArrayList<>();
        for (Tokugimu t : tokugimuList) {
            String atenaName = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, t.getAtenaNo())
                    .map(Atena::getName).orElse("");

            // 合算指定番号なしのレコード
            results.add(new ShiteiGassanSearchDto(t.getAtenaNo().toPlainString(), t.getShiteiNo(), null, atenaName, t.getShisetsuName()));

            // 合算指定番号ありのレコード
            List<GassanUchi> uchiList = gassanUchiRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, t.getShiteiNo());
            for (GassanUchi uchi : uchiList) {
                List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, uchi.getGassanShiteiNo());
                if (!gassanList.isEmpty()) {
                    results.add(new ShiteiGassanSearchDto(t.getAtenaNo().toPlainString(), t.getShiteiNo(), uchi.getGassanShiteiNo(), atenaName, t.getShisetsuName()));
                }
            }
        }
        return results;
    }

    private List<ShiteiGassanSearchDto> toDto(List<Tokugimu> tokugimuList, String gassanNo) {
        List<ShiteiGassanSearchDto> results = new ArrayList<>();
        for (Tokugimu t : tokugimuList) {
            String atenaName = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, t.getAtenaNo())
                    .map(Atena::getName).orElse("");
            results.add(new ShiteiGassanSearchDto(t.getAtenaNo().toPlainString(), t.getShiteiNo(), gassanNo, atenaName, t.getShisetsuName()));
        }
        return results;
    }

    private String applyMatch(String value, String matchType) {
        if (value == null) return null;
        return value;
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
