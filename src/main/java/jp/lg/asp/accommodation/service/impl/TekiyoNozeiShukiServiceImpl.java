package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.NozeiShukiDto;
import jp.lg.asp.accommodation.dto.TekiyoNozeiShukiForm;
import jp.lg.asp.accommodation.dto.TekiyoNozeiShukiHistoryDto;
import jp.lg.asp.accommodation.entity.TekiyoNozeiShuki;
import jp.lg.asp.accommodation.repository.TekiyoNozeiShukiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.TekiyoNozeiShukiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TekiyoNozeiShukiServiceImpl implements TekiyoNozeiShukiService {

    private final TekiyoNozeiShukiRepository tekiyoNozeiShukiRepository;
    private final TokugimuRepository tokugimuRepository;
    private final TekiyoNozeiShukiRepository nozeiShukiRepository;

    private final JichitaiContext jichitaiContext;

    private static final String FLG_ON = "1";
    private static final String FLG_OFF = "0";

    @Override
    public List<NozeiShukiDto> getNozeiShukiOptions() {
        return List.of(
            new NozeiShukiDto(BigDecimal.ONE, BigDecimal.ONE),
            new NozeiShukiDto(BigDecimal.valueOf(3), BigDecimal.valueOf(3))
        );
    }


    @Override
    @Transactional(readOnly = true)
    public TekiyoNozeiShukiForm getByShiteiNo(String shiteiNo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        TekiyoNozeiShukiForm form = new TekiyoNozeiShukiForm();
        form.setShiteiNo(shiteiNo);

        tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream().findFirst().ifPresent(t -> {
                    form.setObligorName(t.getKyokaName());
                    form.setFacilityName(t.getShisetsuName());
                });

        List<TekiyoNozeiShuki> allRecords = tekiyoNozeiShukiRepository
                .findActiveByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);

        if (!allRecords.isEmpty()) {
            TekiyoNozeiShuki latest = allRecords.get(0);
            form.setEdit(true);
            if (latest.getTekiyoStYmd() != null) {
                form.setTekiyoStMonth(latest.getTekiyoStYmd().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            }
            if (latest.getTekiyoEdYmd() != null) {
                form.setTekiyoEdMonth(latest.getTekiyoEdYmd().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            }

            form.setHistories(allRecords.stream()
                    .map(t -> new TekiyoNozeiShukiHistoryDto(
                            t.getTekiyoStYmd() != null ? t.getTekiyoStYmd().format(DateTimeFormatter.ofPattern("yyyy年MM月")) : "",
                            t.getTekiyoEdYmd() != null ? t.getTekiyoEdYmd().format(DateTimeFormatter.ofPattern("yyyy年MM月")) : ""))
                    .toList());
        }

        return form;
    }

    private LocalDate toFirstDay(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) return null;
        return YearMonth.parse(yearMonth).atDay(1);
    }

    private LocalDate toLastDay(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) return null;
        return YearMonth.parse(yearMonth).atEndOfMonth();
    }

    @Override
    @Transactional
    public void save(String shiteiNo, TekiyoNozeiShukiForm form) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        LocalDate stYmd = toFirstDay(form.getTekiyoStMonth());
        LocalDate edYmd = toLastDay(form.getTekiyoEdMonth());

        if (edYmd != null && stYmd.isAfter(edYmd)) {
            throw new IllegalStateException("適用開始年月が適用終了年月より後になっています。");
        }
        checkAndResolveOverlap(shiteiNo, stYmd, edYmd);

        TekiyoNozeiShuki entity = new TekiyoNozeiShuki();
        entity.setJichitaiCd(jichitaiCd);
        entity.setShiteiNo(shiteiNo);
        entity.setTekiyoStYmd(stYmd);
        entity.setTekiyoEdYmd(edYmd);
        entity.setDelFlg(FLG_OFF);

        tekiyoNozeiShukiRepository.save(entity);
        log.debug("適用納税周期保存完了: shiteiNo={}", shiteiNo);
    }

    private void checkAndResolveOverlap(String shiteiNo, LocalDate newStYmd, LocalDate newEdYmd) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();

        List<TekiyoNozeiShuki> existingRecords = tekiyoNozeiShukiRepository
                .findActiveByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);

        for (TekiyoNozeiShuki existing : existingRecords) {
            LocalDate exStYmd = existing.getTekiyoStYmd();
            LocalDate exEdYmd = existing.getTekiyoEdYmd();

            if (exEdYmd == null) {
                // 適用終了年月日が未登録の場合、新規開始日の前日を設定
                if (exStYmd == null || !exStYmd.isAfter(newStYmd.minusDays(1))) {
                    existing.setTekiyoEdYmd(newStYmd.minusDays(1));
                    tekiyoNozeiShukiRepository.save(existing);
                }
            } else {
                // 期間重複チェック
                boolean overlaps = !exEdYmd.isBefore(newStYmd)
                        && (newEdYmd == null || !exStYmd.isAfter(newEdYmd));
                if (overlaps) {
                    throw new IllegalStateException("適用期間が既存のレコードと重複しています。");
                }
            }
        }
    }

    @Override
    @Transactional
    public void delete(String shiteiNo) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        TekiyoNozeiShuki latest = tekiyoNozeiShukiRepository
                .findActiveByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("削除対象のレコードが見つかりません。"));

        latest.setDelFlg(FLG_ON);
        tekiyoNozeiShukiRepository.save(latest);

        log.debug("適用納税周期削除完了: shiteiNo={}", shiteiNo);
    }
}
