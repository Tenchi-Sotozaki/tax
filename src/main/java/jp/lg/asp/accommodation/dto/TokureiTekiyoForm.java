package jp.lg.asp.accommodation.dto;

import java.util.List;

import lombok.Data;

@Data
public class TokureiTekiyoForm {

    private String shiteiNo;
    private String obligorName;
    private String facilityName;

    // 登録/編集対象レコードのrno（編集時にセット）
    private Integer rno;


    private String tekiyoStMonth;
    private String tekiyoEdMonth;

    private List<TokureiTekiyoHistoryDto> histories;
}
