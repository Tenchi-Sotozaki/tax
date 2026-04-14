package jp.lg.asp.accommodation.service.impl;

import jp.lg.asp.accommodation.dto.CollectorForm;
import jp.lg.asp.accommodation.dto.CollectorListItem;
import jp.lg.asp.accommodation.dto.CollectorSearchForm;
import jp.lg.asp.accommodation.dto.FacilityDto;
import jp.lg.asp.accommodation.dto.TaxManagerForm;
import jp.lg.asp.accommodation.service.CollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {

    // TODO: DB実装後は以下のRepositoryを注入して差し替える
    // private final SpecialCollectorRepository collectorRepository;
    // private final AccommodationFacilityRepository facilityRepository;

    private static final List<CollectorListItem> DUMMY_LIST = List.of(
        new CollectorListItem(1L, "T001001", "グランドホテル東京",   "グランドホテル東京本館", "hotel",    "ホテル",     "target"),
        new CollectorListItem(2L, "R002001", "温泉旅館やまと",       "やまと本館",             "ryokan",   "旅館",       "non-target"),
        new CollectorListItem(3L, "S003001", "シティイン新宿",       "シティイン新宿",         "simple",   "簡易宿所",   "target"),
        new CollectorListItem(4L, "M004001", "海辺の民宿しおかぜ",   "しおかぜ",               "minshuku", "民宿",       "target"),
        new CollectorListItem(5L, "P005001", "森のペンション風の丘", "風の丘",                 "pension",  "ペンション", "non-target")
    );

    @Override
    public List<CollectorListItem> search(CollectorSearchForm form) {
        // TODO: collectorRepository.findByCondition(form) に差し替え
        return DUMMY_LIST.stream()
                .filter(i -> !StringUtils.hasText(form.getRegistrationNo())
                        || i.getRegistrationNo().contains(form.getRegistrationNo()))
                .filter(i -> !StringUtils.hasText(form.getObligorName())
                        || i.getObligorName().contains(form.getObligorName()))
                .filter(i -> !StringUtils.hasText(form.getFacilityName())
                        || i.getFacilityName().contains(form.getFacilityName()))
                .filter(i -> !StringUtils.hasText(form.getBusinessType())
                        || i.getBusinessType().equals(form.getBusinessType()))
                .filter(i -> !StringUtils.hasText(form.getConsolidationTarget())
                        || i.getConsolidationTarget().equals(form.getConsolidationTarget()))
                .toList();
    }

    @Override
    public CollectorForm getCollectorById(Long id) {
        // TODO: collectorRepository.findById(id).map(this::toForm).orElseThrow() に差し替え
        CollectorForm form = new CollectorForm();
        form.setId(id);
        form.setObligorName("グランドホテル東京（ID:" + id + "）");
        form.setObligorAddress("東京都新宿区西新宿1-1-1");
        form.setObligorPhone("03-1234-5678");
        form.setFacilityAddress("東京都新宿区西新宿1-1-1");
        form.setFacilityNameKana("ぐらんどほてるとうきょう");
        form.setFacilityPhone("03-1234-5678");
        form.setRoomCount(100);
        form.setCapacity(200);
        form.setBusinessType("hotel");
        form.setLicenseNumber("LIC-" + id);
        form.setMailAddress("東京都新宿区西新宿1-1-1");
        form.setMailNameKana("たなか たろう");
        form.setMailPhone("03-1234-5678");
        return form;
    }

    @Override
    public String getObligorName(String obligorId) {
        // TODO: collectorRepository.findById(obligorId).getName() に差し替え
        return switch (obligorId != null ? obligorId : "") {
            case "1" -> "グランドホテル東京";
            case "2" -> "温泉旅館やまと";
            case "3" -> "シティイン新宿";
            default  -> "株式会社グランドホテル東京（ID:" + obligorId + "）";
        };
    }

    @Override
    public List<FacilityDto> getFacilities(String obligorId) {
        // TODO: facilityRepository.findByCollectorId(obligorId) に差し替え
        String name = getObligorName(obligorId);
        return List.of(
            new FacilityDto(obligorId + "-F1", name + " 本館",      "東京都新宿区西新宿1-1-1"),
            new FacilityDto(obligorId + "-F2", name + " 別館",      "東京都新宿区西新宿1-2-1"),
            new FacilityDto(obligorId + "-F3", name + " アネックス","東京都新宿区西新宿1-3-1"),
            new FacilityDto(obligorId + "-F4", name + " スイート",  "東京都新宿区西新宿1-4-1")
        );
    }

    @Override
    public TaxManagerForm buildTaxManagerForm(Long collectorId) {
        // TODO: DB実装後は既存の納税管理人データを取得してフォームに詰め替え
        CollectorForm collector = getCollectorById(collectorId);
        TaxManagerForm form = new TaxManagerForm();
        form.setCollectorId(collectorId);
        form.setObligorName(collector.getObligorName());
        form.setFacilityName("グランドホテル東京本館（ID:" + collectorId + "）");
        return form;
    }

    @Override
    public void register(CollectorForm form) {
        // TODO: collectorRepository.save(toEntity(form)) に差し替え
        log.info("特別徴収義務者登録: {}", form.getObligorName());
    }

    @Override
    public void update(Long id, CollectorForm form) {
        // TODO: collectorRepository.save(toEntity(form)) に差し替え
        log.info("特別徴収義務者更新: id={}, name={}", id, form.getObligorName());
    }

    @Override
    public void delete(Long id) {
        // TODO: collectorRepository.deleteById(id) に差し替え
        log.info("特別徴収義務者削除: id={}", id);
    }

    @Override
    public void saveTaxManager(Long collectorId, TaxManagerForm form) {
        // TODO: taxManagerRepository.save(toEntity(form)) に差し替え
        log.info("納税管理人登録: collectorId={}, manager={}", collectorId, form.getManagerName());
    }
}
