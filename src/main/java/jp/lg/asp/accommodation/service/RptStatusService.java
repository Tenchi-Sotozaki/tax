package jp.lg.asp.accommodation.service;

import java.util.List;

import jp.lg.asp.accommodation.dto.RptStatusListItem;
import jp.lg.asp.accommodation.dto.RptStatusSearchForm;
import jp.lg.asp.accommodation.entity.Reports;

public interface RptStatusService {
    List<Reports> findAllReports();
    List<RptStatusListItem> search(RptStatusSearchForm form);
}
