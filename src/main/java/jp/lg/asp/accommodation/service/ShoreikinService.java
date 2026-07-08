package jp.lg.asp.accommodation.service;

import org.springframework.data.domain.Page;

import jp.lg.asp.accommodation.dto.ShoreikinDto;

/**
 * 特別徴収事務交付金 Service インターフェース。
 */
public interface ShoreikinService {

	/** 検索条件に合致する一覧を返す */
	Page<ShoreikinDto> search(ShoreikinDto form);
}
