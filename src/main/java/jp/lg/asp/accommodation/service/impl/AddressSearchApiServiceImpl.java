package jp.lg.asp.accommodation.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jp.lg.asp.accommodation.dto.AddressDto;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.service.AddressSearchApiService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddressSearchApiServiceImpl implements AddressSearchApiService{
	
	private final AtenaRepository atenaRepository;
    private final GassanRepository gassanRepository;

    @Override
    public List<AddressDto> searchAddresses(
            String jichitaiCd,
            String addressNumber,
            String name,
            String nameMatchType,
            String address,
            String addressMatchType,
            String phone,
            String kojinNo,
            String hojinNo) {

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
                String.format("%s", StringUtils.hasText(kojinNo) ? kojinNo : "%"),
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

    private String toPattern(String value, String matchType) {
        return switch (matchType) {
            case "prefix"  -> value + "%";
            case "exact"   -> value;
            default        -> "%" + value + "%";
        };
    }
}
