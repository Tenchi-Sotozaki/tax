package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.NozeiShukiDto;
import jp.lg.asp.accommodation.dto.TekiyoNozeiShukiForm;
import jp.lg.asp.accommodation.entity.TekiyoNozeiShuki;
import jp.lg.asp.accommodation.repository.NozeiShukiRepository;
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
    private final NozeiShukiRepository nozeiShukiRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    private static final String FLG_ON = "1";
    private static final String FLG_OFF = "0";

    @Override
    public List<NozeiShukiDto> getNozeiShukiOptions() {
        return nozeiShukiRepository.findActiveByJichitaiCd(jichitaiCd)
                .stream()
                .map(n -> new NozeiShukiDto(n.getSeq(), n.getShuki()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TekiyoNozeiShukiForm getByShiteiNo(String shiteiNo) {
        TekiyoNozeiShukiForm form = new TekiyoNozeiShukiForm();
        form.setShiteiNo(shiteiNo);

        tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream().findFirst().ifPresent(t -> {
                    form.setObligorName(t.getKyokaName());
                    form.setFacilityName(t.getShisetsuName());
                });

        tekiyoNozeiShukiRepository.findLatestByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream().findFirst().ifPresent(t -> {
                    form.setEdit(true);
                    form.setSeq(t.getSeq());
                    form.setTekiyoStYmd(t.getTekiyoStYmd());
                    form.setTekiyoEdYmd(t.getTekiyoEdYmd());
                });

        return form;
    }

    @Override
    @Transactional
    public void save(String shiteiNo, TekiyoNozeiShukiForm form) {
        if (form.getTekiyoEdYmd() != null && form.getTekiyoStYmd().isAfter(form.getTekiyoEdYmd())) {
            throw new IllegalStateException("適用開始年月日が適用終了年月日より後になっています。");
        }
        checkAndResolveOverlap(shiteiNo, form);

        Integer maxIdx = tekiyoNozeiShukiRepository.findMaxIdxByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        Integer newIdx = maxIdx + 1;

        TekiyoNozeiShuki entity = new TekiyoNozeiShuki();
        entity.setJichitaiCd(jichitaiCd);
        entity.setShiteiNo(shiteiNo);
        entity.setIdx(newIdx);
        entity.setSeq(form.getSeq());
        entity.setTekiyoStYmd(form.getTekiyoStYmd());
        entity.setTekiyoEdYmd(form.getTekiyoEdYmd());
        entity.setDelFlg(FLG_OFF);

        tekiyoNozeiShukiRepository.save(entity);
        log.info("適用納税周期保存完了: shiteiNo={}, idx={}", shiteiNo, newIdx);
    }

    private void checkAndResolveOverlap(String shiteiNo, TekiyoNozeiShukiForm form) {
        LocalDate newStYmd = form.getTekiyoStYmd();
        LocalDate newEdYmd = form.getTekiyoEdYmd();

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
        TekiyoNozeiShuki latest = tekiyoNozeiShukiRepository
                .findLatestByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("削除対象のレコードが見つかりません。"));

        latest.setDelFlg(FLG_ON);
        tekiyoNozeiShukiRepository.save(latest);

        log.info("適用納税周期削除完了: shiteiNo={}, idx={}", shiteiNo, latest.getIdx());
    }
}
