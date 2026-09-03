package jp.lg.asp.accommodation.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.JichitaiListSearchForm;
import jp.lg.asp.accommodation.entity.Jichitai;
import jp.lg.asp.accommodation.repository.JichitaiRepository;
import jp.lg.asp.accommodation.service.JichitaiListService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JichitaiListServiceImpl implements JichitaiListService {

    private final JichitaiRepository jichitaiRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Jichitai> search(JichitaiListSearchForm form) {
        return jichitaiRepository.search(
                form.getJichitaiCd(),
                form.getName(),
                form.getNameMatchType(),
                form.getKbnName(),
                form.getKbnNameMatchType());
    }
}
