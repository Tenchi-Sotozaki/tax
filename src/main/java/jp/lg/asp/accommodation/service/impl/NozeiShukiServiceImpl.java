package jp.lg.asp.accommodation.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.NozeiShukiDto;
import jp.lg.asp.accommodation.repository.NozeiShukiRepository;
import jp.lg.asp.accommodation.service.NozeiShukiService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NozeiShukiServiceImpl implements NozeiShukiService {

    private final NozeiShukiRepository nozeiShukiRepository;

    @Value("${app.jichitai.code}")
    private String jichitaiCd;

    @Override
    @Transactional(readOnly = true)
    public List<NozeiShukiDto> findAll() {
        return nozeiShukiRepository.findActiveByJichitaiCd(jichitaiCd)
                .stream()
                .map(n -> new NozeiShukiDto(n.getSeq(), n.getShuki()))
                .toList();
    }
}
