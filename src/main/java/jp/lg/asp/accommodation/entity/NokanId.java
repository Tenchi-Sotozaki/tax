package jp.lg.asp.accommodation.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * 納税管理人ID
 */
@Data
public class NokanId implements Serializable {

    /** 自治体コード */
    private String jichitaiCd;

    /** 指定番号 */
    private String shiteiNo;
}