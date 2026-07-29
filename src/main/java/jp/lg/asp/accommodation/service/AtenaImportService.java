package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.dto.AtenaImportPreviewDto;
import jp.lg.asp.accommodation.entity.AtenaRenkei;
import jp.lg.asp.accommodation.entity.AtenaRenkeiDef;

public interface AtenaImportService {

    /**
     * CSVを解析し、既存データとの差分を抽出する。この時点ではDBを更新しない。
     */
    AtenaImportPreviewDto analyze(MultipartFile file, String jichitaiCd);

    /**
     * 解析結果のうち、取込対象として選択された宛名のみを登録する。
     *
     * @param preview         解析結果
     * @param torikomuAtenaNo 差分ありのうち取込対象として選択された宛名番号
     */
    AtenaRenkei confirm(AtenaImportPreviewDto preview, Set<String> torikomuAtenaNo, String jichitaiCd, String userId);

    List<AtenaRenkei> findHistory(String jichitaiCd);

    /**
     * 取込結果の明細を取得する。
     */
    List<AtenaRenkeiDef> findDetail(String jichitaiCd, BigDecimal seq);
}
