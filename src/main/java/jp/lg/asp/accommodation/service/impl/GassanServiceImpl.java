package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.GassanForm;
import jp.lg.asp.accommodation.dto.GassanForm.FacilityItem;
import jp.lg.asp.accommodation.dto.GassanForm.GassanListItem;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.Gassan;
import jp.lg.asp.accommodation.entity.GassanUchi;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.GassanRepository;
import jp.lg.asp.accommodation.repository.GassanUchiRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.GassanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GassanServiceImpl implements GassanService {

    private final GassanRepository gassanRepository;
    private final GassanUchiRepository gassanUchiRepository;
    private final AtenaRepository atenaRepository;
    private final TokugimuRepository tokugimuRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    @Override
    @Transactional(readOnly = true)
    public void reloadFacilityList(GassanForm form) {
        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, form.getAtenaNo());
        Set<String> checkedSet = form.getShiteiNoList() != null ? Set.copyOf(form.getShiteiNoList()) : Set.of();
        form.setFacilityList(tokugimuList.stream()
                .map(t -> new FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getKyokaName(), checkedSet.contains(t.getShiteiNo())))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GassanForm getByGassanShiteiNo(String gassanShiteiNo) {
        Gassan gassan = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("合算申告が見つかりません: " + gassanShiteiNo));

        List<String> checkedShiteiNos = gassanUchiRepository
                .findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo)
                .stream().map(GassanUchi::getShiteiNo).toList();

        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, gassan.getAtenaNo());
        Set<String> checkedSet = Set.copyOf(checkedShiteiNos);
        List<FacilityItem> facilityList = tokugimuList.stream()
                .map(t -> new FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getKyokaName(), checkedSet.contains(t.getShiteiNo())))
                .toList();

        Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, gassan.getAtenaNo()).orElse(null);

        GassanForm form = new GassanForm();
        form.setGassanShiteiNo(gassan.getGassanShiteiNo());
        form.setAtenaNo(gassan.getAtenaNo());
        form.setAtenaName(atena != null ? atena.getName() : "");
        form.setTekiyoStYmd(gassan.getTekiyoStYmd());
        form.setTekiyoEdYmd(gassan.getTekiyoEdYmd());
        form.setTorokuYmd(gassan.getTorokuYmd());
        form.setShinkokuYmd(gassan.getShinkokuYmd());
        form.setFacilityList(facilityList);
        form.setShiteiNoList(checkedShiteiNos);
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public GassanForm buildFormByShiteiNo(String shiteiNo) {
        Tokugimu tokugimu = tokugimuRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("施設が見つかりません: " + shiteiNo));

        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo());
        List<FacilityItem> facilityList = tokugimuList.stream()
                .map(t -> new FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getKyokaName(), false))
                .toList();

        Atena atena = atenaRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, tokugimu.getAtenaNo()).orElse(null);

        GassanForm form = new GassanForm();
        form.setFromShiteiNo(shiteiNo);
        form.setAtenaNo(tokugimu.getAtenaNo());
        form.setAtenaName(atena != null ? atena.getName() : tokugimu.getKyokaName());
        form.setTorokuYmd(LocalDate.now());
        form.setShinkokuYmd(LocalDate.now());
        form.setFacilityList(facilityList);
        return form;
    }

    @Override
    @Transactional
    public void register(GassanForm form) {
        String gassanShiteiNo = generateGassanShiteiNo();

        Gassan gassan = new Gassan();
        gassan.setJichitaiCd(jichitaiCd);
        gassan.setGassanShiteiNo(gassanShiteiNo);
        gassan.setRno(BigDecimal.ONE);
        gassan.setAtenaNo(form.getAtenaNo());
        gassan.setShiteiNo(form.getDaihyoShiteiNo());
        gassan.setTorokuYmd(form.getTorokuYmd());
        gassan.setShinkokuYmd(form.getShinkokuYmd());
        gassan.setTekiyoStYmd(form.getTekiyoStYmd());
        gassan.setTekiyoEdYmd(form.getTekiyoEdYmd());
        gassan.setNewFlg("1");
        gassan.setDelFlg("0");
        gassanRepository.save(gassan);

        saveGassanUchi(gassanShiteiNo, BigDecimal.ONE, form.getShiteiNoList());
        log.info("合算申告登録完了: gassanShiteiNo={}", gassanShiteiNo);
    }

    @Override
    @Transactional
    public void updateByGassanShiteiNo(String gassanShiteiNo, GassanForm form) {
        Gassan gassan = gassanRepository.findByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("合算申告が見つかりません: " + gassanShiteiNo));

        gassan.setTekiyoStYmd(form.getTekiyoStYmd());
        gassan.setTekiyoEdYmd(form.getTekiyoEdYmd());
        gassan.setShinkokuYmd(form.getShinkokuYmd());
        gassan.setShiteiNo(form.getDaihyoShiteiNo());
        gassanRepository.save(gassan);

        gassanUchiRepository.deleteByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        saveGassanUchi(gassanShiteiNo, gassan.getRno(), form.getShiteiNoList());
        log.info("合算申告更新完了: gassanShiteiNo={}", gassanShiteiNo);
    }

    @Override
    @Transactional
    public void deleteByGassanShiteiNo(String gassanShiteiNo) {
        gassanRepository.deleteLogicallyByJichitaiCdAndGassanShiteiNo(jichitaiCd, gassanShiteiNo);
        log.info("合算申告論理削除完了: gassanShiteiNo={}", gassanShiteiNo);
    }

    @Override
    @Transactional(readOnly = true)
    public GassanForm getLatestByShiteiNo(String shiteiNo) {
        List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        if (gassanList.isEmpty()) {
            throw new RuntimeException("合算申告が見つかりません: shiteiNo=" + shiteiNo);
        }
        return buildViewForm(shiteiNo, gassanList, gassanList.get(0).getGassanShiteiNo());
    }

    @Override
    @Transactional(readOnly = true)
    public GassanForm getViewFormByShiteiNo(String shiteiNo, String gassanShiteiNo) {
        List<Gassan> gassanList = gassanRepository.findByJichitaiCdAndShiteiNo(jichitaiCd, shiteiNo);
        if (gassanList.isEmpty()) {
            throw new RuntimeException("合算申告が見つかりません: shiteiNo=" + shiteiNo);
        }
        return buildViewForm(shiteiNo, gassanList, gassanShiteiNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GassanForm.FacilityItem> getFacilitiesByAtenaNo(BigDecimal atenaNo) {
        List<Tokugimu> tokugimuList = tokugimuRepository.findByJichitaiCdAndAtenaNo(jichitaiCd, atenaNo);
        return tokugimuList.stream()
                .map(t -> new GassanForm.FacilityItem(t.getShiteiNo(), t.getShisetsuName(), t.getKyokaName(), false))
                .toList();
    }

    private GassanForm buildViewForm(String shiteiNo, List<Gassan> gassanList, String selectedGassanShiteiNo) {
        GassanForm form = getByGassanShiteiNo(selectedGassanShiteiNo);
        form.setGassanList(gassanList.stream()
                .map(g -> new GassanListItem(g.getGassanShiteiNo(), g.getTekiyoStYmd()))
                .toList());
        form.setFromShiteiNo(shiteiNo);
        return form;
    }

    private String generateGassanShiteiNo() {
        int max = gassanRepository.findMaxGassanShiteiNoByJichitaiCd(jichitaiCd).orElse(0);
        return String.format("%08d", Math.max(max, 90000000) + 1);
    }

    private void saveGassanUchi(String gassanShiteiNo, BigDecimal rno, List<String> shiteiNoList) {
        if (shiteiNoList == null || shiteiNoList.isEmpty()) return;
        for (String shiteiNo : shiteiNoList) {
            GassanUchi uchi = new GassanUchi();
            uchi.setJichitaiCd(jichitaiCd);
            uchi.setGassanShiteiNo(gassanShiteiNo);
            uchi.setRno(rno);
            uchi.setShiteiNo(shiteiNo);
            gassanUchiRepository.save(uchi);
        }
    }
}
