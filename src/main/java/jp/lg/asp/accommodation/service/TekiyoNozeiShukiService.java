package jp.lg.asp.accommodation.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TekiyoNozeiShukiService {

    private final TekiyoNozeiShukiRepository tekiyoNozeiShukiRepository;
    private final TokugimuRepository tokugimuRepository;
    private final NozeiShukiRepository nozeiShukiRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    private static final String FLG_ON = "1";
    private static final String FLG_OFF = "0";

    public List<NozeiShukiDto> getNozeiShukiOptions() {
        return nozeiShukiRepository.findActiveByJichitaiCd(jichitaiCd)
                .stream()
                .map(n -> new NozeiShukiDto(n.getSeq(), n.getShuki()))
                .toList();
    }

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
                .ifPresent(t -> {
                    form.setEdit(true);
                    form.setSeq(t.getSeq());
                    form.setTekiyoStYmd(t.getTekiyoStYmd());
                    form.setTekiyoEdYmd(t.getTekiyoEdYmd());
                });

        return form;
    }

    @Transactional
    public void save(String shiteiNo, TekiyoNozeiShukiForm form) {
        Integer maxRno = tekiyoNozeiShukiRepository.findMaxRnoByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        Integer newRno = maxRno + 1;

        if (maxRno > 0) {
            tekiyoNozeiShukiRepository.updateNewFlgToZero(jichitaiCd, shiteiNo);
        }

        Integer idxRno = tekiyoNozeiShukiRepository.findMaxIdxRnoByKey(jichitaiCd, shiteiNo, newRno) + 1;

        TekiyoNozeiShuki entity = new TekiyoNozeiShuki();
        entity.setJichitaiCd(jichitaiCd);
        entity.setShiteiNo(shiteiNo);
        entity.setRno(newRno);
        entity.setIdxRno(idxRno);
        entity.setSeq(form.getSeq());
        entity.setTekiyoStYmd(form.getTekiyoStYmd());
        entity.setTekiyoEdYmd(form.getTekiyoEdYmd());
        entity.setNewFlg(FLG_ON);
        entity.setDelFlg(FLG_OFF);

        tekiyoNozeiShukiRepository.save(entity);
        log.info("適用納税周期保存完了: shiteiNo={}, rno={}", shiteiNo, newRno);
    }
}
