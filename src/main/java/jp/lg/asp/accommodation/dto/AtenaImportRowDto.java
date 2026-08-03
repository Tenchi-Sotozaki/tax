package jp.lg.asp.accommodation.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 宛名情報取込：CSV1行分の解析結果
 */
@Data
public class AtenaImportRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 宛名番号 */
    private String atenaNo;

    /** 宛名名（CSVの氏名/名称） */
    private String name;

    /** 新規（既存データが存在しない） */
    private boolean shinki;

    /** 既存データとの差分あり */
    private boolean sabunAri;

    /** 項目単位の差分（既存データがある場合のみ設定） */
    private List<AtenaImportDiffDto> diffs = new ArrayList<>();

    /** CSVの値（確定処理で使用する） */
    private AtenaImportValueDto value;
}
