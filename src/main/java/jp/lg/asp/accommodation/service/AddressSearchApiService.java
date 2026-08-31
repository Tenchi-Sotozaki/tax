package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.AddressDto;

public interface AddressSearchApiService {

	/**
	 * アドレス検索
	 * @param jichitaiCd
	 * @param addressNumber
	 * @param name
	 * @param nameMatchType
	 * @param address
	 * @param addressMatchType
	 * @param phone
	 * @param kojinNo
	 * @param hojinNo
	 * @return 検索結果
	 */
	List<AddressDto> searchAddresses(
			String jichitaiCd,
			String addressNumber,
			String name,
			String nameMatchType,
			String address,
			String addressMatchType,
			String phone,
			String kojinNo,
			String hojinNo);

	/**
	 * アドレス検索（OR検索）
	 * @param jichitaiCd
	 * @param addressNumber
	 * @param name
	 * @param nameMatchType
	 * @param address
	 * @param addressMatchType
	 * @param phone
	 * @param kojinNo
	 * @param hojinNo
	 * @return 検索結果
	 */
	List<AddressDto> searchAddressesOr(
			String jichitaiCd,
			String addressNumber,
			String name,
			String nameMatchType,
			String address,
			String addressMatchType,
			String phone,
			String kojinNo,
			String hojinNo);
}
