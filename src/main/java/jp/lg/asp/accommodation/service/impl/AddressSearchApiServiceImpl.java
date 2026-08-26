package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jp.lg.asp.accommodation.dto.AddressDto;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.service.AddressSearchApiService;
import jp.lg.asp.accommodation.util.HashUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddressSearchApiServiceImpl implements AddressSearchApiService {

	private final AtenaRepository atenaRepository;
	private final GassanRepository gassanRepository;
	private final HashUtil hashUtil;

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
				StringUtils.hasText(name) ? toPattern(name, nameMatchType) : "%",
				"%",
				"%",
				StringUtils.hasText(address) ? toPattern(address, addressMatchType) : "%",
				StringUtils.hasText(phone) ? phone : "%",
				String.format("%s", StringUtils.hasText(kojinNo) ? hashUtil.sha256(kojinNo) : "%"),
				StringUtils.hasText(hojinNo) ? hojinNo : "%").stream().map(a -> {
					String atenaNoStr = a.getAtenaNo().toPlainString();

					// 宛名がすでに合算申請に登録されているかチェック（論理削除済みを除く全履歴対象）
					List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, a.getAtenaNo());
					// 有効（適用中または未来終了）の合算を優先、なければ過去履歴の最新を取得
					Optional<Gassan> activeGassan = gassanList.stream()
							.filter(g -> g.getTekiyoEdYmd() == null || !g.getTekiyoEdYmd().isBefore(LocalDate.now()))
							.findFirst();
					Optional<Gassan> targetGassan = activeGassan.isPresent() ? activeGassan
							: gassanList.stream().findFirst();
					boolean alreadyRegistered = targetGassan.isPresent();
					String gassanShiteiNo = null;
					String tekiyoEdYmd = null;
					if (alreadyRegistered) {
						Gassan g = targetGassan.get();
						gassanShiteiNo = g.getGassanShiteiNo();
						tekiyoEdYmd = g.getTekiyoEdYmd() != null ? g.getTekiyoEdYmd().toString() : null;
					}

					return AddressDto.builder()
							.addressNumber(atenaNoStr)
							.name(a.getName())
							.nameKana(a.getNameKana())
							.yubinNo(a.getYubinNo())
							.address(a.getJusho())
							.phone(a.getTel1())
							.kojinNo(a.getKojinNo())
							.hojinNo(a.getHojinNo())
							.alreadyRegistered(alreadyRegistered)
							.gassanShiteiNo(gassanShiteiNo)
							.tekiyoEdYmd(tekiyoEdYmd)
							.build();
				}).toList();
	}

	@Override
	public List<AddressDto> searchAddressesOr(
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

		return atenaRepository.searchOr(
				jichitaiCd,
				StringUtils.hasText(addressNumber) ? addressNumber : "",
				StringUtils.hasText(name) ? toPattern(name, nameMatchType) : "",
				StringUtils.hasText(address) ? toPattern(address, addressMatchType) : "",
				StringUtils.hasText(phone) ? phone : "",
				String.format("%s", StringUtils.hasText(kojinNo) ? hashUtil.sha256(kojinNo) : ""),
				StringUtils.hasText(hojinNo) ? hojinNo : "").stream().map(a -> {
					String atenaNoStr = a.getAtenaNo().toPlainString();

					return AddressDto.builder()
							.addressNumber(atenaNoStr)
							.name(a.getName())
							.nameKana(a.getNameKana())
							.yubinNo(a.getYubinNo())
							.address(a.getJusho())
							.phone(a.getTel1())
							.kojinNo(a.getKojinNo())
							.hojinNo(a.getHojinNo())
							.build();
				}).toList();
	}

	private String toPattern(String value, String matchType) {
		return switch (matchType) {
		case "prefix" -> value + "%";
		case "exact" -> value;
		default -> "%" + value + "%";
		};
	}
}
