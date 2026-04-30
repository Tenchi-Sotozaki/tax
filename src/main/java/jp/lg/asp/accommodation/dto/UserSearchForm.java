package jp.lg.asp.accommodation.dto;

import lombok.Data;

@Data
public class UserSearchForm {
    private String id;
    private String name;
    private String nameKana;
    private String busho;
}
