package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.constant.FukaConstants;
import jp.lg.asp.accommodation.constant.ZeiritsuConstants;
import jp.lg.asp.accommodation.dto.ZeiritsuDetailForm;
import jp.lg.asp.accommodation.dto.ZeiritsuForm;
import jp.lg.asp.accommodation.dto.ZeiritsuListItem;
import jp.lg.asp.accommodation.dto.ZeiritsuSearchForm;
import jp.lg.asp.accommodation.entity.Zeiritsu;
import jp.lg.asp.accommodation.entity.ZeiritsuId;
import jp.lg.asp.accommodation.entity.ZeiritsuTeigaku;
import jp.lg.asp.accommodation.entity.ZeiritsuTeigakuId;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsu;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsuId;
import jp.lg.asp.accommodation.exception.ResourceNotFoundException;
import jp.lg.asp.accommodation.repository.ZeiritsuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.ZeiritsuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZeiritsuServiceImpl implements ZeiritsuService {

    private final ZeiritsuRepository zeiritsuRepository;
    private final ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
    private final ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ZeiritsuListItem> search(String jichitaiCd, ZeiritsuSearchForm form) {
        return zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd).stream()
                .filter(z -> {
                    if (form.getFukaKbn() != null && !form.getFukaKbn().isBlank()
                            && !form.getFukaKbn().equals(z.getFukaKbn()))
                        return false;
                    if (form.getTaishoKbn() != null && !form.getTaishoKbn().isBlank()
                            && !form.getTaishoKbn().equals(z.getTaishoKbn()))
                        return false;
                    if (form.getTekiyoYmFrom() != null && !form.getTekiyoYmFrom().isBlank()) {
                        String from = form.getTekiyoYmFrom().replace("-", "");
                        if (z.getTekiyoEdYm() != null && !z.getTekiyoEdYm().isBlank()
                                && z.getTekiyoEdYm().compareTo(from) < 0)
                            return false;
                    }
                    if (form.getTekiyoYmTo() != null && !form.getTekiyoYmTo().isBlank()) {
                        String to = form.getTekiyoYmTo().replace("-", "");
                        if (z.getTekiyoStYm().compareTo(to) > 0)
                            return false;
                    }
                    return true;
                })
                .map(z -> new ZeiritsuListItem(
                        z.getSeq(),
                        z.getFukaKbn(),
                        FukaConstants.TEIGAKU.getValue().equals(z.getFukaKbn())
                                ? FukaConstants.TEIGAKU.getName()
                                : FukaConstants.TEIRITSU.getName(),
                        z.getTekiyoStYm(),
                        z.getTekiyoEdYm(),
                        z.getTaishoKbn(),
                        ZeiritsuConstants.CITY.getValue().equals(z.getTaishoKbn())
                                ? ZeiritsuConstants.CITY.getName()
                                : ZeiritsuConstants.KEN.getName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Zeiritsu findOrThrow(String jichitaiCd, BigDecimal seq) {
        return zeiritsuRepository.findById(new ZeiritsuId(jichitaiCd, seq))
                .orElseThrow(() -> new ResourceNotFoundException("税率管理データが見つかりません"));
    }

    @Override
    @Transactional(readOnly = true)
    public ZeiritsuForm toForm(Zeiritsu z, String jichitaiCd) {
        ZeiritsuForm form = new ZeiritsuForm();
        form.setFukaKbn(z.getFukaKbn());
        form.setTekiyoStYm(z.getTekiyoStYm());
        form.setTekiyoEdYm(z.getTekiyoEdYm());
        form.setTaishoKbn(z.getTaishoKbn());

        boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(z.getFukaKbn());
        if (isTeigaku) {
            List<ZeiritsuTeigaku> details = zeiritsuTeigakuRepository.findActiveBySeq(jichitaiCd, z.getSeq());
            for (int i = 0; i < details.size() && i < 5; i++) {
                ZeiritsuTeigaku d = details.get(i);
                ZeiritsuDetailForm df = form.getDetails().get(i);
                df.setZeiValue(d.getZeigaku() != null ? d.getZeigaku().toString() : null);
                df.setRyokinSt(d.getRyokinSt() != null ? d.getRyokinSt().toString() : null);
                df.setRyokinEd(d.getRyokinEd() != null ? d.getRyokinEd().toString() : null);
            }
        } else {
            List<ZeiritsuTeiritsu> details = zeiritsuTeiritsuRepository.findActiveBySeq(jichitaiCd, z.getSeq());
            for (int i = 0; i < details.size() && i < 5; i++) {
                ZeiritsuTeiritsu d = details.get(i);
                ZeiritsuDetailForm df = form.getDetails().get(i);
                df.setZeiValue(d.getZeiRitsu() != null ? d.getZeiRitsu().toPlainString() : null);
                df.setKbnName(d.getKbnName());
            }
        }
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLatestRecord(String jichitaiCd, Zeiritsu z) {
        BigDecimal maxSeq = zeiritsuRepository.findMaxSeqByJichitaiCdAndTaishoKbn(jichitaiCd, z.getTaishoKbn());
        return maxSeq != null && maxSeq.compareTo(z.getSeq()) == 0;
    }

    @Override
    public boolean isFutureStartYm(String tekiyoStYm) {
        if (tekiyoStYm == null || tekiyoStYm.length() != 6) return false;
        java.time.YearMonth current = java.time.YearMonth.now();
        java.time.YearMonth startYm = java.time.YearMonth.of(
                Integer.parseInt(tekiyoStYm.substring(0, 4)),
                Integer.parseInt(tekiyoStYm.substring(4, 6)));
        return startYm.isAfter(current);
    }

    @Override
    @Transactional
    public void save(String jichitaiCd, ZeiritsuForm form) {
        autoUpdateExistingPeriod(form, jichitaiCd, null);

        BigDecimal nextSeq = zeiritsuRepository.findAll().stream()
                .filter(z -> z.getJichitaiCd().equals(jichitaiCd))
                .map(Zeiritsu::getSeq)
                .max(BigDecimal::compareTo)
                .map(s -> s.add(BigDecimal.ONE))
                .orElse(BigDecimal.ONE);

        String tekiyoStYm = form.getTekiyoStYm().replace("-", "");

        Zeiritsu entity = new Zeiritsu();
        entity.setJichitaiCd(jichitaiCd);
        entity.setSeq(nextSeq);
        entity.setFukaKbn(form.getFukaKbn());
        entity.setTekiyoStYm(tekiyoStYm);
        String tekiyoEdYm = form.getTekiyoEdYm();
        entity.setTekiyoEdYm(tekiyoEdYm != null && !tekiyoEdYm.isBlank() ? tekiyoEdYm.replace("-", "") : null);
        entity.setTaishoKbn(form.getTaishoKbn());
        entity.setDelFlg("0");
        zeiritsuRepository.save(entity);

        saveDetails(jichitaiCd, nextSeq, form, true);
        log.debug("税率管理マスタを登録しました。jichitaiCd: {}, seq: {}", jichitaiCd, nextSeq);
    }

    @Override
    @Transactional
    public void update(String jichitaiCd, BigDecimal seq, ZeiritsuForm form, boolean detailEditable) {
        Zeiritsu entity = findOrThrow(jichitaiCd, seq);
        boolean latest = isLatestRecord(jichitaiCd, entity);

        String tekiyoStYm = form.getTekiyoStYm().replace("-", "");

        if (latest) {
            autoUpdateExistingPeriod(form, jichitaiCd, seq);
        }

        entity.setFukaKbn(form.getFukaKbn());
        entity.setTekiyoStYm(tekiyoStYm);
        String tekiyoEdYm = form.getTekiyoEdYm();
        entity.setTekiyoEdYm(tekiyoEdYm != null && !tekiyoEdYm.isBlank() ? tekiyoEdYm.replace("-", "") : null);
        entity.setTaishoKbn(form.getTaishoKbn());
        zeiritsuRepository.save(entity);

        if (detailEditable) {
            boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(form.getFukaKbn());
            if (isTeigaku) {
                zeiritsuTeigakuRepository.findActiveBySeq(jichitaiCd, seq).forEach(d -> {
                    d.setDelFlg("1");
                    zeiritsuTeigakuRepository.save(d);
                });
            } else {
                zeiritsuTeiritsuRepository.findActiveBySeq(jichitaiCd, seq).forEach(d -> {
                    d.setDelFlg("1");
                    zeiritsuTeiritsuRepository.save(d);
                });
            }
            saveDetails(jichitaiCd, seq, form, false);
        }
        log.debug("税率管理マスタを更新しました。jichitaiCd: {}, seq: {}", jichitaiCd, seq);
    }

    @Override
    @Transactional
    public void delete(String jichitaiCd, BigDecimal seq) {
        Zeiritsu entity = findOrThrow(jichitaiCd, seq);
        entity.setDelFlg("1");
        zeiritsuRepository.save(entity);

        boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(entity.getFukaKbn());
        if (isTeigaku) {
            zeiritsuTeigakuRepository.findActiveBySeq(jichitaiCd, seq).forEach(d -> {
                d.setDelFlg("1");
                zeiritsuTeigakuRepository.save(d);
            });
        } else {
            zeiritsuTeiritsuRepository.findActiveBySeq(jichitaiCd, seq).forEach(d -> {
                d.setDelFlg("1");
                zeiritsuTeiritsuRepository.save(d);
            });
        }
        log.debug("税率管理マスタを削除しました。jichitaiCd: {}, seq: {}", jichitaiCd, seq);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Zeiritsu> findActiveByJichitaiCd(String jichitaiCd) {
        return zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Zeiritsu> findActiveByJichitaiCdAndTaishoKbn(String jichitaiCd, String taishoKbn) {
        return zeiritsuRepository.findActiveByJichitaiCdAndTaishoKbn(jichitaiCd, taishoKbn);
    }

    @Override
    @Transactional(readOnly = true)
    public Zeiritsu findAutoUpdateTarget(ZeiritsuForm form, String jichitaiCd, BigDecimal excludeSeq) {
        String taishoKbn = form.getTaishoKbn();
        String tekiyoStYm = form.getTekiyoStYm().replace("-", "");
        return zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd).stream()
                .filter(z -> z.getTaishoKbn().equals(taishoKbn))
                .filter(z -> !"1".equals(z.getDelFlg()))
                .filter(z -> excludeSeq == null || !z.getSeq().equals(excludeSeq))
                .filter(z -> z.getTekiyoEdYm() == null || z.getTekiyoEdYm().isBlank())
                .filter(z -> tekiyoStYm.compareTo(z.getTekiyoStYm()) >= 0)
                .findFirst().orElse(null);
    }

    @Override
    public String formatYm(String ym) {
        if (ym == null || ym.length() != 6) return ym;
        return ym.substring(0, 4) + "年" + ym.substring(4, 6) + "月";
    }

    @Override
    public String getPreviousMonth(String ym) {
        int year = Integer.parseInt(ym.substring(0, 4));
        int month = Integer.parseInt(ym.substring(4, 6));
        if (month == 1) { year--; month = 12; } else { month--; }
        return String.format("%04d%02d", year, month);
    }

    private void autoUpdateExistingPeriod(ZeiritsuForm form, String jichitaiCd, BigDecimal excludeSeq) {
        String taishoKbn = form.getTaishoKbn();
        String tekiyoStYm = form.getTekiyoStYm().replace("-", "");

        zeiritsuRepository.findActiveByJichitaiCd(jichitaiCd).stream()
                .filter(z -> z.getTaishoKbn().equals(taishoKbn))
                .filter(z -> !"1".equals(z.getDelFlg()))
                .filter(z -> z.getTekiyoEdYm() == null || z.getTekiyoEdYm().isBlank())
                .filter(z -> excludeSeq == null || !z.getSeq().equals(excludeSeq))
                .filter(z -> z.getTekiyoStYm().compareTo(tekiyoStYm) < 0)
                .forEach(existing -> {
                    String newEdYm = getPreviousMonth(tekiyoStYm);
                    existing.setTekiyoEdYm(newEdYm);
                    zeiritsuRepository.save(existing);
                });
    }

    private void saveDetails(String jichitaiCd, BigDecimal seq, ZeiritsuForm form, boolean isNew) {
        boolean isTeigaku = FukaConstants.TEIGAKU.getValue().equals(form.getFukaKbn());
        int detailSeq = 1;
        for (ZeiritsuDetailForm detail : form.getDetails()) {
            if (detail.getZeiValue() == null || detail.getZeiValue().isBlank()) continue;
            if (isTeigaku) {
                ZeiritsuTeigaku d = isNew ? new ZeiritsuTeigaku()
                        : zeiritsuTeigakuRepository
                                .findById(new ZeiritsuTeigakuId(jichitaiCd, seq, BigDecimal.valueOf(detailSeq)))
                                .orElse(new ZeiritsuTeigaku());
                d.setJichitaiCd(jichitaiCd);
                d.setSeq(seq);
                d.setTeigakuSeq(BigDecimal.valueOf(detailSeq));
                d.setZeigaku(parseLong(detail.getZeiValue()));
                d.setRyokinSt(parseLong(detail.getRyokinSt()));
                d.setRyokinEd(parseLong(detail.getRyokinEd()));
                d.setDelFlg("0");
                zeiritsuTeigakuRepository.save(d);
            } else {
                ZeiritsuTeiritsu d = isNew ? new ZeiritsuTeiritsu()
                        : zeiritsuTeiritsuRepository
                                .findById(new ZeiritsuTeiritsuId(jichitaiCd, seq, BigDecimal.valueOf(detailSeq)))
                                .orElse(new ZeiritsuTeiritsu());
                d.setJichitaiCd(jichitaiCd);
                d.setSeq(seq);
                d.setTeiritsuSeq(BigDecimal.valueOf(detailSeq));
                d.setZeiRitsu(new BigDecimal(detail.getZeiValue()));
                d.setKbnName(detail.getKbnName());
                d.setDelFlg("0");
                zeiritsuTeiritsuRepository.save(d);
            }
            detailSeq++;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        return new BigDecimal(value.trim()).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
    }
}
