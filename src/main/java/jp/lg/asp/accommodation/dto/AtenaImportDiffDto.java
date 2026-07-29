package jp.lg.asp.accommodation.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 宛名情報取込：項目単位の差分
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AtenaImportDiffDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 項目名 */
    private String label;

    /** 現在の値（既存データ） */
    private String current;

    /** 取込後の値（CSVの値） */
    private String updated;

    /** 変更有無 */
    private boolean changed;
}
