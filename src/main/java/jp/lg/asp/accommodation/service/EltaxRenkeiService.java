package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;
import java.util.List;

import jp.lg.asp.accommodation.dto.EltaxRenkeiDto;
import jp.lg.asp.accommodation.entity.EltaxRenkei;

public interface EltaxRenkeiService {

	/** 取込済みファイル一覧を取得する */
	List<EltaxRenkeiDto> findAll();

	/** SEQを指定してログをダウンロード用に取得する */
	EltaxRenkei findBySeq(BigDecimal seq);
}
