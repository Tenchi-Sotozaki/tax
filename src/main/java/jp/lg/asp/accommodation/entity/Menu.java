package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_menu")
@Getter
@Setter
@IdClass(MenuId.class)
public class Menu extends BaseEntity {

	@Id
	@Column(name = "jichitai_cd")
	private String jichitaiCd;

	@Id
	@Column(name = "menu_id")
	private String menuId;

	@Column(name = "level", nullable = false)
	private Integer level;

	@Column(name = "p_menu_id")
	private String pMenuId;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "screen_id")
	private String screenId;

	@Column(name = "icon_link")
	private String iconLink;

	@Column(name = "link")
	private String link;

	@Column(name = "dsp_odr", nullable = false)
	private Integer dspOdr;
}
