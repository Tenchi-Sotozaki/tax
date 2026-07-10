package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class KanpuMenjoTsuchiDto {
    private String shiteiNo;
    private String cityName;
    private String jorei;
    private LocalDate hakkoYmd;
    private String tokuJusho;
    private String tokuName;
    private String shinsei_kbn;
    private String kettei_naiyou;
    private String shisetsuJusho;
    private String shisetsuName;
    private LocalDate juriYmd;
    private String shinseiYm;
    private String zeigaku;
    private String kanpuMenjoGaku;
    private String riyu;
    private String biko;
}