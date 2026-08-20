package jp.lg.asp.accommodation.service;

import java.util.Optional;

import jp.lg.asp.accommodation.entity.Nokan;

public interface NokanService {

	/**
	 * 指定した納税管理人情報を取得
	 */
	Optional<Nokan> findByJichitaiCdAndShiteiNo(String ShiteiNo);
}
