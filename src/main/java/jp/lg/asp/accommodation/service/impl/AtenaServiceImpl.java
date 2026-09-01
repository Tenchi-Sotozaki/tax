package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import jp.lg.asp.accommodation.dto.AtenaDaichoItem;
import jp.lg.asp.accommodation.dto.AtenaSearchForm;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.AtenaRepository;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.AtenaService;
import jp.lg.asp.accommodation.util.HashUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtenaServiceImpl implements AtenaService {
	
	private final JichitaiRepository jichitaiRepository;
	private final AtenaRepository atenaRepository;
	private final HashUtil hashUtil;
	
	@Override
	public List<AtenaDaichoItem> searchDaicho(String jichitaiCd, AtenaSearchForm searchForm, boolean searched) {
		if (!searched) {
            return List.of();
        }

        BigDecimal atenaStNo = jichitaiRepository.findById(jichitaiCd)
                .map(Jichitai::getAtenaStNo).orElse(null);

        return atenaRepository.search(
                jichitaiCd,
                toLikePattern(searchForm.getAtenaNo(), "exact"),
                toLikePattern(searchForm.getName(), searchForm.getNameMatchType()),
                toLikePattern(searchForm.getNameKana(), searchForm.getNameKanaMatchType()),
                toLikePattern(searchForm.getYubinNo(), "exact"),
                toLikePattern(searchForm.getJusho(), searchForm.getJushoMatchType()),
                toLikePattern(searchForm.getTel(), "exact"),
                toLikePattern(hashIfPresent(searchForm.getKojinNo()), "exact"),
                toLikePattern(searchForm.getHojinNo(), "exact")
        ).stream().map(a -> new AtenaDaichoItem(a, atenaStNo)).toList();
	}
	
	private String hashIfPresent(String s) {
        return (s == null || s.isBlank()) ? null : hashUtil.sha256(s);
    }

    private String toLikePattern(String value, String matchType) {
        if (value == null || value.isBlank())
            return "%";
        return switch (matchType) {
            case "prefix" -> value + "%";
            case "exact" -> value;
            default -> "%" + value + "%"; // partial
        };
    }
}
