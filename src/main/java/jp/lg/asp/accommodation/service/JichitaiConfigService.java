package jp.lg.asp.accommodation.service;

import java.util.Optional;

import jp.lg.asp.accommodation.dto.JichitaiConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;

public interface JichitaiConfigService {

    Optional<Jichitai> findById(String jichitaiCd);

    void save(String currentJichitaiCd, JichitaiConfigDto configForm);

	/**
	 * 現在の自治体情報を取得
	 */
	Jichitai getCurrentJichitai();

	/**
	 * 初期表示用の設定DTOを取得
	 */
	JichitaiConfigDto getJichitaiConfigDto();

	/**
	 * 自治体情報を保存
	 */
	void saveJichitaiConfig(JichitaiConfigDto configForm);
}