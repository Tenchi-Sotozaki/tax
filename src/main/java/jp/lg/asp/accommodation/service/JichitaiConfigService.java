package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.JichitaiConfigDto;
import jp.lg.asp.accommodation.entity.Jichitai;

public interface JichitaiConfigService {

    Jichitai findById(String jichitaiCd);

    void save(String currentJichitaiCd, JichitaiConfigDto configForm);
}
