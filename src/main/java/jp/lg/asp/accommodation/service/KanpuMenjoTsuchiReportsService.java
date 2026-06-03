package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.KanpuMenjoTsuchiDto;

/**
 * 徴収不能額の還付又は納入義務の免除決定通知書PDF生成 Service
 */
public interface KanpuMenjoTsuchiReportsService {

    /**
     * 通知書PDF生成
     */
    byte[] generateTsuchiPdf(KanpuMenjoTsuchiDto dto);
}