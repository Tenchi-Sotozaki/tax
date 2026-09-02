package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.SofusakiCsvDto;
import jp.lg.asp.accommodation.entity.Atena;
import jp.lg.asp.accommodation.entity.ReportsLog;
import jp.lg.asp.accommodation.entity.Tokugimu;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.SofusakiCsvRepository;
import jp.lg.asp.accommodation.repository.TokugimuRepository;
import jp.lg.asp.accommodation.service.SofusakiCsvService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SofusakiCsvServiceImpl implements SofusakiCsvService {

    private final SofusakiCsvRepository sofusakiCsvRepository;
    private final TokugimuRepository tokugimuRepository;
    private final AtenaRepository atenaRepository;
    private final JichitaiContext jichitaiContext;

    @Override
    @Transactional(readOnly = true)
    public List<SofusakiCsvDto> findAll() {
        String jichitaiCd = jichitaiContext.getJichitaiCd();
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);

        List<ReportsLog> logs = sofusakiCsvRepository.findPrintedLogs(jichitaiCd, twoWeeksAgo);

        Map<String, String> rptNameMap = sofusakiCsvRepository
                .findPrintedLogsWithRptName(jichitaiCd, twoWeeksAgo)
                .stream()
                .filter(row -> row[0] != null)
                .collect(Collectors.toMap(
                        row -> ((String) row[0]).strip(),
                        row -> row[1] != null ? (String) row[1] : "",
                        (a, b) -> a));

        List<SofusakiCsvDto> results = new ArrayList<>();
        for (ReportsLog log : logs) {
            List<Tokugimu> tokugimuList = tokugimuRepository
                    .findByJichitaiCdAndShiteiNo(jichitaiCd, log.getShiteiNo());
            if (tokugimuList.isEmpty()) continue;

            Tokugimu t = tokugimuList.get(0);
            Atena atena = atenaRepository
                    .findByJichitaiCdAndAtenaNo(jichitaiCd, t.getAtenaNo())
                    .orElse(null);

            SofusakiCsvDto dto = new SofusakiCsvDto();
            dto.setAtenaNo(t.getAtenaNo());
            dto.setShiteiNo(t.getShiteiNo() != null ? t.getShiteiNo().strip() : null);
            dto.setSoufusakiName(fallback(t.getSoufusakiName(), atena != null ? atena.getName() : null));
            dto.setSoufusakiNameKana(fallback(t.getSoufusakiNameKana(), atena != null ? atena.getNameKana() : null));
            dto.setSoufusakiYubinNo(fallback(t.getSoufusakiYubinNo(), atena != null ? atena.getYubinNo() : null));
            dto.setSoufusakiJusho(fallback(t.getSoufusakiJusho(), atena != null ? atena.getJusho() : null));
            dto.setSoufusakiTel(fallback(t.getSoufusakiTel(), atena != null ? atena.getTel1() : null));
            dto.setRptName(rptNameMap.getOrDefault(log.getRptId().strip(), ""));
            dto.setOpeDt(log.getOpeDt());
            results.add(dto);
        }
        return results;
    }

    @Override
    public String toCsvString(List<SofusakiCsvDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("宛名番号,指定番号,氏名/名称,ふりがな,郵便番号,住所,電話番号\n");
        for (SofusakiCsvDto item : items) {
            sb.append(quoteForced(item.getAtenaNo() != null ? item.getAtenaNo().toPlainString() : "")).append(",");
            sb.append(quoteForced(item.getShiteiNo())).append(",");
            sb.append(escape(item.getSoufusakiName())).append(",");
            sb.append(escape(item.getSoufusakiNameKana())).append(",");
            sb.append(escape(item.getSoufusakiYubinNo())).append(",");
            sb.append(escape(item.getSoufusakiJusho())).append(",");
            sb.append(escape(item.getSoufusakiTel())).append("\n");
        }
        return sb.toString();
    }

    /** valueが空の場合はfallbackValueを返す */
    private String fallback(String value, String fallbackValue) {
        return (value == null || value.isBlank()) ? fallbackValue : value;
    }

    private String quoteForced(String value) {
        // nullと空文字はいずれも空文字とする（="" を出力しない）
        if (value == null || value.isEmpty()) return "";
        return "=\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
