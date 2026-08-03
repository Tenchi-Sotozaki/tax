package jp.lg.asp.accommodation.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * 宛名情報取込：CSVから読み取った登録値
 *
 * 解析フェーズから確定フェーズまでセッションで保持する。
 */
@Data
public class AtenaImportValueDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String atenaNo;
    private String kbn;
    private String name;
    private String nameKana;
    private String yubinNo;
    private String jusho;
    private String tel1;
    private String tel2;
    /** ハッシュ化済みの個人番号 */
    private String kojinNo;
    private String hojinNo;
}
