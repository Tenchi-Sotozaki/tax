package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.TokureiTekiyoForm;
import jp.lg.asp.accommodation.dto.TokureiTekiyoHistoryDto;
import jp.lg.asp.accommodation.entity.TekiyoNozeiShuki;
import jp.lg.asp.accommodation.repository.TekiyoNozeiShukiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.TokureiTekiyoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokureiTekiyoServiceImpl implements TokureiTekiyoService {

    private final TekiyoNozeiShukiRepository tekiyoNozeiShukiRepository;
    private final TokugimuRepository tokugimuRepository;
    private final JichitaiContext jichitaiContext;

    private static final String FLG_ON = "1";
    private static final String FLG_OFF = "0";
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DISP_FMT = DateTimeFormatter.ofPattern("yyyy年MM月");

    @Override
    @Transactional(readOnly = true)
    public List<TokureiTekiyoHistoryDto> getHistories(String shiteiNo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        return tekiyoNozeiShukiRepository
                .findActiveByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream()
                .map(t -> new TokureiTekiyoHistoryDto(
                        t.getRno(),
                        t.getTekiyoStYmd() != null ? t.getTekiyoStYmd().format(DISP_FMT) : "",
                        t.getTekiyoEdYmd() != null ? t.getTekiyoEdYmd().format(DISP_FMT) : ""))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TokureiTekiyoForm getForView(String shiteiNo, Integer rno) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        TekiyoNozeiShuki record = tekiyoNozeiShukiRepository
                .findActiveByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream()
                .filter(t -> t.getRno().equals(rno))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("指定されたレコードが見つかりません。"));

        TokureiTekiyoForm form = buildBaseForm(shiteiNo, jichitaiCd);
        form.setRno(record.getRno());
        if (record.getTekiyoStYmd() != null) {
            form.setTekiyoStMonth(record.getTekiyoStYmd().format(MONTH_FMT));
        }
        if (record.getTekiyoEdYmd() != null) {
            form.setTekiyoEdMonth(record.getTekiyoEdYmd().format(MONTH_FMT));
        }
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public TokureiTekiyoForm getForRegister(String shiteiNo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        return buildBaseForm(shiteiNo, jichitaiCd);
    }

    @Override
    @Transactional
    public void save(String shiteiNo, TokureiTekiyoForm form) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        LocalDate stYmd = toFirstDay(form.getTekiyoStMonth());
        LocalDate edYmd = toLastDay(form.getTekiyoEdMonth());

        validate(stYmd, edYmd);
        checkOverlap(jichitaiCd, shiteiNo, stYmd, edYmd, null);

        int nextRno = tekiyoNozeiShukiRepository
                .findActiveByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo).size() + 1;

        TekiyoNozeiShuki entity = new TekiyoNozeiShuki();
        entity.setJichitaiCd(jichitaiCd);
        entity.setShiteiNo(shiteiNo);
        entity.setRno(nextRno);
        entity.setTekiyoStYmd(stYmd);
        entity.setTekiyoEdYmd(edYmd);
        entity.setDelFlg(FLG_OFF);
        tekiyoNozeiShukiRepository.save(entity);
        log.debug("特例適用登録完了: shiteiNo={}, rno={}", shiteiNo, nextRno);
    }

    @Override
    @Transactional
    public void update(String shiteiNo, Integer rno, TokureiTekiyoForm form) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        LocalDate stYmd = toFirstDay(form.getTekiyoStMonth());
        LocalDate edYmd = toLastDay(form.getTekiyoEdMonth());

        validate(stYmd, edYmd);
        checkOverlap(jichitaiCd, shiteiNo, stYmd, edYmd, rno);

        TekiyoNozeiShuki entity = tekiyoNozeiShukiRepository
                .findActiveByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream()
                .filter(t -> t.getRno().equals(rno))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("更新対象のレコードが見つかりません。"));

        entity.setTekiyoStYmd(stYmd);
        entity.setTekiyoEdYmd(edYmd);
        tekiyoNozeiShukiRepository.save(entity);
        log.debug("特例適用更新完了: shiteiNo={}, rno={}", shiteiNo, rno);
    }

    @Override
    @Transactional
    public void delete(String shiteiNo, Integer rno) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        TekiyoNozeiShuki entity = tekiyoNozeiShukiRepository
                .findActiveByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream()
                .filter(t -> t.getRno().equals(rno))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("削除対象のレコードが見つかりません。"));

        entity.setDelFlg(FLG_ON);
        tekiyoNozeiShukiRepository.save(entity);
        log.debug("特例適用削除完了: shiteiNo={}, rno={}", shiteiNo, rno);
    }

    // ========== private ==========

    private TokureiTekiyoForm buildBaseForm(String shiteiNo, String jichitaiCd) {
        TokureiTekiyoForm form = new TokureiTekiyoForm();
        form.setShiteiNo(shiteiNo);
        tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream().findFirst().ifPresent(t -> {
                    form.setObligorName(t.getKyokaName());
                    form.setFacilityName(t.getShisetsuName());
                });
        return form;
    }

    private void validate(LocalDate stYmd, LocalDate edYmd) {
        if (stYmd == null) {
            throw new IllegalStateException("適用開始年月は必須です。");
        }
        if (edYmd != null && stYmd.isAfter(edYmd)) {
            throw new IllegalStateException("適用開始年月が適用終了年月より後になっています。");
        }
    }

    private void checkOverlap(String jichitaiCd, String shiteiNo,
            LocalDate newSt, LocalDate newEd, Integer excludeRno) {
        tekiyoNozeiShukiRepository
                .findActiveByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream()
                .filter(t -> excludeRno == null || !t.getRno().equals(excludeRno))
                .forEach(t -> {
                    LocalDate exSt = t.getTekiyoStYmd();
                    LocalDate exEd = t.getTekiyoEdYmd();
                    boolean overlaps = (exEd == null || !exEd.isBefore(newSt))
                            && (newEd == null || exSt == null || !exSt.isAfter(newEd));
                    if (overlaps) {
                        throw new IllegalStateException("適用期間が既存のレコードと重複しています。");
                    }
                });
    }

    private LocalDate toFirstDay(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) return null;
        return YearMonth.parse(yearMonth).atDay(1);
    }

    private LocalDate toLastDay(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) return null;
        return YearMonth.parse(yearMonth).atEndOfMonth();
    }
}
