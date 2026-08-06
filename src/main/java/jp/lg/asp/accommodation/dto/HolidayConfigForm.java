package jp.lg.asp.accommodation.dto;

import java.util.List;

import lombok.Data;

@Data
public class HolidayConfigForm {

    private String nendo;
    private List<String> holidayDts; // yyyyMMdd形式
}
