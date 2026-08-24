package jp.lg.asp.accommodation.controller;

import java.util.List;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.AddressDto;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.util.HashUtil;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressSearchApiController {

    private final AtenaRepository atenaRepository;
    private final GassanRepository gassanRepository;
    private final JichitaiContext jichitaiContext;
    private final HashUtil hashUtil;

    @GetMapping("/search")
    public List<AddressDto> search(
            @RequestParam(required = false) String addressNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "partial") String nameMatchType,
            @RequestParam(required = false) String address,
            @RequestParam(required = false, defaultValue = "partial") String addressMatchType,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String kojinNo,
            @RequestParam(required = false) String hojinNo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();

        if (!StringUtils.hasText(addressNumber) && !StringUtils.hasText(name) && !StringUtils.hasText(address)
                && !StringUtils.hasText(phone) && !StringUtils.hasText(kojinNo) && !StringUtils.hasText(hojinNo)) {
            return List.of();
        }

        return atenaRepository.search(
                jichitaiCd,
                StringUtils.hasText(addressNumber) ? addressNumber : "%",
                StringUtils.hasText(name)    ? toPattern(name, nameMatchType)       : "%",
                "%",
                "%",
                StringUtils.hasText(address) ? toPattern(address, addressMatchType) : "%",
                StringUtils.hasText(phone)   ? phone   : "%",
                String.format("%s", StringUtils.hasText(kojinNo) ? hashUtil.sha256(kojinNo) : "%"),
                StringUtils.hasText(hojinNo) ? hojinNo : "%"
        ).stream().map(a -> {
            String atenaNoStr = a.getAtenaNo().toPlainString();
            
            // 宛名がすでに合算申請に登録されているかチェック
            List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, a.getAtenaNo());
            boolean alreadyRegistered = !gassanList.isEmpty();
            String gassanShiteiNo = alreadyRegistered ? gassanList.get(0).getGassanShiteiNo() : null;

            return new AddressDto(
                    atenaNoStr,
                    a.getName(),
                    a.getNameKana(),
                    a.getYubinNo(),
                    a.getJusho(),
                    a.getTel1(),
                    a.getKojinNo(),
                    a.getHojinNo(),
                    alreadyRegistered,
                    gassanShiteiNo
            );
        }).toList();
    }

    @GetMapping("/search-or")
    public List<AddressDto> searchOr(
            @RequestParam(required = false) String addressNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "partial") String nameMatchType,
            @RequestParam(required = false) String address,
            @RequestParam(required = false, defaultValue = "partial") String addressMatchType,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String kojinNo,
            @RequestParam(required = false) String hojinNo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();

        if (!StringUtils.hasText(addressNumber) && !StringUtils.hasText(name) && !StringUtils.hasText(address)
                && !StringUtils.hasText(phone) && !StringUtils.hasText(kojinNo) && !StringUtils.hasText(hojinNo)) {
            return List.of();
        }

        return atenaRepository.searchOr(
                jichitaiCd,
                StringUtils.hasText(addressNumber) ? addressNumber : "",
                StringUtils.hasText(name)    ? toPattern(name, nameMatchType)       : "",
                StringUtils.hasText(address) ? toPattern(address, addressMatchType) : "",
                StringUtils.hasText(phone)   ? phone   : "",
                String.format("%s", StringUtils.hasText(kojinNo) ? hashUtil.sha256(kojinNo) : ""),
                StringUtils.hasText(hojinNo) ? hojinNo : ""
        ).stream().map(a -> {
            String atenaNoStr = a.getAtenaNo().toPlainString();
            
            return new AddressDto(
                    atenaNoStr,
                    a.getName(),
                    a.getNameKana(),
                    a.getYubinNo(),
                    a.getJusho(),
                    a.getTel1(),
                    "",
                    a.getHojinNo(),
                    false,
                    ""
            );
        }).toList();
    }

    private String toPattern(String value, String matchType) {
        return switch (matchType) {
            case "prefix"  -> value + "%";
            case "exact"   -> value;
            default        -> "%" + value + "%";
        };
    }
}