package jp.lg.asp.accommodation.service;

import java.time.LocalDate;
import java.util.List;

import jp.lg.asp.accommodation.dto.KofukinFurikomiDto;

public interface KofukinFurikomiService {

    List<KofukinFurikomiDto> search(String jichitaiCd, LocalDate furikomiFrom, LocalDate furikomiTo,
            String taishoMonth, String shiteiNo, String name, String furikomiStatus);

    List<KofukinFurikomiDto> findByKeys(String jichitaiCd, List<KofukinFurikomiDto.Key> keys);

    KofukinFurikomiDto findById(String jichitaiCd, String shiteiNo, String taishoYm, Integer rno);

    void save(KofukinFurikomiDto dto);

    void delete(String jichitaiCd, String shiteiNo, String taishoYm, Integer rno);
}