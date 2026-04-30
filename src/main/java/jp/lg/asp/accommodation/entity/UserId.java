package jp.lg.asp.accommodation.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class UserId implements Serializable {
	private String jichitaiCd;
	private String id;
}
