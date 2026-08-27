package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;
import java.util.List;

import jp.lg.asp.accommodation.dto.ZeiritsuForm;
import jp.lg.asp.accommodation.dto.ZeiritsuListItem;
import jp.lg.asp.accommodation.dto.ZeiritsuSearchForm;
import jp.lg.asp.accommodation.entity.Zeiritsu;

public interface ZeiritsuService {

    List<ZeiritsuListItem> search(String jichitaiCd, ZeiritsuSearchForm form);

    Zeiritsu findOrThrow(String jichitaiCd, BigDecimal seq);

    ZeiritsuForm toForm(Zeiritsu z, String jichitaiCd);

    boolean isLatestRecord(String jichitaiCd, Zeiritsu z);

    boolean isFutureStartYm(String tekiyoStYm);

    void save(String jichitaiCd, ZeiritsuForm form);

    void update(String jichitaiCd, BigDecimal seq, ZeiritsuForm form, boolean detailEditable);

    void delete(String jichitaiCd, BigDecimal seq);

    Zeiritsu findAutoUpdateTarget(ZeiritsuForm form, String jichitaiCd, BigDecimal excludeSeq);

    List<Zeiritsu> findActiveByJichitaiCd(String jichitaiCd);

    List<Zeiritsu> findActiveByJichitaiCdAndTaishoKbn(String jichitaiCd, String taishoKbn);

    String formatYm(String ym);

    String getPreviousMonth(String ym);
}
