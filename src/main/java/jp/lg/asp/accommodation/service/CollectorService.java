package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.CollectorForm;
import jp.lg.asp.accommodation.dto.CollectorListItem;
import jp.lg.asp.accommodation.dto.CollectorSearchForm;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CollectorService {

    // -------------------------------------------------------------------------
    // ダミーデータ（DB実装後は Repository に差し替える）
    // -------------------------------------------------------------------------
    private static final List<CollectorListItem> DUMMY_LIST = List.of(
        new CollectorListItem(1L, "T001001", "グランドホテル東京",   "グランドホテル東京本館", "hotel",   "ホテル",   "target"),
        new CollectorListItem(2L, "R002001", "温泉旅館やまと",       "やまと本館",             "ryokan",  "旅館",     "non-target"),
        new CollectorListItem(3L, "S003001", "シティイン新宿",       "シティイン新宿",         "simple",  "簡易宿所", "target"),
        new CollectorListItem(4L, "M004001", "海辺の民宿しおかぜ",   "しおかぜ",               "minshuku","民宿",     "target"),
        new CollectorListItem(5L, "P005001", "森のペンション風の丘", "風の丘",                 "pension", "ペンション","non-target")
    );

    // -------------------------------------------------------------------------
    // 検索
    // TODO: DB実装後は DUMMY_LIST を使った処理を削除し、
    //       collectorRepository.findByCondition(form) 等に差し替える。
    // -------------------------------------------------------------------------

    /**
     * 検索条件に合致する特別徴収義務者の一覧を返す。
     * 条件が空の場合は全件返す。
     */
    public List<CollectorListItem> search(CollectorSearchForm form) {
        return DUMMY_LIST.stream()
                .filter(item -> !StringUtils.hasText(form.getRegistrationNo())
                        || item.getRegistrationNo().contains(form.getRegistrationNo()))
                .filter(item -> !StringUtils.hasText(form.getObligorName())
                        || item.getObligorName().contains(form.getObligorName()))
                .filter(item -> !StringUtils.hasText(form.getFacilityName())
                        || item.getFacilityName().contains(form.getFacilityName()))
                .filter(item -> !StringUtils.hasText(form.getBusinessType())
                        || item.getBusinessType().equals(form.getBusinessType()))
                .filter(item -> !StringUtils.hasText(form.getConsolidationTarget())
                        || item.getConsolidationTarget().equals(form.getConsolidationTarget()))
                .toList();
    }

    // -------------------------------------------------------------------------
    // 1件取得
    // TODO: DB実装後は collectorRepository.findById(id) に差し替える。
    // -------------------------------------------------------------------------

    public CollectorForm getCollectorById(Long id) {
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
}
