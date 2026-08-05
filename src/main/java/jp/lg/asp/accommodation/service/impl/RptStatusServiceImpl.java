package jp.lg.asp.accommodation.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.RptStatusListItem;
import jp.lg.asp.accommodation.dto.RptStatusSearchForm;
import jp.lg.asp.accommodation.entity.Reports;
import jp.lg.asp.accommodation.entity.RptStatus;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.ReportsRepository;
import jp.lg.asp.accommodation.repository.RptStatusRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.RptStatusService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RptStatusServiceImpl implements RptStatusService {

    private final JichitaiContext jichitaiContext;
    private final ReportsRepository reportsRepository;
    private final RptStatusRepository rptStatusRepository;
    private final TokugimuRepository tokugimuRepository;

    @Override
    public List<Reports> findAllReports() {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        return reportsRepository.findAll().stream()
                .filter(r -> jichitaiCd.equals(r.getJichitaiCd()))
                .collect(Collectors.toList());
    }

    @Override
    public List<RptStatusListItem> search(RptStatusSearchForm form) {
        String jichitaiCd = jichitaiContext.getJichitaiCd();

        List<Tokugimu> tokugimuList = tokugimuRepository.findBySearchConditions(
                jichitaiCd,
                form.getShiteiNo(),
                form.getName(),
                isEmpty(form.getName()) ? null : "%" + form.getName() + "%",
                form.getShisetsuName(),
                isEmpty(form.getShisetsuName()) ? null : "%" + form.getShisetsuName() + "%",
                "999",
                form.getKojinNo(),
                form.getHojinNo());

        // 年度フィルタ
        List<RptStatus> allStatus = rptStatusRepository.findAll().stream()
                .filter(s -> jichitaiCd.equals(s.getJichitaiCd()))
                .filter(s -> isEmpty(form.getNendo()) || form.getNendo().equals(s.getNendo()))
                .collect(Collectors.toList());

        // shiteiNo -> (rptId -> createDt)
        Map<String, Map<String, java.time.LocalDateTime>> statusMap = new LinkedHashMap<>();
        for (RptStatus s : allStatus) {
            statusMap.computeIfAbsent(s.getShiteiNo(), k -> new LinkedHashMap<>())
                    .put(s.getRptId(), s.getCreateDt());
        }

        // 年度指定がある場合、該当 shiteiNo のみに絞る
        List<RptStatusListItem> result = new ArrayList<>();
        for (Tokugimu t : tokugimuList) {
            if (!isEmpty(form.getNendo()) && !statusMap.containsKey(t.getShiteiNo())) {
                continue;
            }
            RptStatusListItem item = new RptStatusListItem();
            item.setShiteiNo(t.getShiteiNo());
            item.setName(t.getAtena() != null ? t.getAtena().getName() : "");
            item.setShisetsuName(t.getShisetsuName());
            item.setRptStatusMap(statusMap.getOrDefault(t.getShiteiNo(), Map.of()));
            result.add(item);
        }
        return result;
    }

    private boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }
}
