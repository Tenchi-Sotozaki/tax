package jp.lg.asp.accommodation.controller;

import java.util.List;

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
import jp.lg.asp.accommodation.service.ShiteiGassanSearchApiService;
import jp.lg.asp.accommodation.util.SessionHelper;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/shitei-gassan")
@RequiredArgsConstructor
public class ShiteiGassanSearchApiController {

    private final ShiteiGassanSearchApiService shiteiGassanSearchApiService;
    private final JichitaiContext jichitaiContext;

    /** @deprecated {@link SessionHelper#SHITEI_GASSAN_KEY} を使用してください */
    @Deprecated
    public static final String SESSION_KEY = SessionHelper.SHITEI_GASSAN_KEY;

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

        String jichitaiCd = jichitaiContext.getJichitaiCd();

        if (StringUtils.hasText(shiteiNo)) {
            return shiteiGassanSearchApiService.searchByShiteiNo(jichitaiCd, shiteiNo);
        }
        if (StringUtils.hasText(gassanShiteiNo)) {
            return shiteiGassanSearchApiService.searchByGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        }
        if (StringUtils.hasText(name)) {
            return shiteiGassanSearchApiService.searchByName(jichitaiCd, name, nameMatchType);
        }
        if (StringUtils.hasText(shisetsuName)) {
            return shiteiGassanSearchApiService.searchByShisetsuName(jichitaiCd, shisetsuName, shisetsuNameMatchType);
        }
        if (StringUtils.hasText(kojinNo)) {
            return shiteiGassanSearchApiService.searchByKojinNo(jichitaiCd, kojinNo);
        }
        if (StringUtils.hasText(hojinNo)) {
            return shiteiGassanSearchApiService.searchByHojinNo(jichitaiCd, hojinNo);
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
}
