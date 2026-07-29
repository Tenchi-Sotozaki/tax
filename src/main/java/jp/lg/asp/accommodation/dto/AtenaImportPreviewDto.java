package jp.lg.asp.accommodation.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 宛名情報取込：解析結果
 *
 * 「内容取込」押下時にCSVを解析した結果を保持し、
 * 差分確認モーダルの表示および確定処理に使用する。
 */
@Data
public class AtenaImportPreviewDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ファイル名 */
    private String fileName;

    /** 解析した全行 */
    private List<AtenaImportRowDto> rows = new ArrayList<>();

    /** 差分ありの件数（確認モーダルに表示する件数） */
    public int getSabunKensu() {
        return (int) rows.stream().filter(AtenaImportRowDto::isSabunAri).count();
    }

    /** 新規件数 */
    public int getShinkiKensu() {
        return (int) rows.stream().filter(AtenaImportRowDto::isShinki).count();
    }

    /** 差異なし件数（既存データと同一） */
    public int getSaiNashiKensu() {
        return (int) rows.stream().filter(r -> !r.isShinki() && !r.isSabunAri()).count();
    }

    /** 差分ありの行のみ抽出する */
    public List<AtenaImportRowDto> getSabunRows() {
        return rows.stream().filter(AtenaImportRowDto::isSabunAri).toList();
    }
}
