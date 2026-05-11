package jp.lg.asp.accommodation.service;

import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.dto.EltaxRenkeiKakuninDto;

public interface EltaxRenkeiKakuninService {

    /**
     * アップロードされたファイルを解析し、確認画面用DTOを生成する。
     * この時点ではDBへの登録は行わない。
     */
    EltaxRenkeiKakuninDto preview(MultipartFile file);

    /**
     * 確認済みファイルをDBに登録する（t_eltax_renkei への保存と関連テーブル更新）。
     */
    void commit(MultipartFile file);
}
