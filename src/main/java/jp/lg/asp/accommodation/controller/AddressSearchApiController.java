package jp.lg.asp.accommodation.controller;
import jp.lg.asp.accommodation.config.JichitaiContext;

import jp.lg.asp.accommodation.dto.AddressDto;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressSearchApiController {

    private final AtenaRepository atenaRepository;

    private final JichitaiContext jichitaiContext;

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
                StringUtils.hasText(kojinNo) ? kojinNo : "%",
                StringUtils.hasText(hojinNo) ? hojinNo : "%"
        ).stream().map(a -> new AddressDto(
                a.getAtenaNo().toPlainString(),
                a.getName(),
                a.getNameKana(),
                a.getYubinNo(),
                a.getJusho(),
                a.getTel1(),
                a.getKojinNo(),
                a.getHojinNo()
        )).toList();
    }

    private String toPattern(String value, String matchType) {
        return switch (matchType) {
            case "prefix"  -> value + "%";
            case "exact"   -> value;
            default        -> "%" + value + "%";
        };
    }
}
