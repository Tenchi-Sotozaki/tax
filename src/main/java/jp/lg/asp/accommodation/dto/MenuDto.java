package jp.lg.asp.accommodation.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuDto {
	private String menuId;
	private Integer level;
	private String pMenuId;
	private String name;
	private String screenId;
	private String iconLink;
	private String link;
	private List<MenuDto> children = new ArrayList<>();
}
