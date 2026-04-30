package jp.lg.asp.accommodation.controller;

import jp.lg.asp.accommodation.dto.AddressDto;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressSearchApiController {

    private final AtenaRepository atenaRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    @GetMapping("/search")
    public List<AddressDto> search(
            @RequestParam(required = false) String addressNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String kojinNo,
            @RequestParam(required = false) String hojinNo) {

        if (!StringUtils.hasText(addressNumber) && !StringUtils.hasText(name) && !StringUtils.hasText(address)
                && !StringUtils.hasText(phone) && !StringUtils.hasText(kojinNo) && !StringUtils.hasText(hojinNo)) {
            return List.of();
        }

        BigDecimal addressNumberDecimal = null;
        if (StringUtils.hasText(addressNumber)) {
            try {
                addressNumberDecimal = new BigDecimal(addressNumber);
            } catch (NumberFormatException e) {
                return List.of();
            }
        }

        return atenaRepository.searchByAnyField(
                jichitaiCd,
                addressNumberDecimal,
                StringUtils.hasText(name)    ? name    : null,
                StringUtils.hasText(address) ? address : null,
                StringUtils.hasText(phone)   ? phone   : null,
                StringUtils.hasText(kojinNo) ? kojinNo : null,
                StringUtils.hasText(hojinNo) ? hojinNo : null
        ).stream().map(a -> new AddressDto(
                a.getAtenaNo().toPlainString(),
                a.getName(),
                a.getNameKana(),
                a.getJusho(),
                a.getTel1(),
                a.getKojinNo(),
                a.getHojinNo()
        )).toList();
    }
}
