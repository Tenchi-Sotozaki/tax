package jp.lg.asp.accommodation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.AddressDto;
import jp.lg.asp.accommodation.service.AddressSearchApiService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressSearchApiController {

    private final AddressSearchApiService addressSearchApiService;
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

        return addressSearchApiService.searchAddresses(
                jichitaiCd,
                addressNumber,
                name,
                nameMatchType,
                address,
                addressMatchType,
                phone,
                kojinNo,
                hojinNo
        );
    }
}